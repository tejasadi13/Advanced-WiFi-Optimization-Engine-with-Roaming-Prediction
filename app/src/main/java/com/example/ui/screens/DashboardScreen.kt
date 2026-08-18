package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NetPulseSuccess
import com.example.ui.theme.NetPulseWarning
import com.example.ui.viewmodel.WifiWiseViewModel

@Composable
fun DashboardScreen(
    viewModel: WifiWiseViewModel,
    onNavigateToScanner: () -> Unit,
    onNavigateToAnalyzer: () -> Unit,
    onNavigateToRecommendations: () -> Unit,
    onNavigateToSpeedTest: () -> Unit,
    onNavigateToJourney: () -> Unit,
    onNavigateToHeatmap: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val connectedNetwork by viewModel.connectedNetwork.collectAsState()
    val predictions by viewModel.predictions.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val roamingEngineEnabled by viewModel.roamingEngineEnabled.collectAsState()
    val wifiAnalysis by viewModel.wifiAnalysis.collectAsState()

    val pendingRecsCount = recommendations.count { !it.isApplied }
    val activePrediction = predictions.firstOrNull { it.predictionConfidence > 0.6f }
    val greetingName = userEmail.substringBefore("@").takeIf { userEmail.isNotBlank() } ?: "there"

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp).testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Spacer(modifier = Modifier.height(10.dp)) }
        item {
            Column {
                Text("Good morning, $greetingName", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(3.dp))
                Text("Here\u2019s how your network feels today.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            ConnectionHero(
                networkName = connectedNetwork?.ssid ?: "No WiFi connection",
                networkDetails = connectedNetwork?.let { "${it.rssi} dBm  \u00B7  ${it.estimatedSpeedMbps} Mbps" }
                    ?: "Connect to WiFi to see live network health.",
                healthScore = wifiAnalysis?.networkHealthScore,
                isConnected = connectedNetwork != null
            )
        }
        item { SectionLabel("Quick actions") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureAction("Scanner", "Nearby WiFi", Icons.Rounded.Radar, MaterialTheme.colorScheme.primary, Modifier.weight(1f), onNavigateToScanner)
                    FeatureAction("Analyzer", "Channel health", Icons.Rounded.NetworkCheck, MaterialTheme.colorScheme.secondary, Modifier.weight(1f), onNavigateToAnalyzer)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureAction("Speed Test", "Measure internet", Icons.Rounded.Speed, NetPulseWarning, Modifier.weight(1f), onNavigateToSpeedTest)
                    FeatureAction("Recommendations", "Smart guidance", Icons.Rounded.AutoAwesome, NetPulseSuccess, Modifier.weight(1f), onNavigateToRecommendations)
                }
            }
        }
        item { SectionLabel("At a glance") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item { InsightCard("Signal", connectedNetwork?.let { "${it.rssi} dBm" } ?: "Offline", Icons.Rounded.Wifi, NetPulseSuccess) }
                item { InsightCard("Band", if (connectedNetwork?.is5GHz == true) "5 GHz" else "2.4 GHz", Icons.Rounded.NetworkCheck, MaterialTheme.colorScheme.secondary) }
            }
        }
        item { SectionLabel("For you") }
        item { RecommendationCard(pendingRecsCount, recommendations.firstOrNull { !it.isApplied }?.title, activePrediction?.candidateSsid, onNavigateToRecommendations) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                CompactLink("Network journey", "Observed timeline", Icons.Rounded.History, Modifier.weight(1f), onNavigateToJourney)
                CompactLink("Signal map", "Location-backed", Icons.Rounded.Wifi, Modifier.weight(1f), onNavigateToHeatmap)
            }
        }
        item { Spacer(modifier = Modifier.height(22.dp)) }
    }
}

@Composable
private fun ConnectionHero(networkName: String, networkDetails: String, healthScore: Int?, isConnected: Boolean) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Wifi, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(29.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (isConnected) "CONNECTED NOW" else "NO ACTIVE CONNECTION", style = MaterialTheme.typography.labelMedium,
                        color = if (isConnected) NetPulseSuccess else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f))
                    Text(networkName, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("NETWORK HEALTH", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Text(healthScore?.toString() ?: "\u2014", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(if (healthScore != null) "out of 100" else "Available when connected", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f))
                }
                Text(networkDetails, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    maxLines = 2, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FeatureAction(title: String, subtitle: String, icon: ImageVector, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "quickActionScale")
    val iconShape = when (title) {
        "Scanner" -> RoundedCornerShape(17.dp)
        "Analyzer" -> CircleShape
        "Speed Test" -> RoundedCornerShape(12.dp)
        else -> RoundedCornerShape(22.dp)
    }
    Card(
        modifier = modifier.height(142.dp).scale(scale).clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick),
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(46.dp).clip(iconShape).background(accent), contentAlignment = Alignment.Center) {
                    Icon(icon, title, tint = Color.White, modifier = Modifier.size(25.dp))
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(if (title == "Speed Test") 18.dp else 14.dp).clip(CircleShape).background(accent.copy(alpha = 0.22f)))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InsightCard(label: String, value: String, icon: ImageVector, accent: Color, onClick: (() -> Unit)? = null) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "insightScale")
    Card(
        modifier = Modifier.width(152.dp).height(118.dp).scale(scale)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick) else Modifier),
        shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun RecommendationCard(count: Int, title: String?, prediction: String?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Security, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(if (count > 0) "$count helpful suggestion${if (count == 1) "" else "s"}" else "Everything looks good",
                    style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(title ?: prediction?.let { "Next network: $it" } ?: "Keep exploring your network insights.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text("Why?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.clickable(onClick = onClick))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun CompactLink(title: String, subtitle: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column { Text(title, style = MaterialTheme.typography.labelLarge, maxLines = 1); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
        }
    }
}
