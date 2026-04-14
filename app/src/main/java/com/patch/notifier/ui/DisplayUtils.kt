package com.patch.notifier.ui

import java.util.concurrent.TimeUnit

data class StatusInfo(
    val headerLabel: String,
    val timeText: String,
    val isOverdue: Boolean,
)

fun formatStatusInfo(earliestDueAt: Long, patchCount: Int, nowMs: Long): StatusInfo {
    val deltaMs = earliestDueAt - nowMs
    val isOverdue = deltaMs < 0
    val daysAway = TimeUnit.MILLISECONDS.toDays(deltaMs)

    val headerLabel = if (isOverdue) "OVERDUE" else "NEXT REPLACEMENT"
    val timeText = when {
        isOverdue -> "$patchCount patches · overdue!"
        daysAway == 0L -> "$patchCount patches · due today"
        daysAway == 1L -> "$patchCount patches · tomorrow"
        else -> "$patchCount patches · ${daysAway}d from now"
    }
    return StatusInfo(headerLabel, timeText, isOverdue)
}

fun formatDaysLeft(dueAt: Long, nowMs: Long): String {
    val deltaMs = dueAt - nowMs
    val days = TimeUnit.MILLISECONDS.toDays(deltaMs)
    return when {
        deltaMs < 0 -> "overdue!"
        days == 0L -> "due today"
        days == 1L -> "1 day left"
        else -> "${days}d left"
    }
}
