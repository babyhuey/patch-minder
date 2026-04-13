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
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).patchDao().upsertAll(
                                    listOf(
                                        Patch(1, "Left Upper", null, null),
                                        Patch(2, "Left Lower", null, null),
                                        Patch(3, "Right Upper", null, null),
                                        Patch(4, "Right Lower", null, null),
                                    )
                                )
                            }
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
