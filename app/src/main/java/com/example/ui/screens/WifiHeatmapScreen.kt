package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.HeatmapObservation
import com.example.ui.design.NetPulseCard
import com.example.ui.design.NetPulseEmptyState
import com.example.ui.design.NetPulseSectionHeader
import com.example.ui.viewmodel.WifiWiseViewModel

@Composable
fun WifiHeatmapScreen(viewModel: WifiWiseViewModel) {
    val observations by viewModel.heatmapObservations.collectAsState()
    var selectedSsid by remember { mutableStateOf<String?>(null) }
    var selectedBssid by remember { mutableStateOf<String?>(null) }
    val displayed = observations.filter { (selectedSsid == null || it.ssid == selectedSsid) && (selectedBssid == null || it.bssid == selectedBssid) }
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp).testTag("wifi_heatmap_screen"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Spacer(Modifier.height(12.dp)) }
        if (observations.isEmpty()) item { NetPulseEmptyState("Location required for signal mapping", "Enable Location Services, connect to Wi-Fi, and let NetPulse collect a valid observation.", Icons.Rounded.LocationOn) }
        else {
            item { ObservationFilters(observations, selectedSsid, selectedBssid, { selectedSsid = it; selectedBssid = null }, { selectedBssid = it }) }
            item { SignalMap(displayed) }
            item { HeatmapStats(displayed) }
            item { Text("Each point represents a real stored observation; this is not a floor plan.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        item { Spacer(Modifier.height(14.dp)) }
    }
}

@Composable private fun ObservationFilters(observations: List<HeatmapObservation>, selectedSsid: String?, selectedBssid: String?, onSsid: (String?) -> Unit, onBssid: (String?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selectedSsid == null, { onSsid(null) }, { Text("Current view") })
            observations.map { it.ssid }.distinct().forEach { ssid -> FilterChip(selectedSsid == ssid, { onSsid(ssid) }, { Text(ssid) }) }
        }
        selectedSsid?.let { ssid ->
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selectedBssid == null, { onBssid(null) }, { Text("All APs") })
                observations.filter { it.ssid == ssid }.map { it.bssid }.distinct().forEach { bssid -> FilterChip(selectedBssid == bssid, { onBssid(bssid) }, { Text(bssid) }) }
            }
        }
    }
}

@Composable private fun SignalMap(observations: List<HeatmapObservation>) {
    val minLat = observations.minOf { it.latitude }; val maxLat = observations.maxOf { it.latitude }
    val minLon = observations.minOf { it.longitude }; val maxLon = observations.maxOf { it.longitude }
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    NetPulseCard {
        Text("Observed signal zones", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Canvas(Modifier.fillMaxWidth().height(260.dp)) {
            observations.forEach { point ->
                val x = if (maxLon == minLon) size.width / 2 else ((point.longitude - minLon) / (maxLon - minLon)).toFloat() * size.width
                val y = if (maxLat == minLat) size.height / 2 else size.height - ((point.latitude - minLat) / (maxLat - minLat)).toFloat() * size.height
                val color = when { point.rssi >= -55 -> Color(0xFF15803D); point.rssi >= -67 -> primary; point.rssi >= -75 -> Color(0xFFB45309); else -> error }
                drawCircle(color.copy(alpha = .20f), 22.dp.toPx(), androidx.compose.ui.geometry.Offset(x, y))
                drawCircle(color, 6.dp.toPx(), androidx.compose.ui.geometry.Offset(x, y))
            }
        }
    }
}

@Composable private fun HeatmapStats(observations: List<HeatmapObservation>) = NetPulseCard {
    val strongest = observations.maxOf { it.rssi }; val weakest = observations.minOf { it.rssi }; val average = observations.map { it.rssi }.average().toInt()
    Text("Observation summary", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(10.dp))
    Text("${observations.size} observations · strongest $strongest dBm · weakest $weakest dBm · average $average dBm", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
