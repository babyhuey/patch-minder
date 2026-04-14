package com.patch.notifier

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import com.patch.notifier.alarm.AlarmScheduler
import com.patch.notifier.data.PatchDatabase
import com.patch.notifier.data.suggestLocations
import com.patch.notifier.ui.Navy
import com.patch.notifier.ui.PatchScreen
import com.patch.notifier.ui.PatchTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* granted or not, app still works */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Request exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }

        val db = PatchDatabase.getInstance(this)
        val dao = db.patchDao()

        setContent {
            PatchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Navy,
                ) {
                    val patches by dao.observeAll().collectAsState(initial = emptyList())
                    var selectedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
                    var suggestionsApplied by remember { mutableStateOf(false) }
                    var showConfirmation by remember { mutableStateOf(false) }

                    LaunchedEffect(patches) {
                        if (patches.isNotEmpty() && !suggestionsApplied) {
                            selectedIds = suggestLocations(patches).toSet()
                            suggestionsApplied = true
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
                        onConfirm = {
                            val now = System.currentTimeMillis()
                            val dueAt = now + 7 * 24 * 60 * 60 * 1000L
                            val context = this@MainActivity
                            val idsToReplace = selectedIds.toSet()

                            selectedIds = emptySet()
                            showConfirmation = true

                            CoroutineScope(Dispatchers.IO).launch {
                                for (id in idsToReplace) {
                                    val patch = patches.find { it.id == id } ?: continue
                                    val updated = patch.copy(appliedAt = now, dueAt = dueAt)
                                    dao.upsert(updated)

                                    AlarmScheduler.cancelAlarms(context, id)
                                    AlarmScheduler.schedulePatchAlarm(
                                        context, id, patch.location, dueAt,
                                    )
                                }
                            }

                            CoroutineScope(Dispatchers.Main).launch {
                                delay(1500)
                                showConfirmation = false
                            }
                        },
                        showConfirmation = showConfirmation,
                    )
                }
            }
        }
    }
}
