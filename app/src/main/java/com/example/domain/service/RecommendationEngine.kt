package com.example.domain.service

import com.example.domain.model.HandoffRecommendation
import com.example.domain.model.NetworkRecommendation
import com.example.domain.model.NetworkDecisionExplanation
import com.example.domain.model.NetworkMetrics
import com.example.domain.model.PredictionEvidence
import com.example.domain.model.AnalyzerEvidence
import com.example.domain.model.SpeedTestEvidence
import com.example.domain.model.SignalTrend
import com.example.domain.model.RecommendationCategory
import com.example.domain.model.RecommendationPriority
import com.example.domain.model.RecommendationSeverity
import com.example.domain.model.RoamingPrediction
import com.example.domain.model.SpeedTestResult
import com.example.domain.model.WifiAnalysis
import com.example.domain.model.WifiNetwork

class RecommendationEngine {
    fun generate(
        nearbyNetworks: List<WifiNetwork>,
        connectedNetwork: WifiNetwork?,
        analyzerResult: WifiAnalysis?,
        roamingPrediction: RoamingPrediction?,
        latestSpeedTest: SpeedTestResult?
    ): List<NetworkRecommendation> {
        if (connectedNetwork == null) return emptyList()
        val now = System.currentTimeMillis()
        val recommendations = mutableListOf<NetworkRecommendation>()

        if (connectedNetwork.rssi < -75) {
            recommendations += recommendation(
                "Weak Wi-Fi signal", "Your current signal is ${connectedNetwork.rssi} dBm. Move closer to the access point for a steadier connection.",
                RecommendationPriority.HIGH, RecommendationSeverity.WARNING, RecommendationCategory.CONNECTIVITY,
                signalConfidence(connectedNetwork.rssi), "Move closer to the access point", "Higher stability and fewer dropouts", now
            )
        }

        when (roamingPrediction?.recommendation) {
            HandoffRecommendation.PREPARE_ROAMING -> roamingPrediction.recommendedAccessPoint?.let { candidate ->
                recommendation(
                    "Prepare to roam", "A stronger access point is available on ${candidate.ssid} at ${candidate.rssi} dBm.",
                    RecommendationPriority.HIGH, RecommendationSeverity.WARNING, RecommendationCategory.ROAMING,
                    confidence(roamingPrediction), "Prepare to switch to ${candidate.ssid}", "Smoother coverage as signal changes", now
                ).also { recommendations += it }
            }
            HandoffRecommendation.ROAM_NOW -> roamingPrediction.recommendedAccessPoint?.let { candidate ->
                val gain = candidate.rssi - connectedNetwork.rssi
                recommendation(
                    "Roam now", "${candidate.ssid} can improve signal by approximately $gain dB compared with the current access point.",
                    RecommendationPriority.CRITICAL, RecommendationSeverity.CRITICAL, RecommendationCategory.ROAMING,
                    confidence(roamingPrediction), "Switch to ${candidate.ssid}", "Immediate signal improvement", now
                ).also { recommendations += it }
            }
            else -> Unit
        }

        if (connectedNetwork.securityType.equals("None", true) || connectedNetwork.securityType.contains("OPEN", true)) {
            recommendations += recommendation(
                "Open network detected", "This connection does not advertise encryption. Avoid transmitting sensitive information over it.",
                RecommendationPriority.CRITICAL, RecommendationSeverity.CRITICAL, RecommendationCategory.SECURITY,
                100, "Avoid sensitive activity on this network", "Reduced exposure of personal data", now
            )
        }

        val crowdedBand = analyzerResult?.channelCongestion?.firstOrNull { it.congestionScore < 50 }
        if (crowdedBand != null) {
            val channel = crowdedBand.bestChannel
            recommendations += recommendation(
                "Crowded ${crowdedBand.band.displayName} channel", "Nearby access points are concentrated on this band${channel?.let { "; channel $it is recommended" } ?: ""}.",
                RecommendationPriority.MEDIUM, RecommendationSeverity.WARNING, RecommendationCategory.CONGESTION,
                congestionConfidence(crowdedBand.congestionScore), channel?.let { "Prefer channel $it" } ?: "Review router channel settings", "Less interference for connected devices", now
            )
        }

        val fiveGhzAlternative = nearbyNetworks.firstOrNull { it.ssid == connectedNetwork.ssid && it.frequency in 4900..5924 && it.rssi >= -70 }
        if (!connectedNetwork.is5GHz && fiveGhzAlternative != null) {
            recommendations += recommendation(
                "5 GHz is available", "A usable 5 GHz access point with the same network name is nearby at ${fiveGhzAlternative.rssi} dBm.",
                RecommendationPriority.MEDIUM, RecommendationSeverity.INFO, RecommendationCategory.OPTIMIZATION,
                signalConfidence(fiveGhzAlternative.rssi), "Prefer the 5 GHz access point", "Potentially better performance and lower congestion", now
            )
        }

        val sixGhzAlternative = nearbyNetworks.firstOrNull { !it.isConnected && it.frequency >= 5925 && it.rssi >= -70 }
        if (sixGhzAlternative != null) {
            recommendations += recommendation(
                "6 GHz access point available", "${sixGhzAlternative.ssid} is visible on the 6 GHz band at ${sixGhzAlternative.rssi} dBm.",
                RecommendationPriority.LOW, RecommendationSeverity.INFO, RecommendationCategory.OPTIMIZATION,
                signalConfidence(sixGhzAlternative.rssi), "Prefer the 6 GHz access point when compatible", "Potentially more available spectrum and lower contention", now
            )
        }

        if (latestSpeedTest != null && (analyzerResult?.networkHealthScore ?: 0) >= 70 && latestSpeedTest.downloadMbps < 10) {
            recommendations += recommendation(
                "Internet congestion detected", "Wi-Fi health is good, but the measured download speed is ${format(latestSpeedTest.downloadMbps)} Mbps.",
                RecommendationPriority.MEDIUM, RecommendationSeverity.WARNING, RecommendationCategory.PERFORMANCE,
                speedConfidence(analyzerResult?.networkHealthScore, latestSpeedTest.downloadMbps), "Retest near the router or check ISP capacity", "Identify whether the bottleneck is upstream", now
            )
        }

        if (recommendations.isEmpty() && analyzerResult != null && latestSpeedTest != null && analyzerResult.networkHealthScore >= 80 && connectedNetwork.rssi >= -65) {
            recommendations += recommendation(
                "Excellent connection", "Your signal, analyzed network health, and measured internet performance are all in a healthy range.",
                RecommendationPriority.LOW, RecommendationSeverity.POSITIVE, RecommendationCategory.OPTIMIZATION,
                healthyConfidence(connectedNetwork.rssi, analyzerResult.networkHealthScore, latestSpeedTest.downloadMbps), "Stay connected", "No optimization required", now
            )
        }

        return recommendations.sortedBy { it.priority.ordinal }.map { recommendation ->
            recommendation.copy(
                explanation = explain(
                    recommendation,
                    connectedNetwork,
                    nearbyNetworks,
                    analyzerResult,
                    roamingPrediction,
                    latestSpeedTest
                )
            )
        }
    }

