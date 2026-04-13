package com.patch.notifier.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object AlarmScheduler {
    private const val PATCH_ALARM_BASE = 1000
    private const val NAG_ALARM_BASE = 2000
    private const val FIRST_NAG_DELAY_MS = 3_600_000L   // 1 hour
    private const val REPEAT_NAG_DELAY_MS = 7_200_000L  // 2 hours

    const val EXTRA_PATCH_ID = "patch_id"
    const val EXTRA_PATCH_LOCATION = "patch_location"
    const val EXTRA_NAG_COUNT = "nag_count"
    const val EXTRA_IS_NAG = "is_nag"
    const val ACTION_PATCH_DUE = "com.patch.notifier.PATCH_DUE"

    fun patchAlarmRequestCode(patchId: Int): Int = PATCH_ALARM_BASE + patchId
    fun nagAlarmRequestCode(patchId: Int): Int = NAG_ALARM_BASE + patchId

    fun nagDelayMs(nagCount: Int): Long {
        return if (nagCount == 0) FIRST_NAG_DELAY_MS else REPEAT_NAG_DELAY_MS
    }

    fun schedulePatchAlarm(context: Context, patchId: Int, location: String, triggerAtMs: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_PATCH_DUE
            putExtra(EXTRA_PATCH_ID, patchId)
            putExtra(EXTRA_PATCH_LOCATION, location)
            putExtra(EXTRA_IS_NAG, false)
            putExtra(EXTRA_NAG_COUNT, 0)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            patchAlarmRequestCode(patchId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMs,
            pendingIntent,
        )
    }

    fun scheduleNagAlarm(context: Context, patchId: Int, location: String, nagCount: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerAt = System.currentTimeMillis() + nagDelayMs(nagCount)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_PATCH_DUE
            putExtra(EXTRA_PATCH_ID, patchId)
            putExtra(EXTRA_PATCH_LOCATION, location)
            putExtra(EXTRA_IS_NAG, true)
            putExtra(EXTRA_NAG_COUNT, nagCount + 1)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            nagAlarmRequestCode(patchId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent,
        )
    }

    fun cancelAlarms(context: Context, patchId: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_PATCH_DUE
        }
        for (requestCode in listOf(patchAlarmRequestCode(patchId), nagAlarmRequestCode(patchId))) {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }
}
