package com.empire.myapplication.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface BotApiService {
    @POST
    suspend fun generateCode(
        @retrofit2.http.Url url: String,
        @Header("Authorization") authHeader: String,
        @Body request: GenerateCodeRequest
    ): Response<GenerateCodeResponse>
}

data class GenerateCodeRequest(
    @SerializedName("deviceFingerprint") val deviceFingerprint: String
)

data class GenerateCodeResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("code") val code: String?,
    @SerializedName("error") val error: String?
)
