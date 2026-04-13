package com.patch.notifier

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes

class PatchApp : Application() {
    companion object {
        const val CHANNEL_ID = "patch_reminders"
    }

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Patch Reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminders to replace estrogen patches"
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
