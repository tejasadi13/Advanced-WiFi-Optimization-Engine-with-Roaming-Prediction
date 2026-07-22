package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.WifiDao
import com.example.data.local.entity.SavedNetworkEntity
import com.example.data.local.entity.ScanHistoryEntity

@Database(
    entities = [SavedNetworkEntity::class, ScanHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WifiDatabase : RoomDatabase() {
    abstract val wifiDao: WifiDao

    companion object {
        @Volatile
        private var INSTANCE: WifiDatabase? = null

        fun getDatabase(context: Context): WifiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WifiDatabase::class.java,
                    "wifi_wise_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
