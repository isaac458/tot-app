package com.empire.myapplication.data.repository

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.empire.myapplication.data.remote.BotApiService
import com.empire.myapplication.data.remote.GenerateCodeRequest
import com.empire.myapplication.data.remote.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class BotRepository @Inject constructor(
    private val botApiService: BotApiService,
    @ApplicationContext private val context: Context
) {
    private suspend fun getAuthToken(): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return user.getIdToken(true).await().token
    }

    private suspend fun getTunnelUrl(): String? {
        val configDoc = FirebaseFirestore.getInstance().collection("config").document("bot").get().await()
        return configDoc.getString("tunnelUrl")
    }

    suspend fun getUserStatus(): Result<UserData> {
        return try {
            val idToken = getAuthToken() ?: return Result.failure(Exception("يجب تسجيل الدخول أولاً"))
            val tunnelUrl = getTunnelUrl() ?: return Result.failure(Exception("لم يتم العثور على رابط السيرفر. تأكد من تشغيل البوت."))

            val uid = FirebaseAuth.getInstance().currentUser!!.uid
            val response = botApiService.getUserStatus(
                url = "$tunnelUrl/api/app/user/$uid",
                authHeader = "Bearer $idToken"
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.user != null) {
                    Result.success(body.user)
                } else {
                    // المستخدم غير موجود = غير مربوط
                    Result.success(UserData(linked = false))
                }
            } else {
                // 404 أو غيره = غير مربوط
                Result.success(UserData(linked = false))
            }
        } catch (e: Exception) {
            Log.e("BotRepository", "Error getting user status", e)
            Result.success(UserData(linked = false))
        }
    }

    suspend fun generateInstagramCode(): Result<String> {
        return try {
            val idToken = getAuthToken() ?: return Result.failure(Exception("يجب تسجيل الدخول أولاً"))
            val tunnelUrl = getTunnelUrl() ?: return Result.failure(Exception("لم يتم العثور على رابط السيرفر. تأكد من تشغيل البوت."))
            val deviceFingerprint = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

            val response = botApiService.generateCode(
                url = "$tunnelUrl/api/app/generate-code",
                authHeader = "Bearer $idToken",
                request = GenerateCodeRequest(deviceFingerprint)
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.code != null) {
                    Result.success(body.code)
                } else {
                    Result.failure(Exception(body?.error ?: "خطأ غير معروف من الخادم"))
                }
            } else {
                Result.failure(Exception("فشل الاتصال: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("BotRepository", "Error generating code", e)
            Result.failure(e)
        }
    }

    suspend fun unlinkAccount(): Result<String> {
        return try {
            val idToken = getAuthToken() ?: return Result.failure(Exception("يجب تسجيل الدخول أولاً"))
            val tunnelUrl = getTunnelUrl() ?: return Result.failure(Exception("لم يتم العثور على رابط السيرفر. تأكد من تشغيل البوت."))

            val response = botApiService.unlinkAccount(
                url = "$tunnelUrl/api/app/unlink",
                authHeader = "Bearer $idToken"
            )

            if (response.isSuccessful) {
                val body = response.body()
                Result.success(body?.message ?: "تم فك الربط بنجاح")
            } else {
                Result.failure(Exception("فشل فك الربط: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("BotRepository", "Error unlinking account", e)
            Result.failure(e)
        }
    }
}
