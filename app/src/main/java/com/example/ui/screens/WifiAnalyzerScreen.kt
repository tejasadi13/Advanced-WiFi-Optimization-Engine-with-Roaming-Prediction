package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.ChannelCongestionAnalysis
import com.example.domain.model.WifiAnalysis
import com.example.domain.model.WifiBand
import com.example.ui.design.NetPulseCard
import com.example.ui.design.NetPulseEmptyState
import com.example.ui.design.NetPulseErrorState
import com.example.ui.design.NetPulseLoadingState
import com.example.ui.design.NetPulseScoreIndicator
import com.example.ui.design.NetPulseSectionHeader
import com.example.ui.viewmodel.WifiWiseViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics

@Composable
fun WifiAnalyzerScreen(viewModel: WifiWiseViewModel) {
    val analysis by viewModel.wifiAnalysis.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanError by viewModel.nearbyScanError.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .testTag("wifi_analyzer_screen")
    ) {
        when {
            analysis != null -> AnalyzerContent(analysis!!)
            isScanning -> NetPulseLoadingState("Analyzing observed nearby networks…", Modifier.align(Alignment.Center))
            scanError != null -> NetPulseErrorState(
                title = "Analysis unavailable",
                message = scanError.orEmpty(),
                modifier = Modifier.align(Alignment.Center).padding(20.dp)
            )
            else -> NetPulseEmptyState(
                title = "No analysis available",
                message = "Connect to Wi-Fi and scan nearby access points to view network health.",
                icon = Icons.Rounded.Analytics,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun AnalyzerContent(analysis: WifiAnalysis) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).testTag("analyzer_content"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(12.dp)) }
        item { HealthSummary(analysis) }
        item { AssessmentCard(analysis) }
        item { BandDistributionCard(analysis) }
        item { Text("Channel congestion", style = MaterialTheme.typography.titleLarge) }
        items(analysis.channelCongestion, key = { it.band.name }) { ChannelCard(it) }
        item { Spacer(Modifier.height(14.dp)) }
    }
}

@Composable
private fun HealthSummary(analysis: WifiAnalysis) {
    NetPulseCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("NETWORK HEALTH", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(5.dp))
                Text(analysis.targetNetwork.ssid, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                Text("Observed connection quality", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            NetPulseScoreIndicator(analysis.networkHealthScore, "out of 100")
        }
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { analysis.networkHealthScore / 100f }, modifier = Modifier.fillMaxWidth(),
            color = scoreColor(analysis.networkHealthScore), trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Text("Signal, local channel congestion, security, and frequency band are included in this score.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AssessmentCard(analysis: WifiAnalysis) {
    NetPulseCard {
        Text("Connection assessment", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        MetricRow("RSSI", "${analysis.targetNetwork.rssi} dBm · ${analysis.rssiClassification.displayName}")
        MetricRow("Security", "${analysis.securityAssessment.protocol} · ${analysis.securityAssessment.rating}")
        MetricRow("Security score", "${analysis.securityAssessment.score}/100")
        MetricRow("Channel", analysis.targetNetwork.channel.toString())
        MetricRow("Frequency", "${analysis.targetNetwork.frequency} MHz")
        MetricRow("Band", bandLabel(analysis.targetNetwork.frequency))
    }
}

@Composable
private fun BandDistributionCard(analysis: WifiAnalysis) {
    NetPulseCard {
        Text("Band distribution", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        WifiBand.entries.forEach { band ->
            val count = analysis.bandDistribution.counts[band] ?: 0
            val percent = analysis.bandDistribution.percentageFor(band)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(band.displayName, style = MaterialTheme.typography.bodyMedium)
                Text("$count APs · $percent%", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(5.dp))
            LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ChannelCard(congestion: ChannelCongestionAnalysis) {
    NetPulseCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(congestion.band.displayName, style = MaterialTheme.typography.titleMedium)
            Text("${congestion.accessPointCount} APs", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(10.dp))
        if (congestion.accessPointCount == 0) {
            Text("No access points observed in this band.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            MetricRow("Observed congestion", "${congestion.congestionScore}/100")
            MetricRow("Recommended channel", congestion.bestChannel?.toString() ?: "Unavailable")
            Text(
                congestion.channelCounts.entries.joinToString(" · ") { (channel, count) -> "Ch $channel: $count" },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
    }
}

private fun scoreColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF15803D)
    score >= 60 -> Color(0xFFB45309)
    else -> Color(0xFFDC2626)
}

private fun bandLabel(frequency: Int): String = when {
    frequency >= 5925 -> "6 GHz"
    frequency >= 4900 -> "5 GHz"
    else -> "2.4 GHz"
}
