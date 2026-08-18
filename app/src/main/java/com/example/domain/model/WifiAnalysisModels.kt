package com.example.domain.model

enum class WifiBand(val displayName: String) {
    BAND_2_4_GHZ("2.4 GHz"),
    BAND_5_GHZ("5 GHz"),
    BAND_6_GHZ("6 GHz")
}

enum class RssiClassification(val displayName: String) {
    EXCELLENT("Excellent"),
    VERY_GOOD("Very Good"),
    GOOD("Good"),
    FAIR("Fair"),
    WEAK("Weak")
}

data class SecurityAssessment(
    val protocol: String,
    val score: Int,
    val rating: String
)

data class ChannelCongestionAnalysis(
    val band: WifiBand,
    val accessPointCount: Int,
    val channelCounts: Map<Int, Int>,
    val bestChannel: Int?,
    val congestionScore: Int
)

data class BandDistribution(
    val totalAccessPoints: Int,
    val counts: Map<WifiBand, Int>
) {
    fun percentageFor(band: WifiBand): Int {
        if (totalAccessPoints == 0) return 0
        return ((counts[band] ?: 0) * 100f / totalAccessPoints).toInt()
    }
}

data class WifiAnalysis(
    val targetNetwork: WifiNetwork,
    val networkHealthScore: Int,
    val rssiClassification: RssiClassification,
    val securityAssessment: SecurityAssessment,
    val channelCongestion: List<ChannelCongestionAnalysis>,
    val bandDistribution: BandDistribution,
    val rssiContribution: Int = 0,
    val congestionContribution: Int = 0,
    val securityContribution: Int = 0,
    val frequencyContribution: Int = 0
)
