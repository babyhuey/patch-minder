package com.patch.notifier.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PatchDao {
    @Query("SELECT * FROM patches ORDER BY id")
    fun observeAll(): Flow<List<Patch>>

    @Query("SELECT * FROM patches ORDER BY id")
    suspend fun getAll(): List<Patch>

    @Upsert
    suspend fun upsert(patch: Patch)

    @Upsert
    suspend fun upsertAll(patches: List<Patch>)

    @Query("SELECT * FROM patches WHERE id = :id")
    suspend fun getById(id: Int): Patch?

    @Query("SELECT * FROM patches WHERE dueAt IS NOT NULL AND dueAt > 0 ORDER BY dueAt")
    suspend fun getActivePatchesByDueDate(): List<Patch>
}
