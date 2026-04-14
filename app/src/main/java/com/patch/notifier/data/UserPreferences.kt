package com.patch.notifier.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class PatchPreferences(
    val patchCount: Int = 3,
    val notifyHour: Int = 9,
    val notifyMinute: Int = 0,
)

object UserPreferences {
    private val PATCH_COUNT = intPreferencesKey("patch_count")
    private val NOTIFY_HOUR = intPreferencesKey("notify_hour")
    private val NOTIFY_MINUTE = intPreferencesKey("notify_minute")

    fun observe(context: Context): Flow<PatchPreferences> {
        return context.dataStore.data.map { prefs ->
            PatchPreferences(
                patchCount = prefs[PATCH_COUNT] ?: 3,
                notifyHour = prefs[NOTIFY_HOUR] ?: 9,
                notifyMinute = prefs[NOTIFY_MINUTE] ?: 0,
            )
        }
    }

    suspend fun setPatchCount(context: Context, count: Int) {
        context.dataStore.edit { it[PATCH_COUNT] = count.coerceIn(1, 4) }
    }

    suspend fun setNotifyTime(context: Context, hour: Int, minute: Int) {
        context.dataStore.edit {
            it[NOTIFY_HOUR] = hour.coerceIn(0, 23)
            it[NOTIFY_MINUTE] = minute.coerceIn(0, 59)
        }
    }
}
