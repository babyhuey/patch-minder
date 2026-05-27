package com.patch.notifier.widget

import java.util.concurrent.TimeUnit

enum class WidgetUrgency { GOOD, SOON, OVERDUE, NONE }

data class PatchWidgetState(
    val daysUntilDue: Long?,
    val activePatchCount: Int,
    val hoursUntilDue: Long? = null,
) {
    val urgency: WidgetUrgency
        get() = when {
            daysUntilDue == null -> WidgetUrgency.NONE
            daysUntilDue < 0 -> WidgetUrgency.OVERDUE
            daysUntilDue <= 1 -> WidgetUrgency.SOON
            else -> WidgetUrgency.GOOD
        }

    val displayText: String
        get() = when {
            daysUntilDue == null -> "—"
            daysUntilDue < 0 -> "!"
            daysUntilDue == 0L && hoursUntilDue != null -> "${hoursUntilDue}h"
            daysUntilDue == 0L -> "0d"
            else -> "${daysUntilDue}d"
        }

    val subtitleText: String
        get() = when {
            daysUntilDue == null -> "no patches"
            daysUntilDue < 0 -> "overdue"
            daysUntilDue == 0L && hoursUntilDue != null -> "left"
            daysUntilDue == 0L -> "today"
            daysUntilDue == 1L -> "tomorrow"
            else -> "left"
        }
}

fun computeWidgetState(
    earliestDueAt: Long?,
    activePatchCount: Int,
    nowMs: Long,
): PatchWidgetState {
    if (earliestDueAt == null) return PatchWidgetState(null, activePatchCount)
    val deltaMs = earliestDueAt - nowMs
    if (deltaMs < 0) return PatchWidgetState(-1L, activePatchCount)
    val days = TimeUnit.MILLISECONDS.toDays(deltaMs)
    val hours = if (days == 0L) (deltaMs + 1_800_000L) / 3_600_000L else null
    return PatchWidgetState(days, activePatchCount, hours)
}
