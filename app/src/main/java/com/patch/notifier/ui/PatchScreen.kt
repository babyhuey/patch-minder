package com.patch.notifier.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patch.notifier.data.Patch
import com.patch.notifier.data.PatchPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PatchScreen(
    patches: List<Patch>,
    selectedIds: Set<Int>,
    onToggle: (Int) -> Unit,
    onConfirm: () -> Unit,
    onReset: () -> Unit,
    onRemove: (Int) -> Unit,
    showConfirmation: Boolean,
    preferences: PatchPreferences,
    onPatchCountChange: (Int) -> Unit,
    onNotifyTimeChange: (Int, Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var pendingRemovalId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(pendingRemovalId) {
        if (pendingRemovalId != null) {
            @Suppress("RemoveRedundantQualifierName")
            kotlinx.coroutines.delay(3000L)
            pendingRemovalId = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))

        StatusHeader(patches)

        Spacer(Modifier.height(32.dp))

        LocationGrid(
            patches = patches,
            selectedIds = selectedIds,
            pendingRemovalId = pendingRemovalId,
            onToggle = { id ->
                if (pendingRemovalId == id) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    pendingRemovalId = null
                    onRemove(id)
                } else {
                    if (pendingRemovalId != null) pendingRemovalId = null
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggle(id)
                }
            },
            onLongPress = { patch ->
                val active = patch.dueAt != null && patch.dueAt > 0
                if (active) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (patch.id in selectedIds) onToggle(patch.id)
                    pendingRemovalId = patch.id
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = if (pendingRemovalId != null)
                "Tap red slot again to remove · or wait to cancel"
            else
                "${selectedIds.size} selected · tap to toggle · long-press to remove",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onConfirm()
            },
            enabled = selectedIds.isNotEmpty() && !showConfirmation,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Blue,
                disabledContainerColor = NavyLight,
            ),
        ) {
            Text(
                text = if (showConfirmation) "✓ DONE" else "REPLACED ${selectedIds.size} PATCHES",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            )
        }

        Spacer(Modifier.height(24.dp))

        SettingsSection(
            preferences = preferences,
            onPatchCountChange = onPatchCountChange,
            onNotifyTimeChange = onNotifyTimeChange,
            onReset = onReset,
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatusHeader(patches: List<Patch>) {
    val activeDueDates = patches.mapNotNull { it.dueAt }.filter { it > 0 }

    if (activeDueDates.isEmpty()) {
        Text(
            text = "No active patches",
            color = TextSecondary,
            fontSize = 14.sp,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Select locations to get started",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
    } else {
        val earliest = activeDueDates.min()
        val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        val now = System.currentTimeMillis()
        val status = formatStatusInfo(earliest, activeDueDates.size, now)

        Text(
            text = status.headerLabel,
            color = if (status.isOverdue) Color(0xFFEF4444) else TextSecondary,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = dateFormat.format(Date(earliest)),
            color = if (status.isOverdue) Color(0xFFEF4444) else TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = status.timeText,
            color = if (status.isOverdue) Color(0xFFEF4444) else TextMuted,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun LocationGrid(
    patches: List<Patch>,
    selectedIds: Set<Int>,
    pendingRemovalId: Int?,
    onToggle: (Int) -> Unit,
    onLongPress: (Patch) -> Unit,
) {
    val leftPatches = patches.filter { it.location.startsWith("Left") }
        .sortedBy { if (it.location.contains("Upper")) 0 else 1 }
    val rightPatches = patches.filter { it.location.startsWith("Right") }
        .sortedBy { if (it.location.contains("Upper")) 0 else 1 }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ThighColumn(
            label = "LEFT THIGH",
            patches = leftPatches,
            selectedIds = selectedIds,
            pendingRemovalId = pendingRemovalId,
            onToggle = onToggle,
            onLongPress = onLongPress,
            modifier = Modifier.weight(1f),
        )
        ThighColumn(
            label = "RIGHT THIGH",
            patches = rightPatches,
            selectedIds = selectedIds,
            pendingRemovalId = pendingRemovalId,
            onToggle = onToggle,
            onLongPress = onLongPress,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ThighColumn(
    label: String,
    patches: List<Patch>,
    selectedIds: Set<Int>,
    pendingRemovalId: Int?,
    onToggle: (Int) -> Unit,
    onLongPress: (Patch) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavyLight),
            border = BorderStroke(2.dp, BorderColor),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (patch in patches) {
                    val selected = patch.id in selectedIds
                    LocationButton(
                        label = patch.location.substringAfter(" "),
                        selected = selected,
                        pendingRemoval = pendingRemovalId == patch.id,
                        dueAt = patch.dueAt,
                        onClick = { onToggle(patch.id) },
                        onLongClick = { onLongPress(patch) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocationButton(
    label: String,
    selected: Boolean,
    pendingRemoval: Boolean,
    dueAt: Long?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val hasActivePatch = dueAt != null && dueAt > 0

    val backgroundColor by animateColorAsState(
        targetValue = when {
            pendingRemoval -> Color(0xFFEF4444)
            selected -> Blue
            hasActivePatch -> Color(0xFF1E2A5E)
            else -> Navy
        },
        label = "bg",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            pendingRemoval -> Color(0xFFFCA5A5)
            selected -> BlueBright
            hasActivePatch -> Color(0xFF3A4A8A)
            else -> BorderColor
        },
        label = "border",
    )
    val textColor by animateColorAsState(
        targetValue = when {
            pendingRemoval -> TextPrimary
            selected -> TextPrimary
            hasActivePatch -> TextSecondary
            else -> TextMuted
        },
        label = "text",
    )

    val daysLeftText = dueAt?.let { formatDaysLeft(it, System.currentTimeMillis()) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(2.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = when {
                    pendingRemoval -> "✕ $label"
                    selected -> "✓ $label"
                    hasActivePatch -> "● $label"
                    else -> label
                },
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
            if (pendingRemoval) {
                Text(
                    text = "TAP TO REMOVE",
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                )
            } else if (daysLeftText != null) {
                Text(
                    text = daysLeftText,
                    color = when {
                        daysLeftText.contains("overdue") -> Color(0xFFEF4444)
                        daysLeftText.contains("due today") -> Color(0xFFF59E0B)
                        else -> TextMuted
                    },
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// --- Settings Section ---

@Composable
private fun SettingsSection(
    preferences: PatchPreferences,
    onPatchCountChange: (Int) -> Unit,
    onNotifyTimeChange: (Int, Int) -> Unit,
    onReset: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // Auto-dismiss reset confirmation after 3 seconds
    LaunchedEffect(showResetConfirm) {
        if (showResetConfirm) {
            @Suppress("RemoveRedundantQualifierName")
            kotlinx.coroutines.delay(3000L)
            showResetConfirm = false
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextButton(onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            expanded = !expanded
        }) {
            Text(
                text = if (expanded) "▲ Settings" else "▼ Settings",
                color = TextMuted,
                fontSize = 13.sp,
            )
        }

        if (expanded) {
            Spacer(Modifier.height(12.dp))

            SettingRow(label = "Patches per change") {
                StepperControl(
                    value = preferences.patchCount,
                    min = 1,
                    max = 4,
                    onValueChange = onPatchCountChange,
                )
            }

            Spacer(Modifier.height(16.dp))

            SettingRow(label = "Remind me at") {
                TimeStepperControl(
                    hour = preferences.notifyHour,
                    minute = preferences.notifyMinute,
                    onTimeChange = onNotifyTimeChange,
                )
            }

            Spacer(Modifier.height(24.dp))

            if (showResetConfirm) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onReset()
                        showResetConfirm = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                    ),
                ) {
                    Text("TAP AGAIN TO CONFIRM RESET", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showResetConfirm = true
                }) {
                    Text("Reset all patches", color = Color(0xFFEF4444), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 14.sp,
        )
        content()
    }
}

@Composable
private fun StepperControl(
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = NavyLight),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .size(36.dp)
                .clickable(enabled = value > min) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(value - 1)
                },
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("−", color = if (value > min) TextPrimary else TextMuted, fontSize = 18.sp)
            }
        }

        Text(
            text = "$value",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = NavyLight),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .size(36.dp)
                .clickable(enabled = value < max) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(value + 1)
                },
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("+", color = if (value < max) TextPrimary else TextMuted, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun TimeStepperControl(
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val amPm = if (hour < 12) "AM" else "PM"
    val displayMinute = String.format("%02d", minute)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = NavyLight),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .size(36.dp)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val newHour = if (hour == 0) 23 else hour - 1
                    onTimeChange(newHour, minute)
                },
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("−", color = TextPrimary, fontSize = 18.sp)
            }
        }

        Text(
            text = "$displayHour:$displayMinute $amPm",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = NavyLight),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .size(36.dp)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val newHour = if (hour == 23) 0 else hour + 1
                    onTimeChange(newHour, minute)
                },
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("+", color = TextPrimary, fontSize = 18.sp)
            }
        }
    }
}
