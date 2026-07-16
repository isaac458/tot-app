package com.empire.myapplication.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AiApiService {
    // Groq (متوافق مع OpenAI Chat Completions)
    @POST("openai/v1/chat/completions")
    suspend fun generateContent(
        @Header("Authorization") authHeader: String,
        @Body request: GroqRequest
    ): Response<GroqResponse>
}

// هيكلية الطلب (OpenAI-compatible)
data class GroqRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<GroqMessage>,
    @SerializedName("temperature") val temperature: Float = 0.7f,
    @SerializedName("max_tokens") val maxTokens: Int = 2048
)

// content قد يكون نص عادي (String) لأي رسالة تاريخية،
// أو قائمة أجزاء (List<GroqContentPart>) عندما تحتوي رسالة المستخدم على صورة (تنسيق Vision متوافق مع OpenAI/Groq).
// النوع Any هنا مقصود: Gson يسلسل كل قيمة حسب نوعها الفعلي وقت التشغيل.
data class GroqMessage(
    @SerializedName("role") val role: String, // "system" | "user" | "assistant"
    @SerializedName("content") val content: Any
)

data class GroqContentPart(
    @SerializedName("type") val type: String, // "text" | "image_url"
    @SerializedName("text") val text: String? = null,
    @SerializedName("image_url") val imageUrl: GroqImageUrl? = null
)

data class GroqImageUrl(
    @SerializedName("url") val url: String // data:image/jpeg;base64,...
)

// هيكلية الرد
data class GroqResponse(
    @SerializedName("choices") val choices: List<GroqChoice>?
)

data class GroqChoice(
    @SerializedName("message") val message: GroqResponseMessage?
)

data class GroqResponseMessage(
    @SerializedName("role") val role: String?,
    @SerializedName("content") val content: String?
)
