package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.NetworkJourneyEvent
import com.example.ui.design.NetPulseCard
import com.example.ui.design.NetPulseEmptyState
import com.example.ui.design.NetPulseSectionHeader
import com.example.ui.viewmodel.WifiWiseViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun NetworkJourneyScreen(viewModel: WifiWiseViewModel) {
    val events by viewModel.networkJourney.collectAsState()
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp).testTag("network_journey_screen"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Spacer(Modifier.height(12.dp)) }
        if (events.isEmpty()) item { NetPulseEmptyState("No journey events yet", "Connect to Wi-Fi and use NetPulse to build a local observation timeline.", Icons.Rounded.History) }
        else items(events, key = { it.id }) { JourneyCard(it) }
        item { Spacer(Modifier.height(14.dp)) }
    }
}

@Composable
private fun JourneyCard(event: NetworkJourneyEvent) {
    var expanded by remember(event.id) { mutableStateOf(false) }
    val color = when (event.type.name) { "SIGNAL_DEGRADED", "ROAM_NOW" -> MaterialTheme.colorScheme.error; "PREPARE_ROAMING" -> Color(0xFFB45309); else -> MaterialTheme.colorScheme.primary }
    NetPulseCard(Modifier.animateContentSize()) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.History, event.type.name.replace('_', ' '), tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.titleMedium)
                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(event.timestamp)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(event.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            event.ssid?.let { EventMetric("Network", it, Modifier.weight(1f)) }
            event.rssi?.let { EventMetric("RSSI", "$it dBm", Modifier.weight(1f)) }
            event.healthScore?.let { EventMetric("Health", "$it/100", Modifier.weight(1f)) }
        }
        if (event.predictionState != null || event.candidateSsid != null) {
            Row(Modifier.fillMaxWidth().clip(CircleShape).clickable { expanded = !expanded }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Why?", style = MaterialTheme.typography.labelLarge, color = color, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ExpandMore, if (expanded) "Hide event details" else "Show event details", tint = color, modifier = Modifier.rotate(if (expanded) 180f else 0f))
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(top = 4.dp)) {
                    event.predictionState?.let { Text("Roaming state: ${it.replace('_', ' ')}", style = MaterialTheme.typography.bodySmall) }
                    event.candidateSsid?.let { Text("Candidate AP: $it${event.candidateRssi?.let { rssi -> " · $rssi dBm" } ?: ""}", style = MaterialTheme.typography.bodySmall) }
                    event.band?.let { Text("Observed band: $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

@Composable private fun EventMetric(label: String, value: String, modifier: Modifier) = Column(modifier) { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.bodyMedium, maxLines = 1) }
