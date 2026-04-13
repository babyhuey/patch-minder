package com.patch.notifier.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patches")
data class Patch(
    @PrimaryKey val id: Int,
    val location: String,
    val appliedAt: Long?,
    val dueAt: Long?,
)

fun suggestLocations(patches: List<Patch>, count: Int = 3): List<Int> {
    return patches
        .sortedBy { it.appliedAt ?: 0L }
        .take(count)
        .map { it.id }
}
