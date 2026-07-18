package com.empire.myapplication

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TootApplication : Application() {

    companion object {
        const val CHANNEL_LINK = "toot_link_channel"
        const val CHANNEL_UPDATE = "toot_update_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val linkChannel = NotificationChannel(
                CHANNEL_LINK,
                "ربط الحساب",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات ربط حساب انستغرام"
            }

            val updateChannel = NotificationChannel(
                CHANNEL_UPDATE,
                "التحديثات",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات توفر تحديثات جديدة"
            }

            manager.createNotificationChannel(linkChannel)
            manager.createNotificationChannel(updateChannel)
        }
    }
}
