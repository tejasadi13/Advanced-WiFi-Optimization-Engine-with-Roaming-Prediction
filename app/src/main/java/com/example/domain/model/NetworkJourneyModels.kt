package com.example.domain.model

enum class NetworkJourneyEventType {
    CONNECTED, DISCONNECTED, SIGNAL_IMPROVED, SIGNAL_DEGRADED,
    HEALTH_CHANGED, PREPARE_ROAMING, ROAM_NOW, CANDIDATE_DETECTED,
    RECOMMENDATION_GENERATED, SPEED_TEST_COMPLETED
}

data class NetworkJourneyEvent(
    val id: Long = 0,
    val type: NetworkJourneyEventType,
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
    val detail: String,
    val explanation: NetworkDecisionExplanation? = null
)

data class HeatmapObservation(
    val id: Long = 0,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int
)

data class HeatmapSummary(
    val observations: List<HeatmapObservation>,
    val selectedSsid: String?,
    val selectedBssid: String?,
    val strongestRssi: Int?,
    val weakestRssi: Int?,
    val averageRssi: Int?,
    val lastObservationTimestamp: Long?
)
