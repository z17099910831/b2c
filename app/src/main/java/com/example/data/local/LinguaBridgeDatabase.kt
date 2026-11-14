package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RecordingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LinguaBridgeDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao

    companion object {
        @Volatile
        private var INSTANCE: LinguaBridgeDatabase? = null

        fun getInstance(context: Context): LinguaBridgeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LinguaBridgeDatabase::class.java,
                    "linguabridge_offline.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
