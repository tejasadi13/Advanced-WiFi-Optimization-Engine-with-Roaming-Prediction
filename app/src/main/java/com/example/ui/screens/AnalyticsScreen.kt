package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.SpeedTestResult
import com.example.ui.viewmodel.WifiWiseViewModel

@Composable
fun AnalyticsScreen(viewModel: WifiWiseViewModel) {
    val analysis by viewModel.wifiAnalysis.collectAsState()
    val prediction by viewModel.roamingPrediction.collectAsState()
    val speedHistory by viewModel.speedTestHistory.collectAsState()
    val journey by viewModel.networkJourney.collectAsState()
    val heatmapObservations by viewModel.heatmapObservations.collectAsState()
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val pingSamples = speedHistory.takeLast(7).map { it.pingMs.toFloat() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp).testTag("analytics_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Advisory guidance calculated from observed Wi-Fi and speed-test data", style = MaterialTheme.typography.bodyLarge, color = onSurfaceVariant)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WifiTethering, "Roaming status", tint = primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("ROAMING STATUS", style = MaterialTheme.typography.labelMedium, color = primary)
                        Text(prediction?.recommendation?.name?.replace('_', ' ') ?: "UNAVAILABLE", style = MaterialTheme.typography.titleLarge)
                        val evidence = prediction?.let { "${it.currentRssi} dBm · ${it.trend.name.lowercase().replaceFirstChar { c -> c.uppercase() }} trend · ${it.predictionScore}% likelihood · ${it.rssiSampleCount} samples" }
                        Text(evidence ?: "More observed Wi-Fi samples are needed for a prediction.", style = MaterialTheme.typography.bodyMedium, color = onSurfaceVariant)
                        prediction?.recommendedAccessPoint?.let { candidate ->
                            Text("Candidate AP: ${candidate.ssid} · ${candidate.rssi} dBm", style = MaterialTheme.typography.bodySmall, color = onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Measured ping history", style = MaterialTheme.typography.titleMedium)
                    Text("Milliseconds recorded by completed speed tests", style = MaterialTheme.typography.bodySmall, color = onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    if (pingSamples.isEmpty()) {
                        Text("Complete a speed test to populate this chart.", style = MaterialTheme.typography.bodyMedium, color = onSurfaceVariant, modifier = Modifier.padding(vertical = 36.dp))
                    } else {
                        Box(Modifier.fillMaxWidth().height(150.dp)) {
                            Canvas(Modifier.fillMaxSize()) {
                                val maxValue = (pingSamples.maxOrNull() ?: 1f).coerceAtLeast(1f)
                                val barWidth = size.width / (pingSamples.size * 1.8f)
                                val spacing = size.width / pingSamples.size
                                pingSamples.forEachIndexed { index, ping ->
                                    val barHeight = size.height * (ping / maxValue)
                                    drawRoundRect(primary, Offset(index * spacing + (spacing - barWidth) / 2, size.height - barHeight), Size(barWidth, barHeight), CornerRadius(5.dp.toPx(), 5.dp.toPx()))
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiMetricCard("Average Ping", speedHistory.averageOrDash { it.pingMs }, "Measured latency", Modifier.weight(1f))
                KpiMetricCard("Speed Tests", speedHistory.size.toString(), "Persisted results", Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiMetricCard("Journey events", journey.size.toString(), "Meaningful local changes", Modifier.weight(1f))
                KpiMetricCard("Signal map", heatmapObservations.size.toString(), "Location-backed observations", Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiMetricCard("Network Health", analysis?.let { "${it.networkHealthScore}/100" } ?: "—", "Analyzer output", Modifier.weight(1f))
                KpiMetricCard("Roaming State", prediction?.recommendation?.name ?: "—", "Current prediction", Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, null, tint = primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Roaming output is advisory. NetPulse does not perform automatic Wi-Fi handoff.", style = MaterialTheme.typography.bodySmall, color = onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun KpiMetricCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun List<SpeedTestResult>.averageOrDash(selector: (SpeedTestResult) -> Double): String {
    if (isEmpty()) return "—"
    return "%.1f ms".format(averageOf(selector))
}

private fun List<SpeedTestResult>.averageOf(selector: (SpeedTestResult) -> Double): Double = map(selector).average()
