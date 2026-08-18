package com.example.domain.service

import com.example.domain.model.HandoffRecommendation
import com.example.domain.model.NetworkJourneyEvent
import com.example.domain.model.NetworkJourneyEventType
import com.example.domain.model.NetworkRecommendation
import com.example.domain.model.RoamingPrediction
import com.example.domain.model.SpeedTestResult
import com.example.domain.model.WifiAnalysis
import com.example.domain.model.WifiNetwork

/** Turns real state transitions into a compact, de-duplicated local timeline. */
class NetworkJourneyService {
    private var previousNetwork: WifiNetwork? = null
    private var previousHealth: Int? = null
    private var previousPrediction: HandoffRecommendation? = null
    private var previousCandidateBssid: String? = null
    private var latestSpeedTimestamp: Long? = null
    private var latestRecommendationIds: Set<String> = emptySet()

    fun observe(network: WifiNetwork?, analysis: WifiAnalysis?, prediction: RoamingPrediction?, recommendations: List<NetworkRecommendation>, latestSpeedTest: SpeedTestResult?, timestamp: Long): List<NetworkJourneyEvent> {
        val events = mutableListOf<NetworkJourneyEvent>()
        val previous = previousNetwork
        if (previous == null && network != null) events += event(NetworkJourneyEventType.CONNECTED, network, analysis, prediction, "Connected", "Connected to ${network.ssid}", timestamp)
        if (previous != null && network == null) events += event(NetworkJourneyEventType.DISCONNECTED, previous, analysis, prediction, "Disconnected", "Connection to ${previous.ssid} ended", timestamp)
        if (previous != null && network != null && previous.bssid == network.bssid) {
            val delta = network.rssi - previous.rssi
            if (delta >= 5) events += event(NetworkJourneyEventType.SIGNAL_IMPROVED, network, analysis, prediction, "Signal improved", "Observed signal improved by $delta dBm", timestamp)
            if (delta <= -5) events += event(NetworkJourneyEventType.SIGNAL_DEGRADED, network, analysis, prediction, "Signal degraded", "Observed signal decreased by ${-delta} dBm", timestamp)
        }
        if (network != null && analysis != null && previousHealth != null && kotlin.math.abs(analysis.networkHealthScore - previousHealth!!) >= 10) events += event(NetworkJourneyEventType.HEALTH_CHANGED, network, analysis, prediction, "Network health changed", "Health score is now ${analysis.networkHealthScore}/100", timestamp)
        val candidateChanged = prediction?.recommendedAccessPoint?.bssid != previousCandidateBssid
        if (network != null && prediction != null && (prediction.recommendation != previousPrediction || candidateChanged)) {
            when (prediction.recommendation) {
                HandoffRecommendation.PREPARE_ROAMING -> events += event(NetworkJourneyEventType.PREPARE_ROAMING, network, analysis, prediction, "Prepare roaming", "A stronger observed candidate is available", timestamp)
                HandoffRecommendation.ROAM_NOW -> events += event(NetworkJourneyEventType.ROAM_NOW, network, analysis, prediction, "Roaming recommended", "Observed conditions support an advisory roaming recommendation", timestamp)
                else -> Unit
            }
            prediction.recommendedAccessPoint?.let { candidate -> events += event(NetworkJourneyEventType.CANDIDATE_DETECTED, network, analysis, prediction, "Candidate access point detected", "${candidate.ssid} observed at ${candidate.rssi} dBm", timestamp) }
        }
        recommendations.filter { it.id !in latestRecommendationIds }.forEach { rec ->
            events += event(NetworkJourneyEventType.RECOMMENDATION_GENERATED, network, analysis, prediction, rec.title, rec.description, timestamp)
        }
        if (latestSpeedTest != null && latestSpeedTest.timestamp != latestSpeedTimestamp) events += event(NetworkJourneyEventType.SPEED_TEST_COMPLETED, network, analysis, prediction, "Speed test completed", "${format(latestSpeedTest.downloadMbps)} Mbps down · ${format(latestSpeedTest.uploadMbps)} Mbps up · ${format(latestSpeedTest.pingMs)} ms ping", latestSpeedTest.timestamp)
        previousNetwork = network
        previousHealth = analysis?.networkHealthScore
        previousPrediction = prediction?.recommendation
        previousCandidateBssid = prediction?.recommendedAccessPoint?.bssid
        latestSpeedTimestamp = latestSpeedTest?.timestamp
        latestRecommendationIds = recommendations.map { it.id }.toSet()
        return events
    }

    private fun event(type: NetworkJourneyEventType, network: WifiNetwork?, analysis: WifiAnalysis?, prediction: RoamingPrediction?, title: String, detail: String, timestamp: Long) = NetworkJourneyEvent(type = type, timestamp = timestamp, ssid = network?.ssid, bssid = network?.bssid, rssi = network?.rssi, band = network?.let { if (it.frequency >= 5925) "6 GHz" else if (it.frequency >= 4900) "5 GHz" else "2.4 GHz" }, healthScore = analysis?.networkHealthScore, predictionState = prediction?.recommendation?.name, candidateSsid = prediction?.recommendedAccessPoint?.ssid, candidateRssi = prediction?.recommendedAccessPoint?.rssi, title = title, detail = detail)
    private fun format(value: Double) = if (value >= 100) "%.0f".format(value) else "%.1f".format(value)
}
