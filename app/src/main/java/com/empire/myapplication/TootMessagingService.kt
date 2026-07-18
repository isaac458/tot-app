package com.empire.myapplication

import android.app.NotificationManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TootMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("TootFCM", "New FCM token: $token")
        // حفظ التوكن في فايربيس إذا كان المستخدم مسجل دخول
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val type = message.data["type"] ?: ""
        val title = message.data["title"] ?: message.notification?.title ?: "توت"
        val body = message.data["body"] ?: message.notification?.body ?: ""

        when (type) {
            "link_success" -> {
                val igUsername = message.data["instagramUsername"] ?: ""
                showNotification(
                    channelId = TootApplication.CHANNEL_LINK,
                    title = "✅ تم ربط حسابك!",
                    body = "تم ربط حساب الانستغرام @$igUsername بنجاح. يمكنك الآن استخدام أوامر البوت.",
                    notificationId = 1001
                )
            }
            "update_available" -> {
                val version = message.data["version"] ?: ""
                showNotification(
                    channelId = TootApplication.CHANNEL_UPDATE,
                    title = "🔄 تحديث جديد متوفر!",
                    body = "النسخة $version متوفرة الآن. حدّث التطبيق للحصول على أحدث الميزات.",
                    notificationId = 1002
                )
            }
            else -> {
                if (title.isNotBlank() || body.isNotBlank()) {
                    showNotification(
                        channelId = TootApplication.CHANNEL_UPDATE,
                        title = title,
                        body = body,
                        notificationId = (System.currentTimeMillis() % 10000).toInt()
                    )
                }
            }
        }
    }

    private fun showNotification(channelId: String, title: String, body: String, notificationId: Int) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }

    private fun saveTokenToFirestore(token: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                    .await()
                Log.d("TootFCM", "FCM token saved to Firestore")
            } catch (e: Exception) {
                Log.e("TootFCM", "Failed to save FCM token", e)
            }
        }
    }
}
