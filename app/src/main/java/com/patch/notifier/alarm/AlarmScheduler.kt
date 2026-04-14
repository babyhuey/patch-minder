package com.patch.notifier.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

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

    private fun canScheduleExact(context: Context): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun scheduleAlarm(context: Context, requestCode: Int, intent: Intent, triggerAtMs: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (canScheduleExact(context)) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                pendingIntent,
            )
        } else {
            // Fallback to inexact alarm — still fires, just not at the exact ms
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                pendingIntent,
            )
        }
    }

    /**
     * Adjusts a raw dueAt timestamp to fire at the preferred notification time on that day.
     * If the preferred time has already passed on the due day, fires at the preferred time
     * the next day.
     */
    fun adjustToNotifyTime(dueAtMs: Long, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dueAtMs
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // If preferred time on due day is before the actual due time,
        // the patch isn't due yet at that hour — use the same day.
        // If preferred time is after now, use it. Otherwise next day.
        if (cal.timeInMillis < System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }

    fun schedulePatchAlarm(context: Context, patchId: Int, location: String, triggerAtMs: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_PATCH_DUE
            putExtra(EXTRA_PATCH_ID, patchId)
            putExtra(EXTRA_PATCH_LOCATION, location)
            putExtra(EXTRA_IS_NAG, false)
            putExtra(EXTRA_NAG_COUNT, 0)
        }
        scheduleAlarm(context, patchAlarmRequestCode(patchId), intent, triggerAtMs)
    }

    fun scheduleNagAlarm(context: Context, patchId: Int, location: String, nagCount: Int) {
        val triggerAt = System.currentTimeMillis() + nagDelayMs(nagCount)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_PATCH_DUE
            putExtra(EXTRA_PATCH_ID, patchId)
            putExtra(EXTRA_PATCH_LOCATION, location)
            putExtra(EXTRA_IS_NAG, true)
            putExtra(EXTRA_NAG_COUNT, nagCount + 1)
        }
        scheduleAlarm(context, nagAlarmRequestCode(patchId), intent, triggerAt)
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
