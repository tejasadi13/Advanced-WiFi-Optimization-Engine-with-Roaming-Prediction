package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SavedNetworkEntity
import com.example.data.local.entity.ScanHistoryEntity
import com.example.data.local.entity.SpeedTestHistoryEntity
import com.example.data.local.entity.NetworkJourneyEventEntity
import com.example.data.local.entity.HeatmapObservationEntity
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

    @Query("SELECT * FROM speed_test_history ORDER BY timestamp DESC")
    fun getSpeedTestHistory(): Flow<List<SpeedTestHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeedTest(result: SpeedTestHistoryEntity)

    @Query("SELECT * FROM network_journey_events ORDER BY timestamp DESC")
    fun getNetworkJourneyEvents(): Flow<List<NetworkJourneyEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetworkJourneyEvent(event: NetworkJourneyEventEntity)

    @Query("SELECT * FROM heatmap_observations ORDER BY timestamp DESC")
    fun getHeatmapObservations(): Flow<List<HeatmapObservationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeatmapObservation(observation: HeatmapObservationEntity)
}
