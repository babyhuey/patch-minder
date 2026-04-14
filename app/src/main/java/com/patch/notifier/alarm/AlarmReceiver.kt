package com.patch.notifier.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.patch.notifier.MainActivity
import com.patch.notifier.PatchApp
import com.patch.notifier.data.PatchDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val NAG_LIMIT = 48 // stop after 4 days of nagging (48 * 2h)
        private const val NAG_SUPPRESSION_TOLERANCE_MS = 60_000L
        private const val GROUP_KEY = "com.patch.notifier.PATCH_DUE_GROUP"

        /** Returns true if the patch was recently replaced and nag should be suppressed. */
        fun shouldSuppressNag(patchDueAt: Long?, nowMs: Long): Boolean {
            if (patchDueAt == null) return false
            return patchDueAt > nowMs + NAG_SUPPRESSION_TOLERANCE_MS
        }

        /** Returns true if another nag should be scheduled after this one. */
        fun shouldScheduleNextNag(nagCount: Int): Boolean = nagCount < NAG_LIMIT

        fun notificationTitle(isNag: Boolean): String =
            if (isNag) "Patch Still Needs Replacing!" else "Patch Replacement Due"

        fun notificationBody(location: String): String = "Time to replace: $location"

        fun notificationPriority(nagCount: Int): Int =
            if (nagCount >= 2) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_PATCH_DUE) return

        val patchId = intent.getIntExtra(AlarmScheduler.EXTRA_PATCH_ID, -1)
        val location = intent.getStringExtra(AlarmScheduler.EXTRA_PATCH_LOCATION) ?: return
        val isNag = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_NAG, false)
        val nagCount = intent.getIntExtra(AlarmScheduler.EXTRA_NAG_COUNT, 0)

        if (patchId == -1) return

        // Check DB to see if patch was already replaced (stops nag chain if confirmed)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = PatchDatabase.getInstance(context).patchDao()
                val patch = dao.getById(patchId)

                val now = System.currentTimeMillis()
                if (shouldSuppressNag(patch?.dueAt, now)) {
                    return@launch
                }

                showNotification(context, patchId, location, isNag, nagCount)

                if (shouldScheduleNextNag(nagCount)) {
                    AlarmScheduler.scheduleNagAlarm(context, patchId, location, nagCount)
                }
            } finally {
                pendingResult.finish()
            }
        }
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
            putExtra(MainActivity.EXTRA_DUE_PATCH_IDS, intArrayOf(patchId))
        }
        val tapPending = PendingIntent.getActivity(
            context,
            patchId, // unique per patch so intents don't overwrite each other
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = notificationTitle(isNag)
        val body = notificationBody(location)
        val priority = notificationPriority(nagCount)

        val notification = NotificationCompat.Builder(context, PatchApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(tapPending)
            .setGroup(GROUP_KEY)
            .build()

        // Summary notification to group co-due patches
        val summary = NotificationCompat.Builder(context, PatchApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Patches Need Replacing")
            .setPriority(priority)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(tapPending)
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(patchId, notification)
            manager.notify(0, summary)
        } catch (_: SecurityException) {
            // Notification permission not granted
        }
    }
}
