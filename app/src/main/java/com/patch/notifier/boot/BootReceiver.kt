package com.patch.notifier.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.patch.notifier.alarm.AlarmScheduler
import com.patch.notifier.data.PATCH_DURATION_MS
import com.patch.notifier.data.PatchDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = PatchDatabase.getInstance(context).patchDao()
                val patches = dao.getActivePatchesByDueDate()
                val now = System.currentTimeMillis()

                for (patch in patches) {
                    val dueAt = patch.dueAt ?: continue
                    if (dueAt > now) {
                        AlarmScheduler.schedulePatchAlarm(
                            context, patch.id, patch.location, dueAt,
                        )
                    } else if (now - dueAt < PATCH_DURATION_MS) {
                        AlarmScheduler.scheduleNagAlarm(
                            context, patch.id, patch.location, nagCount = 0,
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to reschedule alarms after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
