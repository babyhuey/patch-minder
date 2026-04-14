package com.patch.notifier.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patch.notifier.data.Patch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun PatchScreen(
    patches: List<Patch>,
    selectedIds: Set<Int>,
    onToggle: (Int) -> Unit,
    onConfirm: () -> Unit,
    showConfirmation: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))

        StatusHeader(patches)

        Spacer(Modifier.height(32.dp))

        LocationGrid(
            patches = patches,
            selectedIds = selectedIds,
            onToggle = onToggle,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "${selectedIds.size} selected · tap to toggle",
            color = TextSecondary,
            fontSize = 14.sp,
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onConfirm,
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

        Spacer(Modifier.height(32.dp))
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
        val daysAway = TimeUnit.MILLISECONDS.toDays(earliest - System.currentTimeMillis())
        val patchCount = activeDueDates.size

        val isOverdue = daysAway < 0
        val headerLabel = if (isOverdue) "OVERDUE" else "NEXT REPLACEMENT"
        val timeText = when {
            daysAway < 0 -> "$patchCount patches · overdue!"
            daysAway == 0L -> "$patchCount patches · due today"
            daysAway == 1L -> "$patchCount patches · tomorrow"
            else -> "$patchCount patches · ${daysAway}d from now"
        }

        Text(
            text = headerLabel,
            color = if (isOverdue) Color(0xFFEF4444) else TextSecondary,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = dateFormat.format(Date(earliest)),
            color = if (isOverdue) Color(0xFFEF4444) else TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = timeText,
            color = if (isOverdue) Color(0xFFEF4444) else TextMuted,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun LocationGrid(
    patches: List<Patch>,
    selectedIds: Set<Int>,
    onToggle: (Int) -> Unit,
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
            onToggle = onToggle,
            modifier = Modifier.weight(1f),
        )
        ThighColumn(
            label = "RIGHT THIGH",
            patches = rightPatches,
            selectedIds = selectedIds,
            onToggle = onToggle,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ThighColumn(
    label: String,
    patches: List<Patch>,
    selectedIds: Set<Int>,
    onToggle: (Int) -> Unit,
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
                        dueAt = patch.dueAt,
                        onClick = { onToggle(patch.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationButton(
    label: String,
    selected: Boolean,
    dueAt: Long?,
    onClick: () -> Unit,
) {
    val hasActivePatch = dueAt != null && dueAt > 0

    val backgroundColor by animateColorAsState(
        targetValue = when {
            selected -> Blue
            hasActivePatch -> Color(0xFF1E2A5E) // subtle blue tint for active
            else -> Navy
        },
        label = "bg",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> BlueBright
            hasActivePatch -> Color(0xFF3A4A8A) // soft blue border for active
            else -> BorderColor
        },
        label = "border",
    )
    val textColor by animateColorAsState(
        targetValue = when {
            selected -> TextPrimary
            hasActivePatch -> TextSecondary
            else -> TextMuted
        },
        label = "text",
    )

    val daysLeftText = dueAt?.let {
        val days = TimeUnit.MILLISECONDS.toDays(it - System.currentTimeMillis())
        when {
            days < 0 -> "overdue!"
            days == 0L -> "due today"
            days == 1L -> "1 day left"
            else -> "${days}d left"
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(2.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = when {
                    selected -> "✓ $label"
                    hasActivePatch -> "● $label"
                    else -> label
                },
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
            if (daysLeftText != null) {
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
