package com.example.domain.service

import com.example.domain.model.BandDistribution
import com.example.domain.model.ChannelCongestionAnalysis
import com.example.domain.model.RssiClassification
import com.example.domain.model.SecurityAssessment
import com.example.domain.model.WifiAnalysis
import com.example.domain.model.WifiBand
import com.example.domain.model.WifiNetwork
import kotlin.math.roundToInt

class AnalyzerService {

    fun analyze(networks: List<WifiNetwork>): WifiAnalysis? {
        if (networks.isEmpty()) return null

        val targetNetwork = networks.firstOrNull { it.isConnected }
            ?: networks.maxBy { it.rssi }
        val targetBand = bandForFrequency(targetNetwork.frequency)
        val channelCongestion = WifiBand.entries.map { band ->
            analyzeBand(networks, band)
        }
        val securityAssessment = assessSecurity(targetNetwork.securityType)
        val rssiScore = signalPercentage(targetNetwork.rssi)
        val frequencyScore = when (targetBand) {
            WifiBand.BAND_2_4_GHZ -> 60
            WifiBand.BAND_5_GHZ -> 85
            WifiBand.BAND_6_GHZ -> 100
        }
        val sameChannelNeighbors = networks.count { network ->
            network.bssid != targetNetwork.bssid &&
                bandForFrequency(network.frequency) == targetBand &&
                network.channel == targetNetwork.channel
        }
        val channelScore = (100 - sameChannelNeighbors * 20).coerceIn(0, 100)
        val healthScore = (
            rssiScore * RSSI_WEIGHT +
                channelScore * CONGESTION_WEIGHT +
                securityAssessment.score * SECURITY_WEIGHT +
                frequencyScore * BAND_WEIGHT
            ).toInt().coerceIn(0, 100)
        val rssiContribution = (rssiScore * RSSI_WEIGHT).roundToInt()
        val congestionContribution = (channelScore * CONGESTION_WEIGHT).roundToInt()
        val securityContribution = (securityAssessment.score * SECURITY_WEIGHT).roundToInt()
        val frequencyContribution = (frequencyScore * BAND_WEIGHT).roundToInt()

        return WifiAnalysis(
            targetNetwork = targetNetwork,
            networkHealthScore = healthScore,
            rssiClassification = classifyRssi(targetNetwork.rssi),
            securityAssessment = securityAssessment,
            channelCongestion = channelCongestion,
            bandDistribution = createBandDistribution(networks),
            rssiContribution = rssiContribution,
            congestionContribution = congestionContribution,
            securityContribution = securityContribution,
            frequencyContribution = frequencyContribution
        )
    }

    fun classifyRssi(rssi: Int): RssiClassification {
        return when {
            rssi >= -50 -> RssiClassification.EXCELLENT
            rssi >= -60 -> RssiClassification.VERY_GOOD
            rssi >= -70 -> RssiClassification.GOOD
            rssi >= -80 -> RssiClassification.FAIR
            else -> RssiClassification.WEAK
        }
    }

    fun assessSecurity(securityType: String): SecurityAssessment {
        val normalized = securityType.uppercase()
        return when {
            normalized == "NONE" || "OPEN" in normalized ->
                SecurityAssessment("Open", 0, "Critical")
            "WEP" in normalized ->
                SecurityAssessment("WEP", 10, "Very Weak")
            "WPA3" in normalized || "SAE" in normalized ->
                SecurityAssessment("WPA3", 100, "Excellent")
            "WPA2" in normalized ->
                SecurityAssessment("WPA2", 80, "Strong")
            "WPA" in normalized ->
                SecurityAssessment("WPA", 50, "Moderate")
            else ->
                SecurityAssessment("Unknown", 30, "Unverified")
        }
    }

    private fun analyzeBand(
        networks: List<WifiNetwork>,
        band: WifiBand
    ): ChannelCongestionAnalysis {
        val bandNetworks = networks.filter { bandForFrequency(it.frequency) == band }
        val channelCounts = bandNetworks
            .filter { it.channel > 0 }
            .groupingBy { it.channel }
            .eachCount()
            .toSortedMap()
        val bestChannel = if (bandNetworks.isEmpty()) {
            null
        } else {
            candidateChannels.getValue(band)
                .minWithOrNull(compareBy<Int> { channelCounts[it] ?: 0 }.thenBy { it })
        }
        val busiestChannelCount = channelCounts.values.maxOrNull() ?: 0
        val congestionScore = (100 - busiestChannelCount * 15).coerceIn(0, 100)

        return ChannelCongestionAnalysis(
            band = band,
            accessPointCount = bandNetworks.size,
            channelCounts = channelCounts,
            bestChannel = bestChannel,
            congestionScore = congestionScore
        )
    }

    private fun createBandDistribution(networks: List<WifiNetwork>): BandDistribution {
        val counts = WifiBand.entries.associateWith { band ->
            networks.count { bandForFrequency(it.frequency) == band }
        }
        return BandDistribution(
            totalAccessPoints = networks.size,
            counts = counts
        )
    }

    private fun bandForFrequency(frequency: Int): WifiBand {
        return when {
            frequency >= 5925 -> WifiBand.BAND_6_GHZ
            frequency >= 4900 -> WifiBand.BAND_5_GHZ
            else -> WifiBand.BAND_2_4_GHZ
        }
    }

    private fun signalPercentage(rssi: Int): Int {
        return (2 * (rssi + 100)).coerceIn(0, 100)
    }

    private companion object {
        const val RSSI_WEIGHT = 0.40
        const val CONGESTION_WEIGHT = 0.25
        const val SECURITY_WEIGHT = 0.25
        const val BAND_WEIGHT = 0.10

        val candidateChannels = mapOf(
            WifiBand.BAND_2_4_GHZ to listOf(1, 6, 11),
            WifiBand.BAND_5_GHZ to listOf(36, 40, 44, 48, 149, 153, 157, 161, 165),
            WifiBand.BAND_6_GHZ to (5..229 step 16).toList()
        )
    }
}
