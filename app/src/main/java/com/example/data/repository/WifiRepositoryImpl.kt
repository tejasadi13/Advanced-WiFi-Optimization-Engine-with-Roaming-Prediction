package com.example.data.repository

import com.example.data.local.dao.WifiDao
import com.example.data.local.entity.SavedNetworkEntity
import com.example.data.local.entity.ScanHistoryEntity
import com.example.data.local.entity.SpeedTestHistoryEntity
import com.example.data.local.entity.NetworkJourneyEventEntity
import com.example.data.local.entity.HeatmapObservationEntity
import com.example.data.util.WifiManagerHelper
import com.example.domain.model.RoamingPrediction
import com.example.domain.model.ScanHistoryItem
import com.example.domain.model.WifiNetwork
import com.example.domain.model.WifiRecommendation
import com.example.domain.model.WifiAnalysis
import com.example.domain.model.SpeedTestResult
import com.example.domain.model.NetworkRecommendation
import com.example.domain.repository.WifiRepository
import com.example.domain.service.AnalyzerService
import com.example.domain.service.RoamingPredictionService
import com.example.domain.service.RecommendationEngine
import com.example.domain.service.HeatmapService
import com.example.worker.BackgroundObservationScheduler
import com.example.domain.model.NetworkJourneyEvent
import com.example.domain.model.HeatmapObservation
import com.example.domain.model.HeatmapSummary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.util.UUID

class WifiRepositoryImpl(
    private val wifiDao: WifiDao,
    private val wifiManagerHelper: WifiManagerHelper,
    private val analyzerService: AnalyzerService,
    private val roamingPredictionService: RoamingPredictionService,
    private val recommendationEngine: RecommendationEngine,
    private val heatmapService: HeatmapService,
    private val backgroundObservationScheduler: BackgroundObservationScheduler
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

    override fun analyzeNetworks(networks: List<WifiNetwork>): WifiAnalysis? {
        return analyzerService.analyze(networks)
    }

    override fun getRoamingPrediction(
        currentNetwork: WifiNetwork?,
        nearbyNetworks: List<WifiNetwork>
    ): Flow<RoamingPrediction?> = flow {
        emit(roamingPredictionService.predict(currentNetwork, nearbyNetworks))
    }

    override fun getNetworkRecommendations(
        nearbyNetworks: List<WifiNetwork>,
        connectedNetwork: WifiNetwork?,
        analyzerResult: WifiAnalysis?,
        roamingPrediction: RoamingPrediction?,
        latestSpeedTest: SpeedTestResult?
    ): Flow<List<NetworkRecommendation>> = flow {
        emit(recommendationEngine.generate(nearbyNetworks, connectedNetwork, analyzerResult, roamingPrediction, latestSpeedTest))
    }

    override fun getSpeedTestHistory(): Flow<List<SpeedTestResult>> {
        return wifiDao.getSpeedTestHistory().map { entries ->
            entries.map { entry ->
                SpeedTestResult(
                    networkName = entry.networkName,
                    downloadMbps = entry.downloadMbps,
                    uploadMbps = entry.uploadMbps,
                    pingMs = entry.pingMs,
                    jitterMs = entry.jitterMs,
                    durationMs = entry.durationMs,
                    timestamp = entry.timestamp
                )
            }
        }
    }

    override suspend fun saveSpeedTestResult(result: SpeedTestResult) {
        wifiDao.insertSpeedTest(
            SpeedTestHistoryEntity(
                networkName = result.networkName,
                downloadMbps = result.downloadMbps,
                uploadMbps = result.uploadMbps,
                pingMs = result.pingMs,
                jitterMs = result.jitterMs,
                durationMs = result.durationMs,
                timestamp = result.timestamp
            )
        )
    }

    override fun getNetworkJourney(): Flow<List<NetworkJourneyEvent>> = wifiDao.getNetworkJourneyEvents().map { entries ->
        entries.map { NetworkJourneyEvent(it.id, com.example.domain.model.NetworkJourneyEventType.valueOf(it.type), it.timestamp, it.ssid, it.bssid, it.rssi, it.band, it.healthScore, it.predictionState, it.candidateSsid, it.candidateRssi, it.title, it.detail) }
    }

    override suspend fun recordNetworkJourney(events: List<NetworkJourneyEvent>) {
        events.forEach { event -> wifiDao.insertNetworkJourneyEvent(NetworkJourneyEventEntity(type = event.type.name, timestamp = event.timestamp, ssid = event.ssid, bssid = event.bssid, rssi = event.rssi, band = event.band, healthScore = event.healthScore, predictionState = event.predictionState, candidateSsid = event.candidateSsid, candidateRssi = event.candidateRssi, title = event.title, detail = event.detail)) }
    }

    override fun getHeatmapObservations(): Flow<List<HeatmapObservation>> = wifiDao.getHeatmapObservations().map { entries -> entries.map { HeatmapObservation(it.id, it.timestamp, it.latitude, it.longitude, it.ssid, it.bssid, it.rssi, it.frequency) } }

    override fun getHeatmapSummary(ssid: String?, bssid: String?): Flow<HeatmapSummary> = getHeatmapObservations().map { heatmapService.summarize(it, ssid, bssid) }

    override suspend fun captureHeatmapObservation(network: WifiNetwork): Boolean {
        val coordinates = wifiManagerHelper.getLastKnownLocation() ?: return false
        val next = heatmapService.createObservation(network, coordinates.first, coordinates.second, System.currentTimeMillis())
        val previous = wifiDao.getHeatmapObservations().first()
            .firstOrNull { it.bssid.equals(next.bssid, ignoreCase = true) }
            ?.let { HeatmapObservation(it.id, it.timestamp, it.latitude, it.longitude, it.ssid, it.bssid, it.rssi, it.frequency) }
        if (!heatmapService.shouldPersist(previous, next)) return false
        wifiDao.insertHeatmapObservation(HeatmapObservationEntity(timestamp = next.timestamp, latitude = next.latitude, longitude = next.longitude, ssid = next.ssid, bssid = next.bssid, rssi = next.rssi, frequency = next.frequency))
        return true
    }

    override suspend fun captureBackgroundObservation(): Boolean = wifiManagerHelper.getConnectedWifiInfo()?.let { captureHeatmapObservation(it) } ?: false

    override fun setBackgroundObservationEnabled(enabled: Boolean) = backgroundObservationScheduler.setEnabled(enabled)

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

}
