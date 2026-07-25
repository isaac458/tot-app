package com.empire.myapplication.core.utils

import android.content.Context
import android.os.Build
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

@Singleton
class AnalyticsManager @Inject constructor(
    private val themeManager: ThemeManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    private val db = FirebaseFirestore.getInstance()
    private val analytics = FirebaseAnalytics.getInstance(context)
    
    // Throttling: Only sync device profile once a day (24h)
    private val prefs = context.getSharedPreferences("analytics_prefs", Context.MODE_PRIVATE)

    fun logDailyDeviceProfile() {
        val userId = themeManager.getUserId()
        if (userId == "guest" || userId.isBlank()) return

        val lastSync = prefs.getLong("last_profile_sync", 0)
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L

        if (now - lastSync < oneDayMillis) return

        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            
            val userData = hashMapOf(
                "uid" to userId,
                "email" to themeManager.getUserEmail(),
                "name" to themeManager.getUserName(),
                "device_model" to Build.MODEL,
                "manufacturer" to Build.MANUFACTURER,
                "os_version" to Build.VERSION.RELEASE,
                "app_version" to packageInfo.versionName,
                "locale" to Locale.getDefault().toString(),
                "timezone" to TimeZone.getDefault().id,
                "network_type" to getNetworkType(),
                "battery_level" to getBatteryLevel(),
                "platform" to "Android",
                "last_sync" to Date()
            )

            db.collection("users").document(userId)
                .set(userData, SetOptions.merge())

            prefs.edit().putLong("last_profile_sync", now).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getNetworkType(): String {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return "none"
            val capabilities = cm.getNetworkCapabilities(network) ?: return "none"
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
                else -> "other"
            }
        } catch (e: Exception) {
            return "unknown"
        }
    }

    private fun getBatteryLevel(): Int {
        try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level == -1 || scale == -1) return -1
            return (level * 100 / scale.toFloat()).toInt()
        } catch (e: Exception) {
            return -1
        }
    }

    // ==========================================
    // EVENTS TRACKING
    // ==========================================

    private fun logEvent(eventName: String, params: Bundle = Bundle()) {
        val userId = themeManager.getUserId()
        if (userId.isNotBlank() && userId != "guest") {
            params.putString("uid", userId)
            
            // Log to Firebase Analytics
            analytics.logEvent(eventName, params)

            // Optional: Increment counters in Firestore for certain major events
            when (eventName) {
                "message_sent" -> incrementCounter("total_messages_sent")
                "message_received" -> incrementCounter("total_messages_received")
                "app_open" -> {
                    db.collection("users").document(userId)
                        .set(mapOf("last_seen" to Date(), "session_count" to FieldValue.increment(1)), SetOptions.merge())
                }
            }
        }
    }

    private fun incrementCounter(field: String) {
        val userId = themeManager.getUserId()
        if (userId == "guest" || userId.isBlank()) return
        db.collection("users").document(userId)
            .set(mapOf(field to FieldValue.increment(1)), SetOptions.merge())
    }

    fun logAppOpen() = logEvent("app_open")
    fun logChatStarted() = logEvent("chat_started")
    fun logMessageSent(hasImage: Boolean = false) {
        val bundle = Bundle().apply { putBoolean("has_image", hasImage) }
        logEvent("message_sent", bundle)
    }
    fun logMessageReceived() = logEvent("message_received")
    fun logImageUploaded() = logEvent("image_uploaded")
    
    fun logMemoryEnabled() {
        logEvent("memory_enabled")
        val userId = themeManager.getUserId()
        if (userId != "guest" && userId.isNotBlank()) {
            db.collection("users").document(userId).set(mapOf("is_memory_enabled" to true), SetOptions.merge())
        }
    }
    
    fun logMemoryDisabled() {
        logEvent("memory_disabled")
        val userId = themeManager.getUserId()
        if (userId != "guest" && userId.isNotBlank()) {
            db.collection("users").document(userId).set(mapOf("is_memory_enabled" to false), SetOptions.merge())
        }
    }
    
    fun logConversationDeleted() = logEvent("conversation_deleted")
    fun logLoginGoogle() = logEvent("login_google")
    fun logLoginGuest() = logEvent("login_guest")
    fun logLogout() = logEvent("logout")
    fun logThemeChanged(themeName: String) {
        val bundle = Bundle().apply { putString("theme_name", themeName) }
        logEvent("theme_changed", bundle)
    }
    fun logSettingsOpened() = logEvent("settings_opened")
    fun logErrorApi(errorMessage: String) {
        val bundle = Bundle().apply { putString("error_message", errorMessage.take(100)) }
        logEvent("error_api", bundle)
    }
    fun logApiTimeout() = logEvent("api_timeout")
    fun logShareChat() = logEvent("share_chat")
    fun logCopyMessage() = logEvent("copy_message")
}
