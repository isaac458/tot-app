package com.empire.myapplication.data.repository

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.empire.myapplication.data.remote.BotApiService
import com.empire.myapplication.data.remote.GenerateCodeRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class BotRepository @Inject constructor(
    private val botApiService: BotApiService,
    @ApplicationContext private val context: Context
) {
    suspend fun generateInstagramCode(): Result<String> {
        return try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                return Result.failure(Exception("يجب تسجيل الدخول أولاً"))
            }

            // جلب التوكن الخاص بفايربيس للتحقق من هوية المستخدم في البوت
            val idTokenResult = user.getIdToken(true).await()
            val idToken = idTokenResult.token
            if (idToken == null) {
                return Result.failure(Exception("فشل الحصول على التوثيق"))
            }

            // جلب بصمة الجهاز
            val deviceFingerprint = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

            // 1. جلب رابط السيرفر من فايربيس
            val configDoc = FirebaseFirestore.getInstance().collection("config").document("bot").get().await()
            val tunnelUrl = configDoc.getString("tunnelUrl") ?: return Result.failure(Exception("لم يتم العثور على رابط السيرفر. تأكد من تشغيل البوت."))
            val fullUrl = "$tunnelUrl/api/app/generate-code"

            // 2. الاتصال بالـ API
            val response = botApiService.generateCode(
                url = fullUrl,
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
}