    private fun explain(
        recommendation: NetworkRecommendation,
        current: WifiNetwork,
        nearby: List<WifiNetwork>,
        analysis: WifiAnalysis?,
        prediction: RoamingPrediction?,
        speedTest: SpeedTestResult?
    ): NetworkDecisionExplanation {
        val candidate = prediction?.recommendedAccessPoint
            ?: nearby.firstOrNull { it.bssid != current.bssid && it.ssid == current.ssid && it.rssi > current.rssi }
        val predictionEvidence = prediction?.let {
            PredictionEvidence(
                currentRssi = it.currentRssi,
                trend = it.trend,
                historicalSampleCount = it.rssiSampleCount,
                score = it.predictionScore,
                likelihood = it.likelihood,
                recommendation = it.recommendation,
                candidateRssiImprovement = it.recommendedAccessPoint?.let { ap -> ap.rssi - current.rssi }
            )
        }
        val crowded = analysis?.channelCongestion?.firstOrNull { it.congestionScore < 50 }
        val analyzerEvidence = analysis?.let {
            AnalyzerEvidence(
                healthScore = it.networkHealthScore,
                rssiContribution = it.rssiContribution,
                congestionContribution = it.congestionContribution,
                securityContribution = it.securityContribution,
                frequencyContribution = it.frequencyContribution,
                observedAccessPointCount = it.bandDistribution.totalAccessPoints,
                crowdedBand = crowded?.band?.displayName,
                crowdedBandAccessPointCount = crowded?.accessPointCount,
                recommendedChannel = crowded?.bestChannel,
                recommendedChannelObservedAccessPoints = crowded?.bestChannel?.let { channel -> crowded.channelCounts[channel] ?: 0 }
            )
        }
        val speedEvidence = speedTest?.let {
            SpeedTestEvidence(it.downloadMbps, it.uploadMbps, it.pingMs, it.jitterMs, it.timestamp)
        }
        val supporting = buildList {
            add("Current RSSI: ${current.rssi} dBm")
            add("Current band: ${band(current.frequency)}")
            prediction?.let { add("RSSI trend: ${it.trend.displayName()}; ${it.rssiSampleCount} historical samples") }
            candidate?.let { add("Candidate: ${it.ssid} at ${it.rssi} dBm (${band(it.frequency)})") }
            analysis?.let { add("Analyzer health: ${it.networkHealthScore}/100") }
            speedTest?.let { add("Measured download: ${format(it.downloadMbps)} Mbps; ping: ${format(it.pingMs)} ms") }
        }
        val reason = when {
            recommendation.category == RecommendationCategory.SECURITY -> "Current security is ${current.securityType}."
            recommendation.category == RecommendationCategory.ROAMING -> prediction?.let { "The connected AP is ${it.trend.displayName().lowercase()} with a ${it.predictionScore}/100 roaming score." } ?: "A stronger observed candidate is available."
            recommendation.category == RecommendationCategory.CONGESTION -> crowded?.let { "${it.accessPointCount} APs were observed on the ${it.band.displayName} band." } ?: "Observed channel data indicates congestion."
            recommendation.category == RecommendationCategory.PERFORMANCE -> speedTest?.let { "Measured download is ${format(it.downloadMbps)} Mbps despite analyzer health of ${analysis?.networkHealthScore ?: 0}/100." } ?: "Measured network performance is below the configured rule threshold."
            recommendation.title.contains("5 GHz") -> "A suitable 5 GHz AP was observed at ${candidate?.rssi} dBm."
            recommendation.title.contains("6 GHz") -> "A 6 GHz AP was observed at ${candidate?.rssi} dBm."
            else -> "Current RSSI is ${current.rssi} dBm."
        }
        return NetworkDecisionExplanation(
            decision = recommendation.title,
            primaryReason = reason,
            supportingFactors = supporting,
            currentNetwork = metrics(current),
            candidateNetwork = candidate?.let(::metrics),
            prediction = predictionEvidence,
            analyzer = analyzerEvidence,
            speedTest = speedEvidence,
            expectedBenefit = recommendation.expectedBenefit,
            confidence = recommendation.confidence,
            timestamp = recommendation.timestamp
        )
    }

