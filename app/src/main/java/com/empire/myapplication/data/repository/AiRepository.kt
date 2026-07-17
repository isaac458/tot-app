package com.empire.myapplication.data.repository

import com.empire.myapplication.BuildConfig
import com.empire.myapplication.data.local.Message
import com.empire.myapplication.data.local.SourceRef
import com.empire.myapplication.data.local.TootDao
import com.empire.myapplication.data.local.UserMemory
import com.empire.myapplication.data.remote.*
import org.json.JSONObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor(
    private val apiService: AiApiService,
    private val tootDao: TootDao
) {
    fun getMessages(chatId: Long): Flow<List<Message>> = tootDao.getMessagesForSession(chatId)

    fun getSessionsForOwner(ownerId: String): Flow<List<com.empire.myapplication.data.local.ChatSession>> = tootDao.getSessionsForOwner(ownerId)

    suspend fun createNewSession(title: String, ownerId: String = "guest"): Long {
        return tootDao.insertSession(com.empire.myapplication.data.local.ChatSession(title = title, ownerId = ownerId))
    }

    suspend fun updateSessionTitle(sessionId: Long, title: String) {
        tootDao.updateSessionTitle(sessionId, title)
    }

    suspend fun sendMessage(
        chatId: Long,
        content: String,
        imageBase64: String? = null,
        onRetry: ((attempt: Int) -> Unit)? = null
    ): String {
        // 1. حفظ رسالة المستخدم
        val userMsg = Message(sessionId = chatId, role = "user", content = content, imageUri = imageBase64)
        tootDao.insertMessage(userMsg)

        // 2. محاولة استخراج معلومات للذاكرة
        extractAndSaveMemory(content)

        // 3. تحضير السياق (System Instruction + Memory)
        val memories = tootDao.getUserMemory().first()
        val memoryContext = if (memories.isNotEmpty()) {
            "\nمعلومات سابقة عن المستخدم: " + memories.joinToString(". ") { "${it.key}: ${it.value}" }
        } else ""

        val systemMessage = GroqMessage(
            role = "system",
            content = com.empire.myapplication.core.AiConstants.SYSTEM_PROMPT + memoryContext
        )

        // 4. تحويل التاريخ إلى تنسيق Groq/OpenAI (role: system/user/assistant)
        // كل رسالة تحتوي صورة تُبنى كمصفوفة أجزاء (نص + image_url) حتى يقدر الموديل يحللها فعلياً،
        // وباقي الرسائل نصية عادية. وجود أي صورة في المحادثة يفعّل استخدام موديل الرؤية (Vision).
        val chatHistory = mutableListOf<GroqMessage>()
        var hadImage = false
        val messagesList = tootDao.getMessagesForSessionOnce(chatId)
        messagesList.forEach { msg ->
            val role = if (msg.role == "user") "user" else "assistant"
            if (msg.imageUri != null) {
                hadImage = true
                val parts = mutableListOf<GroqContentPart>()
                if (msg.content.isNotBlank()) {
                    parts.add(GroqContentPart(type = "text", text = msg.content))
                }
                parts.add(
                    GroqContentPart(
                        type = "image_url",
                        imageUrl = GroqImageUrl(url = "data:image/jpeg;base64,${msg.imageUri}")
                    )
                )
                chatHistory.add(GroqMessage(role = role, content = parts))
            } else if (msg.content.isNotBlank()) {
                chatHistory.add(GroqMessage(role = role, content = msg.content))
            }
        }

        // 5. استدعاء API مع Retry (نختار موديل الرؤية تلقائياً إذا كانت هناك صورة)
        val finalContent = callApiWithRetry(systemMessage, chatHistory, useVisionModel = hadImage, onRetry = onRetry)

        // 6. استخراج المصادر وحفظ الرد
        val extractedSources = extractSources(finalContent)
        val modelMsgId = tootDao.insertMessage(
            Message(sessionId = chatId, role = "model", content = finalContent, hasSources = extractedSources.isNotEmpty())
        )
        extractedSources.forEach { (title, url) ->
            tootDao.insertSource(SourceRef(messageId = modelMsgId, title = title, url = url, ownerId = "shared"))
        }

        return finalContent
    }

    suspend fun regenerateMessage(
        chatId: Long,
        onRetry: ((attempt: Int) -> Unit)? = null
    ): String {
        // 1. حذف آخر رسالة موديل إن وجدت
        val currentMsgs = tootDao.getMessagesForSessionOnce(chatId)
        val lastModelMsg = currentMsgs.lastOrNull { it.role == "model" }
        if (lastModelMsg != null) {
            tootDao.deleteMessageById(lastModelMsg.id)
            tootDao.deleteSourcesForMessage(lastModelMsg.id)
        }

        // 2. تحضير السياق (دون إضافة أي رسالة مستخدم جديدة)
        val memories = tootDao.getUserMemory().first()
        val memoryContext = if (memories.isNotEmpty()) {
            "\nمعلومات سابقة عن المستخدم: " + memories.joinToString(". ") { "${it.key}: ${it.value}" }
        } else ""

        val systemMessage = GroqMessage(
            role = "system",
            content = com.empire.myapplication.core.AiConstants.SYSTEM_PROMPT + memoryContext
        )

        val chatHistory = mutableListOf<GroqMessage>()
        var hadImage = false
        val messagesList = tootDao.getMessagesForSessionOnce(chatId)
        messagesList.forEach { msg ->
            val role = if (msg.role == "user") "user" else "assistant"
            if (msg.imageUri != null) {
                hadImage = true
                val parts = mutableListOf<GroqContentPart>()
                if (msg.content.isNotBlank()) {
                    parts.add(GroqContentPart(type = "text", text = msg.content))
                }
                parts.add(
                    GroqContentPart(
                        type = "image_url",
                        imageUrl = GroqImageUrl(url = "data:image/jpeg;base64,${msg.imageUri}")
                    )
                )
                chatHistory.add(GroqMessage(role = role, content = parts))
            } else if (msg.content.isNotBlank()) {
                chatHistory.add(GroqMessage(role = role, content = msg.content))
            }
        }

        val finalContent = callApiWithRetry(systemMessage, chatHistory, useVisionModel = hadImage, onRetry = onRetry)

        val extractedSources = extractSources(finalContent)
        val modelMsgId = tootDao.insertMessage(
            Message(sessionId = chatId, role = "model", content = finalContent, hasSources = extractedSources.isNotEmpty())
        )
        extractedSources.forEach { (title, url) ->
            tootDao.insertSource(SourceRef(messageId = modelMsgId, title = title, url = url, ownerId = "shared"))
        }

        return finalContent
    }

    fun getSourcesForMessage(messageId: Long) = tootDao.getSourcesForMessage(messageId)

    private fun extractSources(content: String): List<Pair<String, String>> {
        val linkRegex = Regex("\\[([^\\]]+)]\\((https?://[^\\s)]+)\\)")
        val markdownLinks = linkRegex.findAll(content)
            .map { it.groupValues[1] to it.groupValues[2] }
            .toList()

        // نزيل روابط الماركداون من النص أولاً حتى لا تُكتشف مرة ثانية كرابط مجرّد (bare URL)
        val withoutMarkdownLinks = linkRegex.replace(content, "")
        val bareUrlRegex = Regex("https?://[^\\s)\\]]+")
        val bareLinks = bareUrlRegex.findAll(withoutMarkdownLinks)
            .map { match ->
                val url = match.value.trimEnd('.', ',', '،')
                val host = try { java.net.URI(url).host ?: url } catch (e: Exception) { url }
                host to url
            }
            .toList()

        return (markdownLinks + bareLinks)
            .distinctBy { it.second }
            .take(6)
            .toList()
    }

    suspend fun generateChatTitle(userMessage: String, assistantResponse: String): String? {
        val prompt = """
            أنت مولد عناوين للمحادثات. أنشئ عنواناً من 2 إلى 5 كلمات فقط يلخص الموضوع الرئيسي.
            
            القواعد الصارمة:
            1. استخدم نفس لغة المستخدم (العربية أو الإنجليزية مثلاً).
            2. لا تستخدم علامات اقتباس أو رموز أو نقطة في النهاية.
            3. لا تبدأ بـ "كيفية"، "سؤال عن"، "طريقة"، "شرح".
            4. لا تكتب مقدمات، أعد العنوان فقط.
            5. ركز على أهم الكلمات الجوهرية.
            6. إذا كان المحتوى غير كافٍ لتوليد عنوان مفيد (مثل مجرد سلام)، أرجع العنوان "محادثة" أو اترك العنوان كما هو.
            
            المحادثة:
            المستخدم: $userMessage
            المساعد: $assistantResponse
            
            العنوان المطلوب:
        """.trimIndent()

        return try {
            val request = GroqRequest(
                model = MODEL_NAME,
                messages = listOf(GroqMessage(role = "user", content = prompt))
            )
            val response = apiService.generateContent(
                authHeader = "Bearer ${BuildConfig.AI_API_KEY.trim()}",
                request = request
            )
            if (response.isSuccessful) {
                response.body()?.choices?.firstOrNull()?.message?.content?.trim()?.removeSurrounding("\"")
            } else null
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        // للنصوص
        private const val MODEL_NAME = "deepseek-r1-distill-llama-70b"

        // للصور
        private const val VISION_MODEL_NAME = "llama-3.2-11b-vision-preview"
    }

    /**
     * يفكّك جسم خطأ Groq/OpenAI (JSON) ويستخرج منه type / code / message الحقيقيين
     * بدل ما يتقص أو يتخفى.
     */
    private fun parseGroqError(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "(لا يوجد جسم خطأ راجع من الخادم)"
        return try {
            val root = JSONObject(errorBody).optJSONObject("error") ?: return errorBody
            val type = root.optString("type", "?")
            val code = root.optString("code", "?")
            val message = root.optString("message", "?")
            "type=$type | code=$code\nرسالة Groq: $message"
        } catch (e: Exception) {
            "تعذّر تحليل جسم الخطأ كـ JSON (${e.javaClass.simpleName}). الجسم الخام:\n$errorBody"
        }
    }

    private suspend fun callApiWithRetry(
        systemMessage: GroqMessage,
        history: List<GroqMessage>,
        useVisionModel: Boolean = false,
        maxRetries: Int = 3,
        onRetry: ((attempt: Int) -> Unit)? = null
    ): String {
        val delayMillis = listOf(5_000L, 15_000L, 30_000L)

        if (BuildConfig.AI_API_KEY.isBlank()) {
            return "❌ لا يوجد مفتاح API مُعرَّف في المشروع (AI_API_KEY فارغ في local.properties)."
        }

        val key = BuildConfig.AI_API_KEY.trim().replace("\\s".toRegex(), "")
        val modelToUse = if (useVisionModel) VISION_MODEL_NAME else MODEL_NAME

        repeat(maxRetries) { attempt ->
            try {
                val request = GroqRequest(
                    model = modelToUse,
                    messages = listOf(systemMessage) + history
                )

                val response = apiService.generateContent(
                    authHeader = "Bearer $key",
                    request = request
                )

                if (response.isSuccessful) {
                    val aiText = response.body()?.choices?.firstOrNull()?.message?.content
                    return aiText ?: "⚠️ الخادم رجّع 200 لكن بدون أي نص رد. الاستجابة الكاملة: ${response.body()}"
                }

                val errorBody = try { response.errorBody()?.string() } catch (e: Exception) { "(فشل قراءة جسم الخطأ: ${e.javaClass.simpleName})" }
                val diagnosis = parseGroqError(errorBody)

                when (response.code()) {
                    400 -> return "❌ خطأ 400 (طلب غير صالح).\n$diagnosis"
                    429 -> {
                        if (attempt < maxRetries - 1) {
                            onRetry?.invoke(attempt + 1)
                            delay(delayMillis[attempt])
                        } else {
                            return "⚠️ 429 - تجاوزت الحصة/معدل الطلبات بعد $maxRetries محاولات.\n$diagnosis"
                        }
                    }
                    401, 403 -> return "❌ رفض الخادم المفتاح (كود ${response.code()}). تأكد إن مفتاح Groq صحيح ومنسوخ كامل.\n$diagnosis"
                    404 -> return "❌ خطأ 404 (غير موجود).\nالموديل المُستخدَم فعلياً: $modelToUse\n$diagnosis"
                    else -> return "❌ خطأ غير متوقع (كود ${response.code()}).\n$diagnosis"
                }
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) {
                    return "❌ استثناء أثناء الاتصال: ${e.javaClass.simpleName}\nالرسالة: ${e.localizedMessage ?: "(بدون رسالة)"}"
                }
                onRetry?.invoke(attempt + 1)
                delay(delayMillis[attempt])
            }
        }
        return "⚠️ فشلت كل المحاولات ($maxRetries) بدون استجابة ناجحة."
    }

    suspend fun deleteMessages(chatId: Long) {
        tootDao.deleteSessionById(chatId)
    }

    suspend fun clearSessionsForOwner(ownerId: String) {
        tootDao.clearSessionsForOwner(ownerId)
    }

    private suspend fun extractAndSaveMemory(content: String) {
        if (content.contains("اسمي هو", ignoreCase = true) || content.contains("اسمي", ignoreCase = true)) {
            val name = content.replace("اسمي هو", "").replace("اسمي", "").trim()
            if (name.length < 20) tootDao.insertMemory(UserMemory(key = "اسم المستخدم", value = name))
        }

        if (content.contains("أحب", ignoreCase = true)) {
            val hobby = content.substringAfter("أحب").trim()
            if (hobby.length < 50) tootDao.insertMemory(UserMemory(key = "هواية/اهتمام", value = hobby))
        }
    }
}
