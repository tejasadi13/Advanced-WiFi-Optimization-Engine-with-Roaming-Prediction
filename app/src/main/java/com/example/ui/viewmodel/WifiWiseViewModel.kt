package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.RoamingPrediction
import com.example.domain.model.ScanHistoryItem
import com.example.domain.model.WifiNetwork
import com.example.domain.model.WifiRecommendation
import com.example.domain.repository.WifiRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WifiWiseViewModel(
    private val repository: WifiRepository
) : ViewModel() {

    // Login state
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    // Scanning & state indicators
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Settings
    private val _roamingEngineEnabled = MutableStateFlow(true)
    val roamingEngineEnabled: StateFlow<Boolean> = _roamingEngineEnabled.asStateFlow()

    private val _autoOptimizeEnabled = MutableStateFlow(false)
    val autoOptimizeEnabled: StateFlow<Boolean> = _autoOptimizeEnabled.asStateFlow()

    // Local DB Saved Networks (Mapped from Room)
    val savedNetworks: StateFlow<List<WifiNetwork>> = repository.getSavedNetworks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Local DB Scan History
    val scanHistory: StateFlow<List<ScanHistoryItem>> = repository.getScanHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Scan Results
    private val _liveNetworks = MutableStateFlow<List<WifiNetwork>>(emptyList())
    val liveNetworks: StateFlow<List<WifiNetwork>> = _liveNetworks.asStateFlow()

    // Nearby WiFi access points from WifiManager scan results
    private val _nearbyNetworks = MutableStateFlow<List<WifiNetwork>>(emptyList())
    val nearbyNetworks: StateFlow<List<WifiNetwork>> = _nearbyNetworks.asStateFlow()

    private val _lastNearbyScanTimestamp = MutableStateFlow<Long?>(null)
    val lastNearbyScanTimestamp: StateFlow<Long?> = _lastNearbyScanTimestamp.asStateFlow()

    private val _nearbyScanError = MutableStateFlow<String?>(null)
    val nearbyScanError: StateFlow<String?> = _nearbyScanError.asStateFlow()

    // Currently connected network (derived from live networks)
    val connectedNetwork: StateFlow<WifiNetwork?> = liveNetworks
        .combine(_liveNetworks) { networks, _ ->
            networks.firstOrNull { it.isConnected }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Roaming predictions
    private val _predictions = MutableStateFlow<List<RoamingPrediction>>(emptyList())
    val predictions: StateFlow<List<RoamingPrediction>> = _predictions.asStateFlow()

    // Recommendations (re-calculated dynamically based on live networks)
    private val _recommendations = MutableStateFlow<List<WifiRecommendation>>(emptyList())
    val recommendations: StateFlow<List<WifiRecommendation>> = _recommendations.asStateFlow()

    private var scanJob: Job? = null
    private var nearbyScanJob: Job? = null
    private var predictionJob: Job? = null
    private var recommendationJob: Job? = null

    init {
        // Automatically begin fetching real-time simulation on start
        startLiveWiFiEngine()
        startNearbyWiFiEngine()
    }

    fun login(email: String, name: String): Boolean {
        if (email.contains("@") && email.length > 3) {
            _userEmail.value = email
            _isLoggedIn.value = true
            return true
        }
        return false
    }

    fun logout() {
        _isLoggedIn.value = false
        _userEmail.value = ""
    }

    fun toggleRoamingEngine(enabled: Boolean) {
        _roamingEngineEnabled.value = enabled
        if (enabled) {
            startPredictionEngine()
        } else {
            predictionJob?.cancel()
            _predictions.value = emptyList()
        }
    }

    fun toggleAutoOptimize(enabled: Boolean) {
        _autoOptimizeEnabled.value = enabled
    }

    fun triggerManualScan() {
        startNearbyWiFiEngine(logSuccessfulScan = true)
    }

    fun clearScanHistory() {
        viewModelScope.launch {
            repository.clearScanHistory()
        }
    }

    fun toggleSaveNetwork(network: WifiNetwork) {
        viewModelScope.launch {
            val isAlreadySaved = savedNetworks.value.any { it.bssid == network.bssid }
            if (isAlreadySaved) {
                repository.deleteNetwork(network.bssid)
            } else {
                repository.saveNetwork(network)
            }
        }
    }

    fun applyRecommendation(recommendationId: String) {
        viewModelScope.launch {
            // Find and mark as applied
            _recommendations.value = _recommendations.value.map {
                if (it.id == recommendationId) it.copy(isApplied = true) else it
            }
            
            // Also log to history
            repository.addScanHistory(
                ScanHistoryItem(
                    timestamp = System.currentTimeMillis(),
                    averageSignalStrength = connectedNetwork.value?.rssi ?: -50,
                    networkCount = _liveNetworks.value.size,
                    optimizedCount = 1,
                    securityIssuesFound = 0,
                    statusMessage = "Applied optimization target recommendation."
                )
            )
        }
    }

    private fun startLiveWiFiEngine() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            repository.getLiveWifiNetworks().collect { networks ->
                _liveNetworks.value = networks
                
                // Trigger predictions and recommendations on new data
                if (_roamingEngineEnabled.value) {
                    startPredictionEngine()
                }
                startRecommendationEngine(networks)
            }
        }
    }

    private fun startNearbyWiFiEngine(logSuccessfulScan: Boolean = false) {
        nearbyScanJob?.cancel()
        _isScanning.value = true
        _nearbyScanError.value = null

        nearbyScanJob = viewModelScope.launch {
            var shouldLogScan = logSuccessfulScan
            repository.getNearbyNetworks()
                .catch { exception ->
                    _nearbyScanError.value =
                        exception.message ?: "Unable to retrieve nearby WiFi networks."
                    _isScanning.value = false
                }
                .collect { networks ->
                    val scanTimestamp = System.currentTimeMillis()
                    _nearbyNetworks.value = networks
                    _lastNearbyScanTimestamp.value = scanTimestamp
                    _nearbyScanError.value = null
                    _isScanning.value = false

                    if (shouldLogScan && networks.isNotEmpty()) {
                        val secureCount = networks.count { it.securityType != "None" }
                        repository.addScanHistory(
                            ScanHistoryItem(
                                timestamp = scanTimestamp,
                                averageSignalStrength =
                                    networks.map { it.rssi }.average().toInt(),
                                networkCount = networks.size,
                                optimizedCount = networks.count { it.isConnected },
                                securityIssuesFound = networks.size - secureCount,
                                statusMessage = "Manual nearby WiFi scan completed."
                            )
                        )
                        shouldLogScan = false
                    }
                }
        }
    }

    private fun startPredictionEngine() {
        predictionJob?.cancel()
        predictionJob = viewModelScope.launch {
            val current = _liveNetworks.value.firstOrNull { it.isConnected }
            repository.getRoamingPredictions(current).collect { predictionList ->
                _predictions.value = predictionList
            }
        }
    }

    private fun startRecommendationEngine(networks: List<WifiNetwork>) {
        recommendationJob?.cancel()
        recommendationJob = viewModelScope.launch {
            repository.getWifiRecommendations(networks).collect { recs ->
                // Maintain applied state
                val currentAppliedIds = _recommendations.value.filter { it.isApplied }.map { it.id }.toSet()
                _recommendations.value = recs.map { rec ->
                    if (currentAppliedIds.contains(rec.id)) rec.copy(isApplied = true) else rec
                }
            }
        }
    }
}
