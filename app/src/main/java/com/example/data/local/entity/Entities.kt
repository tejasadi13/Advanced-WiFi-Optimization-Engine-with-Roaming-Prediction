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
