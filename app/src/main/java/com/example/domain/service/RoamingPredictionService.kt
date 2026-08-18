package com.example.domain.service

import com.example.domain.model.HandoffRecommendation
import com.example.domain.model.RoamingLikelihood
import com.example.domain.model.RoamingPrediction
import com.example.domain.model.SignalTrend
import com.example.domain.model.WifiNetwork
import kotlin.math.roundToInt

/**
 * Computes a roaming decision from real connected and scan-result observations only.
 * Samples are intentionally in-memory: they describe the active session and do not alter Room.
 */
class RoamingPredictionService {
    private var activeNetworkKey: String? = null
    private val rssiSamples = ArrayDeque<Int>()

    fun predict(currentNetwork: WifiNetwork?, nearbyNetworks: List<WifiNetwork>): RoamingPrediction? {
        if (
            currentNetwork == null ||
            currentNetwork.bssid.isBlank() ||
            currentNetwork.bssid == REDACTED_BSSID ||
            nearbyNetworks.isEmpty()
        ) {
            clearHistory()
            return null
        }

        recordSample(currentNetwork)
        if (rssiSamples.size < MINIMUM_SAMPLES) return null

        val trend = signalTrend()
        val candidate = selectBestCandidate(currentNetwork, nearbyNetworks)
        val candidateAdvantage = candidate?.let { it.rssi - currentNetwork.rssi } ?: 0
        val score = predictionScore(currentNetwork.rssi, trend, candidateAdvantage)
        val viableCandidate = candidate?.takeIf { it.rssi >= currentNetwork.rssi + MINIMUM_CANDIDATE_ADVANTAGE }
        val recommendation = when {
            score >= ROAM_NOW_THRESHOLD && viableCandidate != null -> HandoffRecommendation.ROAM_NOW
            score >= PREPARE_THRESHOLD && viableCandidate != null -> HandoffRecommendation.PREPARE_ROAMING
            else -> HandoffRecommendation.STAY_CONNECTED
        }

        return RoamingPrediction(
            currentRssi = currentNetwork.rssi,
            trend = trend,
            predictionScore = score,
            likelihood = when {
                score >= ROAM_NOW_THRESHOLD -> RoamingLikelihood.HIGH
                score >= PREPARE_THRESHOLD -> RoamingLikelihood.MEDIUM
                else -> RoamingLikelihood.LOW
            },
            recommendedAccessPoint = viableCandidate,
            recommendation = recommendation,
            timestamp = System.currentTimeMillis(),
            rssiSampleCount = rssiSamples.size
        )
    }

    private fun recordSample(network: WifiNetwork) {
        val networkKey = network.bssid.takeUnless { it.isBlank() || it == REDACTED_BSSID } ?: network.ssid
        if (activeNetworkKey != networkKey) {
            activeNetworkKey = networkKey
            rssiSamples.clear()
        }
        rssiSamples.addLast(network.rssi)
        while (rssiSamples.size > MAX_SAMPLES) rssiSamples.removeFirst()
    }

    private fun clearHistory() {
        activeNetworkKey = null
        rssiSamples.clear()
    }

    private fun signalTrend(): SignalTrend {
        val midpoint = rssiSamples.size / 2
        val earlierAverage = rssiSamples.take(midpoint).average()
        val recentAverage = rssiSamples.drop(midpoint).average()
        val delta = recentAverage - earlierAverage
        return when {
            delta >= TREND_DELTA_DBM -> SignalTrend.IMPROVING
            delta <= -TREND_DELTA_DBM -> SignalTrend.DEGRADING
            else -> SignalTrend.STABLE
        }
    }

    private fun selectBestCandidate(current: WifiNetwork, networks: List<WifiNetwork>): WifiNetwork? {
        return networks
            .asSequence()
            .filter { it.bssid.isNotBlank() && !it.bssid.equals(current.bssid, ignoreCase = true) }
            .filter { securityCompatible(current.securityType, it.securityType) }
            .maxByOrNull { candidate ->
                signalQuality(candidate.rssi) +
                    (if (candidate.ssid == current.ssid) SAME_SSID_BONUS else 0) +
                    securityScore(candidate.securityType) +
                    bandBonus(candidate.frequency) -
                    channelCongestionPenalty(candidate, networks)
            }
    }

    private fun predictionScore(currentRssi: Int, trend: SignalTrend, candidateAdvantage: Int): Int {
        val weakSignal = ((-currentRssi - 55) * 1.2).roundToInt().coerceIn(0, 30)
        val trendRisk = when (trend) {
            SignalTrend.DEGRADING -> 35
            SignalTrend.STABLE -> 10
            SignalTrend.IMPROVING -> 0
        }
        val betterAccessPoint = (candidateAdvantage * 2).coerceIn(0, 25)
        val strongerApPresent = if (candidateAdvantage >= MINIMUM_CANDIDATE_ADVANTAGE) 10 else 0
        return (weakSignal + trendRisk + betterAccessPoint + strongerApPresent).coerceIn(0, 100)
    }

    private fun signalQuality(rssi: Int) = (rssi + 100).coerceIn(0, 60)

    private fun securityScore(securityType: String): Int = when {
        "WPA3" in securityType.uppercase() || "SAE" in securityType.uppercase() -> 12
        "WPA2" in securityType.uppercase() -> 9
        "WPA" in securityType.uppercase() -> 5
        else -> 0
    }

    private fun securityCompatible(current: String, candidate: String): Boolean {
        val currentRank = securityScore(current)
        val candidateRank = securityScore(candidate)
        // A secured connection must not be scored toward an open or weaker AP.
        return currentRank == 0 || candidateRank >= currentRank
    }

    private fun bandBonus(frequency: Int) = when {
        frequency >= 5925 -> 10
        frequency >= 4900 -> 6
        else -> 0
    }

    private fun channelCongestionPenalty(candidate: WifiNetwork, networks: List<WifiNetwork>): Int {
        return networks.count { it.channel == candidate.channel && sameBand(it.frequency, candidate.frequency) && it.bssid != candidate.bssid } * 2
    }

    private fun sameBand(first: Int, second: Int): Boolean = band(first) == band(second)

    private fun band(frequency: Int) = when {
        frequency >= 5925 -> 6
        frequency >= 4900 -> 5
        else -> 2
    }

    private companion object {
        const val MAX_SAMPLES = 20
        const val MINIMUM_SAMPLES = 2
        const val TREND_DELTA_DBM = 3.0
        const val MINIMUM_CANDIDATE_ADVANTAGE = 5
        const val PREPARE_THRESHOLD = 40
        const val ROAM_NOW_THRESHOLD = 70
        const val SAME_SSID_BONUS = 20
        const val REDACTED_BSSID = "02:00:00:00:00:00"
    }
}
