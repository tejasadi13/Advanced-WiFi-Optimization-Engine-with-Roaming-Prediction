package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_networks")
data class SavedNetworkEntity(
    @PrimaryKey val bssid: String,
    val ssid: String,
    val security: String,
    val lastRssi: Int,
    val frequency: Int,
    val isPreferred: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val networkCount: Int,
    val averageRssi: Int,
    val optimizedCount: Int,
    val securityIssuesFound: Int,
    val statusMessage: String
)

@Entity(tableName = "speed_test_history")
data class SpeedTestHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val networkName: String,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Double,
    val jitterMs: Double,
    val durationMs: Long,
    val timestamp: Long
)

@Entity(tableName = "network_journey_events")
data class NetworkJourneyEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val timestamp: Long,
    val ssid: String?,
    val bssid: String?,
    val rssi: Int?,
    val band: String?,
    val healthScore: Int?,
    val predictionState: String?,
    val candidateSsid: String?,
    val candidateRssi: Int?,
    val title: String,
    val detail: String
)

@Entity(tableName = "heatmap_observations")
data class HeatmapObservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int
)
