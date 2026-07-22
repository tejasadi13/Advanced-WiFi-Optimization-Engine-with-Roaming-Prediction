package com.example.domain.repository

import com.example.domain.model.RoamingPrediction
import com.example.domain.model.ScanHistoryItem
import com.example.domain.model.WifiNetwork
import com.example.domain.model.WifiRecommendation
import kotlinx.coroutines.flow.Flow

interface WifiRepository {
    fun getSavedNetworks(): Flow<List<WifiNetwork>>
    suspend fun saveNetwork(network: WifiNetwork)
    suspend fun deleteNetwork(bssid: String)

    fun getScanHistory(): Flow<List<ScanHistoryItem>>
    suspend fun addScanHistory(item: ScanHistoryItem)
    suspend fun clearScanHistory()

    fun getLiveWifiNetworks(): Flow<List<WifiNetwork>>
    fun getRoamingPredictions(currentNetwork: WifiNetwork?): Flow<List<RoamingPrediction>>
    fun getWifiRecommendations(liveNetworks: List<WifiNetwork>): Flow<List<WifiRecommendation>>
}
