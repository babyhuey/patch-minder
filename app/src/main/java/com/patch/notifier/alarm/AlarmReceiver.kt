package com.patch.notifier.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.patch.notifier.MainActivity
import com.patch.notifier.PatchApp

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_PATCH_DUE) return

        val patchId = intent.getIntExtra(AlarmScheduler.EXTRA_PATCH_ID, -1)
        val location = intent.getStringExtra(AlarmScheduler.EXTRA_PATCH_LOCATION) ?: return
        val isNag = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_NAG, false)
        val nagCount = intent.getIntExtra(AlarmScheduler.EXTRA_NAG_COUNT, 0)

        if (patchId == -1) return

        showNotification(context, patchId, location, isNag, nagCount)

        // Schedule next nag
        AlarmScheduler.scheduleNagAlarm(context, patchId, location, nagCount)
    }

    private fun showNotification(
        context: Context,
        patchId: Int,
        location: String,
        isNag: Boolean,
        nagCount: Int,
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPending = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = if (isNag) "Patch Still Needs Replacing!" else "Patch Replacement Due"
        val body = "Time to replace: $location"
        val priority = if (nagCount >= 2) {
            NotificationCompat.PRIORITY_MAX
        } else {
            NotificationCompat.PRIORITY_HIGH
        }

        val notification = NotificationCompat.Builder(context, PatchApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(tapPending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(patchId, notification)
        } catch (_: SecurityException) {
            // Notification permission not granted
        }
    }
}
