package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SavedNetworkEntity
import com.example.data.local.entity.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WifiDao {
    @Query("SELECT * FROM saved_networks ORDER BY lastUpdated DESC")
    fun getAllSavedNetworks(): Flow<List<SavedNetworkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedNetwork(network: SavedNetworkEntity)

    @Query("DELETE FROM saved_networks WHERE bssid = :bssid")
    suspend fun deleteSavedNetwork(bssid: String)

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScanHistory(): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanHistory(history: ScanHistoryEntity)

    @Query("DELETE FROM scan_history")
    suspend fun clearAllScanHistory()
}
