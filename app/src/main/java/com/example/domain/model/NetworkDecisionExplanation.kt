package com.example.domain.model

data class NetworkMetrics(
    val ssid: String?,
    val bssid: String?,
    val rssi: Int?,
    val frequencyMHz: Int?,
    val channel: Int?,
    val band: String?,
    val security: String?
)

data class PredictionEvidence(
    val currentRssi: Int,
    val trend: SignalTrend,
    val historicalSampleCount: Int,
    val score: Int,
    val likelihood: RoamingLikelihood,
    val recommendation: HandoffRecommendation,
    val candidateRssiImprovement: Int?
)

data class AnalyzerEvidence(
    val healthScore: Int,
    val rssiContribution: Int,
    val congestionContribution: Int,
    val securityContribution: Int,
    val frequencyContribution: Int,
    val observedAccessPointCount: Int,
    val crowdedBand: String?,
    val crowdedBandAccessPointCount: Int?,
    val recommendedChannel: Int?,
    val recommendedChannelObservedAccessPoints: Int?
)

data class SpeedTestEvidence(
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Double,
    val jitterMs: Double,
    val timestamp: Long
)

data class NetworkDecisionExplanation(
    val decision: String,
    val primaryReason: String,
    val supportingFactors: List<String>,
    val currentNetwork: NetworkMetrics?,
    val candidateNetwork: NetworkMetrics?,
    val prediction: PredictionEvidence?,
    val analyzer: AnalyzerEvidence?,
    val speedTest: SpeedTestEvidence?,
    val expectedBenefit: String,
    val confidence: Int?,
    val timestamp: Long
)
