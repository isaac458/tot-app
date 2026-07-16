package com.empire.myapplication.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

interface AiApiService {
    @POST("v1/chat/completions")
    suspend fun generateContent(
        @Header("Authorization") authHeader: String,
        @Body request: GroqRequest
    ): Response<GroqResponse>
}

// هيكلية الطلب الموحدة (OpenAI/Groq style)
data class GroqRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<GroqMessage>,
    @SerializedName("temperature") val temperature: Float = 0.7f,
    @SerializedName("max_tokens") val maxTokens: Int = 2048
)

data class GroqMessage(
    @SerializedName("role") val role: String, // "system", "user", or "assistant"
    @SerializedName("content") val content: String
)

// هيكلية الرد
data class GroqResponse(
    @SerializedName("choices") val choices: List<GroqChoice>
)

data class GroqChoice(
    @SerializedName("message") val message: GroqMessage
)
