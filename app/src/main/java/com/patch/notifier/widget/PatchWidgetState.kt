package com.patch.notifier.widget

import java.util.concurrent.TimeUnit

enum class WidgetUrgency { GOOD, SOON, OVERDUE, NONE }

data class PatchWidgetState(
    val daysUntilDue: Long?,
    val activePatchCount: Int,
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
            daysUntilDue == 0L -> "0d"
            else -> "${daysUntilDue}d"
        }

    val subtitleText: String
        get() = when {
            daysUntilDue == null -> "no patches"
            daysUntilDue < 0 -> "overdue"
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
    val daysUntilDue = earliestDueAt?.let {
        val deltaMs = it - nowMs
        if (deltaMs < 0) {
            -1L // overdue (collapse all negative to -1)
        } else {
            TimeUnit.MILLISECONDS.toDays(deltaMs)
        }
    }
    return PatchWidgetState(daysUntilDue, activePatchCount)
}
