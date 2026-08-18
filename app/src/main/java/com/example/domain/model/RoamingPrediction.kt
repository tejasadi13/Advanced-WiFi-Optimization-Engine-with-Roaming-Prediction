package com.example.domain.model

enum class SignalTrend {
    IMPROVING,
    STABLE,
    DEGRADING
}

enum class RoamingLikelihood {
    LOW,
    MEDIUM,
    HIGH
}

enum class HandoffRecommendation {
    STAY_CONNECTED,
    PREPARE_ROAMING,
    ROAM_NOW
}

data class RoamingPrediction(
    val currentRssi: Int,
    val trend: SignalTrend,
    val predictionScore: Int,
    val likelihood: RoamingLikelihood,
    val recommendedAccessPoint: WifiNetwork?,
    val recommendation: HandoffRecommendation,
    val timestamp: Long,
    val rssiSampleCount: Int = 0
) {
    // Compatibility accessors keep existing presentation code independent of the new engine shape.
    val candidateSsid: String get() = recommendedAccessPoint?.ssid.orEmpty()
    val predictionConfidence: Float get() = predictionScore / 100f
    val recommendedAction: String get() = recommendation.name
    val signalTrendCurrent: String get() = trend.name
}