    private fun metrics(network: WifiNetwork) = NetworkMetrics(network.ssid, network.bssid, network.rssi, network.frequency, network.channel, band(network.frequency), network.securityType)
    private fun band(frequency: Int) = when { frequency >= 5925 -> "6 GHz"; frequency >= 4900 -> "5 GHz"; else -> "2.4 GHz" }
    private fun SignalTrend.displayName() = when (this) { SignalTrend.IMPROVING -> "Improving"; SignalTrend.STABLE -> "Stable"; SignalTrend.DEGRADING -> "Degrading" }

    private fun recommendation(title: String, description: String, priority: RecommendationPriority, severity: RecommendationSeverity, category: RecommendationCategory, confidence: Int, action: String, benefit: String, timestamp: Long) = NetworkRecommendation("${category.name}:${title.lowercase().replace(' ', '_')}", title, description, priority, severity, category, confidence.coerceIn(0, 100), timestamp, action, benefit)
    private fun confidence(prediction: RoamingPrediction) = prediction.predictionScore.coerceIn(0, 100)
    private fun signalConfidence(rssi: Int) = ((rssi + 100) * 2).coerceIn(0, 100)
    private fun congestionConfidence(congestionScore: Int) = (100 - congestionScore).coerceIn(0, 100)
    private fun speedConfidence(healthScore: Int?, downloadMbps: Double) = (((healthScore ?: 0) + ((10.0 - downloadMbps).coerceIn(0.0, 10.0) * 10.0)) / 2.0).toInt().coerceIn(0, 100)
    private fun healthyConfidence(rssi: Int, healthScore: Int, downloadMbps: Double) = ((signalConfidence(rssi) + healthScore + (downloadMbps.coerceIn(0.0, 100.0)).toInt()) / 3).coerceIn(0, 100)
    private fun format(value: Double) = if (value >= 100) "%.0f".format(value) else "%.1f".format(value)
}
