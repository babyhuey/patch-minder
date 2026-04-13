package com.patch.notifier

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

        val db = PatchDatabase.getInstance(this)
        val dao = db.patchDao()

        setContent {
            PatchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Navy,
                ) {
                    val patches by dao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
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

                            CoroutineScope(Dispatchers.IO).launch {
                                for (id in selectedIds) {
                                    val patch = patches.find { it.id == id } ?: continue
                                    val updated = patch.copy(appliedAt = now, dueAt = dueAt)
                                    dao.upsert(updated)

                                    AlarmScheduler.cancelAlarms(context, id)
                                    AlarmScheduler.schedulePatchAlarm(
                                        context, id, patch.location, dueAt,
                                    )
                                }
                            }

                            showConfirmation = true
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(1500)
                                showConfirmation = false
                                suggestionsApplied = false
                            }
                        },
                        showConfirmation = showConfirmation,
                    )
                }
            }
        }
    }
}
