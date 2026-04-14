package com.patch.notifier.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Patch::class], version = 1, exportSchema = false)
abstract class PatchDatabase : RoomDatabase() {
    abstract fun patchDao(): PatchDao

    companion object {
        @Volatile
        private var INSTANCE: PatchDatabase? = null

        fun getInstance(context: Context): PatchDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PatchDatabase::class.java,
                    "patch_db",
                )
                    .addCallback(SeedCallback(context))
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

private class SeedCallback(private val context: Context) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Insert seed data directly via SQL to avoid any INSTANCE timing issues
        db.execSQL("INSERT OR IGNORE INTO patches (id, location, appliedAt, dueAt) VALUES (1, 'Left Upper', NULL, NULL)")
        db.execSQL("INSERT OR IGNORE INTO patches (id, location, appliedAt, dueAt) VALUES (2, 'Left Lower', NULL, NULL)")
        db.execSQL("INSERT OR IGNORE INTO patches (id, location, appliedAt, dueAt) VALUES (3, 'Right Upper', NULL, NULL)")
        db.execSQL("INSERT OR IGNORE INTO patches (id, location, appliedAt, dueAt) VALUES (4, 'Right Lower', NULL, NULL)")
    }
}
