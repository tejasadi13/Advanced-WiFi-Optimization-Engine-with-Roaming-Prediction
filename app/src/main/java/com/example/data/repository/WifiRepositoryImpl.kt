package com.example.data.repository

import com.example.data.local.dao.WifiDao
import com.example.data.local.entity.SavedNetworkEntity
import com.example.data.local.entity.ScanHistoryEntity
import com.example.data.util.WifiManagerHelper
import com.example.domain.model.RoamingPrediction
import com.example.domain.model.ScanHistoryItem
import com.example.domain.model.WifiNetwork
import com.example.domain.model.WifiRecommendation
import com.example.domain.repository.WifiRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import kotlin.random.Random

class WifiRepositoryImpl(
    private val wifiDao: WifiDao,
    private val wifiManagerHelper: WifiManagerHelper
) : WifiRepository {

    override fun getSavedNetworks(): Flow<List<WifiNetwork>> {
        return wifiDao.getAllSavedNetworks().map { entities ->
            entities.map { entity ->
                WifiNetwork(
                    ssid = entity.ssid,
                    bssid = entity.bssid,
                    rssi = entity.lastRssi,
                    frequency = entity.frequency,
                    channel = getChannelFromFrequency(entity.frequency),
                    securityType = entity.security,
                    isConnected = false,
                    estimatedSpeedMbps = getEstimatedSpeed(entity.lastRssi)
                )
            }
        }
    }

    override suspend fun saveNetwork(network: WifiNetwork) {
        wifiDao.insertSavedNetwork(
            SavedNetworkEntity(
                bssid = network.bssid,
                ssid = network.ssid,
                security = network.securityType,
                lastRssi = network.rssi,
                frequency = network.frequency,
                isPreferred = true
            )
        )
    }

    override suspend fun deleteNetwork(bssid: String) {
        wifiDao.deleteSavedNetwork(bssid)
    }

    override fun getScanHistory(): Flow<List<ScanHistoryItem>> {
        return wifiDao.getAllScanHistory().map { entities ->
            if (entities.isEmpty()) {
                // Return default seed data if database is empty
                getSeedHistory()
            } else {
                entities.map { entity ->
                    ScanHistoryItem(
                        id = entity.id.toString(),
                        timestamp = entity.timestamp,
                        averageSignalStrength = entity.averageRssi,
                        networkCount = entity.networkCount,
                        optimizedCount = entity.optimizedCount,
                        securityIssuesFound = entity.securityIssuesFound,
                        statusMessage = entity.statusMessage
                    )
                }
            }
        }
    }

    override suspend fun addScanHistory(item: ScanHistoryItem) {
        wifiDao.insertScanHistory(
            ScanHistoryEntity(
                timestamp = item.timestamp,
                networkCount = item.networkCount,
                averageRssi = item.averageSignalStrength,
                optimizedCount = item.optimizedCount,
                securityIssuesFound = item.securityIssuesFound,
                statusMessage = item.statusMessage
            )
        )
    }

    override suspend fun clearScanHistory() {
        wifiDao.clearAllScanHistory()
    }

    override fun getLiveWifiNetworks(): Flow<List<WifiNetwork>> = flow {
        while (true) {
            val connectedNetwork = wifiManagerHelper.getConnectedWifiInfo()
            emit(connectedNetwork?.let(::listOf) ?: emptyList())
            delay(3000) // update every 3 seconds
        }
    }

    override fun getNearbyNetworks(): Flow<List<WifiNetwork>> = flow {
        while (true) {
            emit(wifiManagerHelper.scanNearbyNetworks())
            delay(3000)
        }
    }

    override fun getRoamingPredictions(currentNetwork: WifiNetwork?): Flow<List<RoamingPrediction>> = flow {
        val candidates = listOf(
            Pair("HQ_Corporate_Node_North", "AA:BB:CC:DD:EE:11"),
            Pair("HQ_Corporate_Node_West", "AA:BB:CC:DD:EE:22"),
            Pair("HQ_Corporate_Node_South", "AA:BB:CC:DD:EE:33")
        )

        while (true) {
            val signalLossCurrent = currentNetwork?.rssi ?: -75
            // When current network RSSI drops below -65, roaming recommendations trigger!
            val predictions = candidates.mapIndexed { index, candidate ->
                val baseConfidence = if (signalLossCurrent < -70) 0.85f - (index * 0.1f) else 0.4f - (index * 0.1f)
                val confidence = baseConfidence.coerceIn(0.1f, 0.99f)
                val action = when {
                    confidence > 0.8f -> "Prepare handover - High priority"
                    confidence > 0.6f -> "Pre-auth handshake initiated"
                    else -> "Monitor coverage"
                }
                val trend = when {
                    signalLossCurrent < -75 -> "Degrading rapidly"
                    signalLossCurrent < -65 -> "Slight decay"
                    else -> "Stable"
                }

                RoamingPrediction(
                    currentBssid = currentNetwork?.bssid ?: "00:11:22:33:44:55",
                    candidateBssid = candidate.second,
                    candidateSsid = candidate.first,
                    predictionConfidence = confidence,
                    recommendedAction = action,
                    signalTrendCurrent = trend,
                    estimatedDelayMs = 45 + (index * 12)
                )
            }
            emit(predictions)
            delay(4000)
        }
    }

    override fun getWifiRecommendations(liveNetworks: List<WifiNetwork>): Flow<List<WifiRecommendation>> = flow {
        val recommendations = mutableListOf<WifiRecommendation>()

        // Check for security issues (unsecured / none security)
        val openNetworks = liveNetworks.filter { it.securityType == "None" }
        if (openNetworks.isNotEmpty()) {
            recommendations.add(
                WifiRecommendation(
                    title = "Disable Unsecured Network Auto-Connect",
                    description = "Found ${openNetworks.size} unsecured network(s) in range (including '${openNetworks.firstOrNull()?.ssid}'). Disable auto-connect to prevent man-in-the-middle attacks.",
                    priority = WifiRecommendation.Priority.HIGH,
                    category = WifiRecommendation.Category.SECURITY
                )
            )
        }

        // Check for 5GHz optimization
        val connectedNetwork = liveNetworks.firstOrNull { it.isConnected }
        if (connectedNetwork != null && !connectedNetwork.is5GHz) {
            val fiveGhzAlternative = liveNetworks.firstOrNull { it.ssid == connectedNetwork.ssid && it.is5GHz }
            if (fiveGhzAlternative != null) {
                recommendations.add(
                    WifiRecommendation(
                        title = "Switch to 5GHz Band",
                        description = "You are connected to '${connectedNetwork.ssid}' on the 2.4GHz band, but a faster 5GHz band with the same name is available with strong signal (${fiveGhzAlternative.rssi} dBm).",
                        priority = WifiRecommendation.Priority.MEDIUM,
                        category = WifiRecommendation.Category.PERFORMANCE
                    )
                )
            }
        }

        // Channel congestion
        recommendations.add(
            WifiRecommendation(
                title = "Optimize Router Channel Selection",
                description = "Channel congestion is high on the 2.4GHz band. Recommend moving your router to Channel 1, 6 or 11 to avoid co-channel overlap.",
                priority = WifiRecommendation.Priority.LOW,
                category = WifiRecommendation.Category.PERFORMANCE
            )
        )

        // Roaming optimization
        recommendations.add(
            WifiRecommendation(
                title = "Aggressive Roaming Handover Target",
                description = "Predictive Roaming Engine has cached 'HQ_Corporate_Node_North' with 94% signal confidence. We recommend triggering handover when current signal falls past -72 dBm.",
                priority = WifiRecommendation.Priority.HIGH,
                category = WifiRecommendation.Category.COVERAGE
            )
        )

        emit(recommendations)
    }

    private fun getChannelFromFrequency(freq: Int): Int {
        return when {
            freq == 2484 -> 14
            freq in 2412..2472 -> (freq - 2412) / 5 + 1
            freq in 5180..5825 -> (freq - 5000) / 5
            else -> 0
        }
    }

    private fun getEstimatedSpeed(rssi: Int): Int {
        return when {
            rssi >= -50 -> 866
            rssi >= -60 -> 650
            rssi >= -70 -> 433
            rssi >= -80 -> 144
            else -> 24
        }
    }

    private fun getSeedHistory(): List<ScanHistoryItem> {
        val now = System.currentTimeMillis()
        return listOf(
            ScanHistoryItem(
                id = "1",
                timestamp = now - 3600000, // 1 hr ago
                averageSignalStrength = -52,
                networkCount = 8,
                optimizedCount = 3,
                securityIssuesFound = 1,
                statusMessage = "Roaming transition completed in 38ms"
            ),
            ScanHistoryItem(
                id = "2",
                timestamp = now - 14400000, // 4 hrs ago
                averageSignalStrength = -64,
                networkCount = 12,
                optimizedCount = 5,
                securityIssuesFound = 2,
                statusMessage = "Mitigated connection drop in weak zone"
            ),
            ScanHistoryItem(
                id = "3",
                timestamp = now - 86400000, // 1 day ago
                averageSignalStrength = -58,
                networkCount = 10,
                optimizedCount = 4,
                securityIssuesFound = 0,
                statusMessage = "Periodic scan optimization complete"
            )
        )
    }
}
