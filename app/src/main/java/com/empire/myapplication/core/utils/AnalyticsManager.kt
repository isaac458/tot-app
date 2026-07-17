package com.empire.myapplication.core.utils

import android.content.Context
import android.os.Build
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    private val themeManager: ThemeManager
) {
    private val db = FirebaseFirestore.getInstance()

    fun logUserPresence(context: Context) {
        val userId = themeManager.getUserId()
        if (userId == "guest" || userId.isBlank()) return

        val analytics = FirebaseAnalytics.getInstance(context)

        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            
            val userData = hashMapOf(
                "uid" to userId,
                "email" to themeManager.getUserEmail(),
                "name" to themeManager.getUserName(),
                "device_model" to Build.MODEL,
                "manufacturer" to Build.MANUFACTURER,
                "os_version" to Build.VERSION.RELEASE,
                "sdk_int" to Build.VERSION.SDK_INT,
                "app_version" to packageInfo.versionName,
                "app_version_code" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode.toLong(),
                "last_seen" to Date(),
                "platform" to "Android"
            )

            // 1. حفظ في قاعدة البيانات للوحة التحكم
            db.collection("users").document(userId)
                .set(userData, SetOptions.merge())

            // 2. إرسال حدث لجوجل (Analytics) لكي يصلك إشعار
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.METHOD, "google")
                putString("device", Build.MODEL)
                putString("user_email", themeManager.getUserEmail())
            }
            analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
