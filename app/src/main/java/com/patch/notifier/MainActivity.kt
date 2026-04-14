package com.patch.notifier

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.patch.notifier.alarm.AlarmScheduler
import com.patch.notifier.data.PATCH_DURATION_MS
import com.patch.notifier.data.PatchDatabase
import com.patch.notifier.data.PatchPreferences
import com.patch.notifier.data.UserPreferences
import com.patch.notifier.data.suggestLocations
import com.patch.notifier.ui.Navy
import com.patch.notifier.widget.PatchWidgetReceiver
import com.patch.notifier.ui.PatchScreen
import com.patch.notifier.ui.PatchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_DUE_PATCH_IDS = "due_patch_ids"
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* granted or not, app still works */ }

    private val duePatchIds = mutableStateOf<Set<Int>?>(null)
    private val hasAutoSelected = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }

        duePatchIds.value = intent?.getIntArrayExtra(EXTRA_DUE_PATCH_IDS)?.toSet()

        val db = PatchDatabase.getInstance(this)
        val dao = db.patchDao()
        val appContext = applicationContext

        setContent {
            PatchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Navy,
                ) {
                    val patches by dao.observeAll().collectAsState(initial = emptyList())
                    val prefs by UserPreferences.observe(appContext)
                        .collectAsState(initial = PatchPreferences())
                    var selectedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
                    var autoSelected by hasAutoSelected
                    var showConfirmation by remember { mutableStateOf(false) }

                    LaunchedEffect(patches, autoSelected) {
                        if (patches.isNotEmpty() && !autoSelected) {
                            val fromNotification = duePatchIds.value
                            selectedIds = if (fromNotification != null && fromNotification.isNotEmpty()) {
                                duePatchIds.value = null
                                fromNotification
                            } else {
                                suggestLocations(patches, prefs.patchCount).toSet()
                            }
                            autoSelected = true
                        }
                    }

                    PatchScreen(
                        patches = patches,
                        selectedIds = selectedIds,
                        onToggle = { id ->
                            selectedIds = if (id in selectedIds) {
                                selectedIds - id
                            } else {
                                selectedIds + id
                            }
                        },
                        onReset = {
                            val context = this@MainActivity
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    val allPatches = dao.getAll()
                                    for (patch in allPatches) {
                                        dao.upsert(patch.copy(appliedAt = null, dueAt = null))
                                        AlarmScheduler.cancelAlarms(context, patch.id)
                                    }
                                    PatchWidgetReceiver.updateAllWidgets(context)
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Failed to reset patches", e)
                                }
                                launch(Dispatchers.Main) { autoSelected = false }
                            }
                        },
                        onConfirm = {
                            val now = System.currentTimeMillis()
                            val rawDueAt = now + PATCH_DURATION_MS
                            val dueAt = AlarmScheduler.adjustToNotifyTime(
                                rawDueAt, prefs.notifyHour, prefs.notifyMinute,
                            )
                            val context = this@MainActivity
                            val idsToReplace = selectedIds.toSet()
                            val currentPatches = patches.toList()

                            selectedIds = emptySet()
                            showConfirmation = true

                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    for (id in idsToReplace) {
                                        val patch = currentPatches.find { it.id == id } ?: continue
                                        dao.upsert(patch.copy(appliedAt = now, dueAt = dueAt))

                                        AlarmScheduler.cancelAlarms(context, id)
                                        AlarmScheduler.schedulePatchAlarm(
                                            context, id, patch.location, dueAt,
                                        )
                                    }
                                    PatchWidgetReceiver.updateAllWidgets(context)
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Failed to save patch replacement", e)
                                    launch(Dispatchers.Main) {
                                        selectedIds = idsToReplace
                                        showConfirmation = false
                                    }
                                }
                            }

                            lifecycleScope.launch {
                                delay(1500)
                                showConfirmation = false
                                autoSelected = false
                            }
                        },
                        showConfirmation = showConfirmation,
                        preferences = prefs,
                        onPatchCountChange = { count ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                UserPreferences.setPatchCount(appContext, count)
                                launch(Dispatchers.Main) { autoSelected = false }
                            }
                        },
                        onNotifyTimeChange = { hour, minute ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    UserPreferences.setNotifyTime(appContext, hour, minute)
                                    val activePatches = dao.getActivePatchesByDueDate()
                                    for (patch in activePatches) {
                                        val appliedAt = patch.appliedAt ?: continue
                                        val rawDueAt = appliedAt + PATCH_DURATION_MS
                                        val newDueAt = AlarmScheduler.adjustToNotifyTime(rawDueAt, hour, minute)
                                        dao.upsert(patch.copy(dueAt = newDueAt))
                                        AlarmScheduler.cancelAlarms(appContext, patch.id)
                                        AlarmScheduler.schedulePatchAlarm(appContext, patch.id, patch.location, newDueAt)
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Failed to reschedule alarms for new time", e)
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val ids = intent.getIntArrayExtra(EXTRA_DUE_PATCH_IDS)?.toSet()
        if (ids != null && ids.isNotEmpty()) {
            duePatchIds.value = ids
            hasAutoSelected.value = false
        }
    }
}
