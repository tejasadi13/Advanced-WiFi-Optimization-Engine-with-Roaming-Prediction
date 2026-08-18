package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.RoamingPrediction
import com.example.domain.model.ScanHistoryItem
import com.example.domain.model.WifiNetwork
import com.example.domain.model.WifiRecommendation
import com.example.domain.model.WifiAnalysis
import com.example.domain.model.SpeedTestResult
import com.example.domain.model.SpeedTestState
import com.example.domain.model.SpeedTestPhase
import com.example.domain.model.NetworkRecommendation
import com.example.domain.model.RecommendationCategory
import com.example.domain.model.RecommendationPriority
import com.example.domain.repository.WifiRepository
import com.example.domain.service.SpeedTestService
import com.example.domain.service.NetworkJourneyService
import com.example.domain.model.NetworkJourneyEvent
import com.example.domain.model.HeatmapObservation
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WifiWiseViewModel(
    private val repository: WifiRepository,
    private val speedTestService: SpeedTestService,
    private val networkJourneyService: NetworkJourneyService
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

    private val _wifiAnalysis = MutableStateFlow<WifiAnalysis?>(null)
    val wifiAnalysis: StateFlow<WifiAnalysis?> = _wifiAnalysis.asStateFlow()

    // Currently connected network (derived from live networks)
    val connectedNetwork: StateFlow<WifiNetwork?> = liveNetworks
        .combine(_liveNetworks) { networks, _ ->
            networks.firstOrNull { it.isConnected }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Roaming predictions
    private val _roamingPrediction = MutableStateFlow<RoamingPrediction?>(null)
    val roamingPrediction: StateFlow<RoamingPrediction?> = _roamingPrediction.asStateFlow()

    private val _predictions = MutableStateFlow<List<RoamingPrediction>>(emptyList())
    val predictions: StateFlow<List<RoamingPrediction>> = _predictions.asStateFlow()

    // Recommendations (re-calculated dynamically based on live networks)
    private val _recommendations = MutableStateFlow<List<WifiRecommendation>>(emptyList())
    val recommendations: StateFlow<List<WifiRecommendation>> = _recommendations.asStateFlow()

    private val _networkRecommendations = MutableStateFlow<List<NetworkRecommendation>>(emptyList())
    val networkRecommendations: StateFlow<List<NetworkRecommendation>> = _networkRecommendations.asStateFlow()

    private val _speedTestState = MutableStateFlow(SpeedTestState())
    val speedTestState: StateFlow<SpeedTestState> = _speedTestState.asStateFlow()

    val speedTestHistory: StateFlow<List<SpeedTestResult>> = repository.getSpeedTestHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val networkJourney: StateFlow<List<NetworkJourneyEvent>> = repository.getNetworkJourney()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val heatmapObservations: StateFlow<List<HeatmapObservation>> = repository.getHeatmapObservations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var scanJob: Job? = null
    private var nearbyScanJob: Job? = null
    private var predictionJob: Job? = null
    private var recommendationJob: Job? = null
    private var speedTestJob: Job? = null
    private var journeyJob: Job? = null

    init {
        // Automatically begin collecting live Wi-Fi observations on start
        startLiveWiFiEngine()
        startNearbyWiFiEngine()
        startPredictionEngine()
        startRecommendationEngine()
        startJourneyEngine()
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
        if (!enabled) {
            _roamingPrediction.value = null
            _predictions.value = emptyList()
        }
    }

    fun toggleAutoOptimize(enabled: Boolean) {
        _autoOptimizeEnabled.value = enabled
        repository.setBackgroundObservationEnabled(enabled)
    }

    fun startSpeedTest() {
        if (speedTestJob?.isActive == true) return
        val network = connectedNetwork.value
        if (network == null) {
            _speedTestState.value = SpeedTestState(
                phase = SpeedTestPhase.FAILED,
                errorMessage = "Connect to WiFi before starting a speed test."
            )
            return
        }

        speedTestJob = viewModelScope.launch {
            _speedTestState.value = SpeedTestState(
                phase = SpeedTestPhase.CONNECTING,
                networkName = network.ssid
            )
            try {
                val result = speedTestService.run(network.ssid) { phase, progress, download, upload, ping, jitter ->
                    _speedTestState.value = _speedTestState.value.copy(
                        phase = phase,
                        progress = progress,
                        networkName = network.ssid,
                        downloadMbps = download ?: _speedTestState.value.downloadMbps,
                        uploadMbps = upload ?: _speedTestState.value.uploadMbps,
                        pingMs = ping ?: _speedTestState.value.pingMs,
                        jitterMs = jitter ?: _speedTestState.value.jitterMs
                    )
                }
                repository.saveSpeedTestResult(result)
                _speedTestState.value = SpeedTestState(
                    phase = SpeedTestPhase.COMPLETED,
                    progress = 1f,
                    downloadMbps = result.downloadMbps,
                    uploadMbps = result.uploadMbps,
                    pingMs = result.pingMs,
                    jitterMs = result.jitterMs,
                    durationMs = result.durationMs,
                    networkName = result.networkName,
                    timestamp = result.timestamp,
                    result = result
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _speedTestState.value = _speedTestState.value.copy(
                    phase = SpeedTestPhase.FAILED,
                    errorMessage = exception.message ?: "Speed test could not be completed."
                )
            }
        }
    }

    fun resetSpeedTest() {
        speedTestJob?.cancel()
        _speedTestState.value = SpeedTestState()
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
        }
    }

    fun acknowledgeNetworkRecommendation(recommendationId: String) {
        _networkRecommendations.value = _networkRecommendations.value.filterNot { it.id == recommendationId }
        _recommendations.value = _recommendations.value.filterNot { it.id == recommendationId }
    }

    private fun startLiveWiFiEngine() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            repository.getLiveWifiNetworks().collect { networks ->
                _liveNetworks.value = networks
                updateWifiAnalysis()
                networks.firstOrNull { it.isConnected }?.let { network ->
                    viewModelScope.launch { repository.captureHeatmapObservation(network) }
                }

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
                    _wifiAnalysis.value = null
                    _isScanning.value = false
                }
                .collect { networks ->
                    val scanTimestamp = System.currentTimeMillis()
                    _nearbyNetworks.value = networks
                    updateWifiAnalysis()
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

    private fun updateWifiAnalysis() {
        val hasConnectedNetwork = _liveNetworks.value.any { it.isConnected }
        val networks = _nearbyNetworks.value
        _wifiAnalysis.value = if (hasConnectedNetwork && networks.isNotEmpty()) {
            repository.analyzeNetworks(networks)
        } else {
            null
        }
    }

    private fun startPredictionEngine() {
        predictionJob?.cancel()
        predictionJob = viewModelScope.launch {
            combine(_liveNetworks, _nearbyNetworks, _roamingEngineEnabled) { live, nearby, enabled ->
                Triple(live.firstOrNull { it.isConnected }, nearby, enabled)
            }.collect { (currentNetwork, nearbyNetworks, isEnabled) ->
                if (!isEnabled) {
                    _roamingPrediction.value = null
                    _predictions.value = emptyList()
                } else {
                    repository.getRoamingPrediction(currentNetwork, nearbyNetworks).collect { prediction ->
                        _roamingPrediction.value = prediction
                        _predictions.value = prediction?.let(::listOf) ?: emptyList()
                    }
                }
            }
        }
    }

    private fun startRecommendationEngine() {
        recommendationJob?.cancel()
        recommendationJob = viewModelScope.launch {
            combine(_liveNetworks, _nearbyNetworks, _wifiAnalysis, _roamingPrediction, _speedTestState) {
                    live, nearby, analysis, roaming, speed ->
                RecommendationInputs(
                    connected = live.firstOrNull { it.isConnected },
                    nearby = nearby,
                    analysis = analysis,
                    roaming = roaming,
                    speed = speed.result
                )
            }.collect { inputs ->
                repository.getNetworkRecommendations(
                    nearbyNetworks = inputs.nearby,
                    connectedNetwork = inputs.connected,
                    analyzerResult = inputs.analysis,
                    roamingPrediction = inputs.roaming,
                    latestSpeedTest = inputs.speed
                ).collect { recs ->
                    _networkRecommendations.value = recs
                    _recommendations.value = recs.map { rec ->
                        WifiRecommendation(
                            id = rec.id,
                            title = rec.title,
                            description = rec.description,
                            priority = rec.priority.toLegacyPriority(),
                            category = rec.category.toLegacyCategory()
                        )
                    }
                }
            }
        }
    }

    private fun startJourneyEngine() {
        journeyJob?.cancel()
        journeyJob = viewModelScope.launch {
            combine(_liveNetworks, _wifiAnalysis, _roamingPrediction, _networkRecommendations, _speedTestState) { live, analysis, prediction, recommendations, speed ->
                JourneyInputs(live.firstOrNull { it.isConnected }, analysis, prediction, recommendations, speed.result)
            }.collect { input ->
                repository.recordNetworkJourney(
                    networkJourneyService.observe(
                        network = input.connected,
                        analysis = input.analysis,
                        prediction = input.prediction,
                        recommendations = input.recommendations,
                        latestSpeedTest = input.speed,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private data class RecommendationInputs(
        val connected: WifiNetwork?,
        val nearby: List<WifiNetwork>,
        val analysis: WifiAnalysis?,
        val roaming: RoamingPrediction?,
        val speed: SpeedTestResult?
    )

    private data class JourneyInputs(
        val connected: WifiNetwork?,
        val analysis: WifiAnalysis?,
        val prediction: RoamingPrediction?,
        val recommendations: List<NetworkRecommendation>,
        val speed: SpeedTestResult?
    )

    private fun RecommendationPriority.toLegacyPriority(): WifiRecommendation.Priority = when (this) {
        RecommendationPriority.CRITICAL, RecommendationPriority.HIGH -> WifiRecommendation.Priority.HIGH
        RecommendationPriority.MEDIUM -> WifiRecommendation.Priority.MEDIUM
        RecommendationPriority.LOW -> WifiRecommendation.Priority.LOW
    }

    private fun RecommendationCategory.toLegacyCategory(): WifiRecommendation.Category = when (this) {
        RecommendationCategory.SECURITY -> WifiRecommendation.Category.SECURITY
        RecommendationCategory.ROAMING, RecommendationCategory.CONNECTIVITY -> WifiRecommendation.Category.COVERAGE
        else -> WifiRecommendation.Category.PERFORMANCE
    }
}
