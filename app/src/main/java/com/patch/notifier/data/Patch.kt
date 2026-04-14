package com.patch.notifier.data

import androidx.room.Entity
import androidx.room.PrimaryKey

const val PATCH_DURATION_MS = 7 * 24 * 60 * 60 * 1000L // 7 days

@Entity(tableName = "patches")
data class Patch(
    @PrimaryKey val id: Int,
    val location: String,
    val appliedAt: Long?,
    val dueAt: Long?,
)

fun suggestLocations(patches: List<Patch>, count: Int = 3): List<Int> {
    return patches
        .sortedWith(compareBy<Patch> { it.appliedAt ?: 0L }.thenBy { it.id })
        .take(count.coerceAtMost(patches.size))
        .map { it.id }
}
