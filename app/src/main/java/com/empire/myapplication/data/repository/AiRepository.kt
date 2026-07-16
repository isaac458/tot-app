package com.empire.myapplication.data.repository

import com.empire.myapplication.BuildConfig
import com.empire.myapplication.data.local.Message
import com.empire.myapplication.data.local.SourceRef
import com.empire.myapplication.data.local.TootDao
import com.empire.myapplication.data.local.UserMemory
import com.empire.myapplication.data.remote.*
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

        val fullSystemPrompt = com.empire.myapplication.core.AiConstants.SYSTEM_PROMPT + memoryContext

        // 4. تحويل التاريخ إلى تنسيق Groq/OpenAI
        val groqMessages = mutableListOf<GroqMessage>()
        
        // إضافة تعليمات النظام أولاً
        groqMessages.add(GroqMessage(role = "system", content = fullSystemPrompt))

        // إضافة تاريخ المحادثة
        tootDao.getMessagesForSessionOnce(chatId).forEach { msg ->
            val role = if (msg.role == "user") "user" else "assistant"
            // Groq حالياً يدعم النصوص بشكل أفضل في الشات العادي
            groqMessages.add(GroqMessage(role = role, content = msg.content))
        }

        // 5. استدعاء API
        val aiContent = callApiWithRetry(groqMessages, onRetry = onRetry)

        // 6. استخراج المصادر وحفظ الرد
        val extractedSources = extractSources(aiContent)
        val modelMsgId = tootDao.insertMessage(
            Message(sessionId = chatId, role = "model", content = aiContent, hasSources = extractedSources.isNotEmpty())
        )
        extractedSources.forEach { (title, url) ->
            tootDao.insertSource(SourceRef(messageId = modelMsgId, title = title, url = url, ownerId = "shared"))
        }

        return aiContent
    }

    fun getSourcesForMessage(messageId: Long) = tootDao.getSourcesForMessage(messageId)

    private fun extractSources(content: String): List<Pair<String, String>> {
        val linkRegex = Regex("\\[([^\\]]+)]\\((https?://[^\\s)]+)\\)")
        return linkRegex.findAll(content)
            .map { it.groupValues[1] to it.groupValues[2] }
            .distinctBy { it.second }
            .take(6)
            .toList()
    }

    private suspend fun callApiWithRetry(
        messages: List<GroqMessage>,
        maxRetries: Int = 3,
        onRetry: ((attempt: Int) -> Unit)? = null
    ): String {
        val delayMillis = listOf(3_000L, 10_000L, 20_000L)

        // تنظيف المفتاح (سواء كان Groq أو OpenAI)
        val key = BuildConfig.AI_API_KEY.replace("\\s".toRegex(), "").trim()
        
        if (key.isBlank()) {
            return "❌ لا يوجد مفتاح API (AI_API_KEY فارغ في local.properties)."
        }

        repeat(maxRetries) { attempt ->
            try {
                // نستخدم موديل Llama 3.1 70B القوي جداً
                val request = GroqRequest(
                    model = "llama-3.3-70b-versatile", // أحدث موديل متاح في Groq حالياً
                    messages = messages
                )
                
                val response = apiService.generateContent(
                    authHeader = "Bearer $key",
                    request = request
                )
                
                if (response.isSuccessful) {
                    return response.body()?.choices?.firstOrNull()?.message?.content 
                        ?: "عذراً، لم أستطع الرد. 🤔"
                }

                val errorBody = response.errorBody()?.string() ?: ""
                
                when (response.code()) {
                    401 -> return "❌ مفتاح API غير صحيح. تأكد من استخراج مفتاح يبدأ بـ gsk_ من Groq Console."
                    429 -> {
                        if (attempt < maxRetries - 1) {
                            onRetry?.invoke(attempt + 1)
                            delay(delayMillis[attempt])
                        } else return "⚠️ تجاوزت حد الاستخدام المجاني، جرب بعد دقيقة."
                    }
                    else -> return "خطأ من السيرفر (${response.code()}): $errorBody"
                }
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) {
                    return "حدث خطأ في الاتصال: ${e.localizedMessage}"
                }
                onRetry?.invoke(attempt + 1)
                delay(delayMillis[attempt])
            }
        }
        return "⚠️ فشلت المحاولات."
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
    }
}
