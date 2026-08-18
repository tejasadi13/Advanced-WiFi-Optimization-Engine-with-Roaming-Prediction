package com.example.domain.repository

import com.example.domain.model.RoamingPrediction
import com.example.domain.model.ScanHistoryItem
import com.example.domain.model.WifiNetwork
import com.example.domain.model.WifiAnalysis
import com.example.domain.model.SpeedTestResult
import com.example.domain.model.NetworkRecommendation
import com.example.domain.model.NetworkJourneyEvent
import com.example.domain.model.HeatmapObservation
import com.example.domain.model.HeatmapSummary
import kotlinx.coroutines.flow.Flow

interface WifiRepository {
    fun getSavedNetworks(): Flow<List<WifiNetwork>>
    suspend fun saveNetwork(network: WifiNetwork)
    suspend fun deleteNetwork(bssid: String)

    fun getScanHistory(): Flow<List<ScanHistoryItem>>
    suspend fun addScanHistory(item: ScanHistoryItem)
    suspend fun clearScanHistory()

    fun getLiveWifiNetworks(): Flow<List<WifiNetwork>>
    fun getNearbyNetworks(): Flow<List<WifiNetwork>>
    fun analyzeNetworks(networks: List<WifiNetwork>): WifiAnalysis?
    fun getRoamingPrediction(
        currentNetwork: WifiNetwork?,
        nearbyNetworks: List<WifiNetwork>
    ): Flow<RoamingPrediction?>
    fun getSpeedTestHistory(): Flow<List<SpeedTestResult>>
    suspend fun saveSpeedTestResult(result: SpeedTestResult)
    fun getNetworkJourney(): Flow<List<NetworkJourneyEvent>>
    suspend fun recordNetworkJourney(events: List<NetworkJourneyEvent>)
    fun getHeatmapObservations(): Flow<List<HeatmapObservation>>
    fun getHeatmapSummary(ssid: String? = null, bssid: String? = null): Flow<HeatmapSummary>
    suspend fun captureHeatmapObservation(network: WifiNetwork): Boolean
    suspend fun captureBackgroundObservation(): Boolean
    fun setBackgroundObservationEnabled(enabled: Boolean)
    fun getNetworkRecommendations(
        nearbyNetworks: List<WifiNetwork>,
        connectedNetwork: WifiNetwork?,
        analyzerResult: WifiAnalysis?,
        roamingPrediction: RoamingPrediction?,
        latestSpeedTest: SpeedTestResult?
    ): Flow<List<NetworkRecommendation>>
}
