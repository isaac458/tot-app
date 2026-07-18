package com.empire.myapplication.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.GET

interface BotApiService {
    @POST
    suspend fun generateCode(
        @retrofit2.http.Url url: String,
        @Header("Authorization") authHeader: String,
        @Body request: GenerateCodeRequest
    ): Response<GenerateCodeResponse>

    @GET
    suspend fun getUserStatus(
        @retrofit2.http.Url url: String,
        @Header("Authorization") authHeader: String
    ): Response<UserStatusResponse>

    @POST
    suspend fun unlinkAccount(
        @retrofit2.http.Url url: String,
        @Header("Authorization") authHeader: String
    ): Response<UnlinkResponse>
}

data class GenerateCodeRequest(
    @SerializedName("deviceFingerprint") val deviceFingerprint: String
)

data class GenerateCodeResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("code") val code: String?,
    @SerializedName("error") val error: String?
)

data class UserStatusResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("user") val user: UserData?,
    @SerializedName("error") val error: String?
)

data class UserData(
    @SerializedName("linked") val linked: Boolean = false,
    @SerializedName("instagramUsername") val instagramUsername: String? = null,
    @SerializedName("plan") val plan: String? = "free",
    @SerializedName("quota") val quota: QuotaData? = null
)

data class QuotaData(
    @SerializedName("daily") val daily: Int = 100,
    @SerializedName("used") val used: Int = 0
)

data class UnlinkResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?
)
