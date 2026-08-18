package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.WifiDao
import com.example.data.local.entity.SavedNetworkEntity
import com.example.data.local.entity.ScanHistoryEntity
import com.example.data.local.entity.SpeedTestHistoryEntity
import com.example.data.local.entity.NetworkJourneyEventEntity
import com.example.data.local.entity.HeatmapObservationEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SavedNetworkEntity::class, ScanHistoryEntity::class, SpeedTestHistoryEntity::class, NetworkJourneyEventEntity::class, HeatmapObservationEntity::class],
    version = 3,
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS speed_test_history (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "networkName TEXT NOT NULL, " +
                        "downloadMbps REAL NOT NULL, " +
                        "uploadMbps REAL NOT NULL, " +
                        "pingMs REAL NOT NULL, " +
                        "jitterMs REAL NOT NULL, " +
                        "durationMs INTEGER NOT NULL, " +
                        "timestamp INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS network_journey_events (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, timestamp INTEGER NOT NULL, ssid TEXT, bssid TEXT, rssi INTEGER, band TEXT, healthScore INTEGER, predictionState TEXT, candidateSsid TEXT, candidateRssi INTEGER, title TEXT NOT NULL, detail TEXT NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS heatmap_observations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, timestamp INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, ssid TEXT NOT NULL, bssid TEXT NOT NULL, rssi INTEGER NOT NULL, frequency INTEGER NOT NULL)")
            }
        }
    }
}
