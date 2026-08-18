package com.example.domain.model

import java.util.UUID

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val channel: Int,
    val securityType: String,
    val isConnected: Boolean = false,
    val estimatedSpeedMbps: Int = 0,
    val is5GHz: Boolean = frequency > 4000
) {
    val signalLevel: Int
        get() = when {
            rssi >= -50 -> 4 // Excellent
            rssi >= -65 -> 3 // Good
            rssi >= -75 -> 2 // Fair
            rssi >= -85 -> 1 // Poor
            else -> 0        // No signal
        }
}

data class WifiRecommendation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val priority: Priority,
    val category: Category,
    val isApplied: Boolean = false
) {
    enum class Priority { HIGH, MEDIUM, LOW }
    enum class Category { SECURITY, PERFORMANCE, COVERAGE }
}

data class ScanHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long,
    val averageSignalStrength: Int,
    val networkCount: Int,
    val optimizedCount: Int,
    val securityIssuesFound: Int,
    val statusMessage: String
)
