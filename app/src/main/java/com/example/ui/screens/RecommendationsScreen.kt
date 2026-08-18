package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.NetworkDecisionExplanation
import com.example.domain.model.NetworkRecommendation
import com.example.domain.model.RecommendationCategory
import com.example.domain.model.RecommendationPriority
import com.example.ui.design.NetPulseCard
import com.example.ui.design.NetPulseEmptyState
import com.example.ui.design.NetPulsePrimaryButton
import com.example.ui.viewmodel.WifiWiseViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun RecommendationsScreen(viewModel: WifiWiseViewModel) {
    val recommendations by viewModel.networkRecommendations.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp).testTag("recommendations_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(12.dp)) }
        if (recommendations.isEmpty()) {
            item {
                NetPulseEmptyState(
                    title = "No recommendations yet",
                    message = "Connect to Wi-Fi and complete a nearby scan to receive data-based guidance.",
                    icon = Icons.Rounded.CheckCircle
                )
            }
        } else {
            items(recommendations, key = { it.id }) { recommendation ->
                RecommendationCard(recommendation, onAction = { viewModel.acknowledgeNetworkRecommendation(recommendation.id) })
            }
        }
        item { Spacer(Modifier.height(14.dp)) }
    }
}

@Composable
private fun RecommendationCard(recommendation: NetworkRecommendation, onAction: () -> Unit) {
    val accent = categoryColor(recommendation.category)
    var expanded by remember(recommendation.id) { mutableStateOf(false) }
    NetPulseCard(modifier = Modifier.animateContentSize()) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                Icon(categoryIcon(recommendation.category), null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(recommendation.category.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium, color = accent)
                Text(recommendation.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            PriorityBadge(recommendation.priority)
        }
        Spacer(Modifier.height(12.dp))
        Text(recommendation.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Detail("Confidence", "${recommendation.confidence}%", Modifier.weight(1f))
            Detail("Expected benefit", recommendation.expectedBenefit, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Text("Updated ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(recommendation.timestamp))}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        recommendation.explanation?.let { explanation ->
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).clickable { expanded = !expanded }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Why this recommendation?", style = MaterialTheme.typography.labelLarge, color = accent, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ExpandMore, if (expanded) "Hide explanation" else "Show explanation", tint = accent, modifier = Modifier.rotate(if (expanded) 180f else 0f))
            }
            AnimatedVisibility(expanded) { EvidencePanel(explanation, accent) }
        }
        Spacer(Modifier.height(12.dp))
        NetPulsePrimaryButton(recommendation.action, onAction, Modifier.fillMaxWidth())
    }
}

@Composable
private fun EvidencePanel(explanation: NetworkDecisionExplanation, accent: Color) {
    Column(Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).background(accent.copy(alpha = .07f)).padding(14.dp)) {
        Text(explanation.primaryReason, style = MaterialTheme.typography.bodyMedium)
        if (explanation.supportingFactors.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            explanation.supportingFactors.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        explanation.currentNetwork?.let { Text("Current AP: ${it.ssid ?: "Unavailable"} · ${it.rssi?.let { rssi -> "$rssi dBm" } ?: "RSSI unavailable"}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
        explanation.candidateNetwork?.let { Text("Candidate AP: ${it.ssid ?: "Unavailable"} · ${it.rssi?.let { rssi -> "$rssi dBm" } ?: "RSSI unavailable"}", style = MaterialTheme.typography.bodySmall) }
        explanation.prediction?.let { Text("Prediction: ${it.recommendation.name.replace('_', ' ')} · ${it.score}/100", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
        explanation.analyzer?.let { Text("Analyzer: ${it.healthScore}/100 · RSSI ${it.rssiContribution} · congestion ${it.congestionContribution}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)) }
        explanation.speedTest?.let { Text("Speed test: ${formatEvidence(it.downloadMbps)} Mbps down · ${formatEvidence(it.pingMs)} ms ping", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)) }
    }
}

@Composable private fun Detail(label: String, value: String, modifier: Modifier) = Column(modifier) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
}

@Composable private fun PriorityBadge(priority: RecommendationPriority) {
    val color = when (priority) { RecommendationPriority.CRITICAL, RecommendationPriority.HIGH -> MaterialTheme.colorScheme.error; RecommendationPriority.MEDIUM -> Color(0xFFB45309); RecommendationPriority.LOW -> MaterialTheme.colorScheme.primary }
    Box(Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(9.dp)).background(color.copy(alpha = .12f)).padding(horizontal = 8.dp, vertical = 5.dp)) { Text(priority.name, style = MaterialTheme.typography.labelMedium, color = color) }
}

private fun categoryIcon(category: RecommendationCategory): ImageVector = when (category) { RecommendationCategory.SECURITY -> Icons.Rounded.Security; RecommendationCategory.PERFORMANCE -> Icons.Rounded.Speed; RecommendationCategory.ROAMING, RecommendationCategory.CONGESTION -> Icons.Rounded.NetworkCheck; RecommendationCategory.CONNECTIVITY -> Icons.Rounded.Wifi; RecommendationCategory.OPTIMIZATION -> Icons.Rounded.Tune }
@Composable
private fun categoryColor(category: RecommendationCategory): Color = when (category) { RecommendationCategory.SECURITY -> MaterialTheme.colorScheme.error; RecommendationCategory.PERFORMANCE, RecommendationCategory.OPTIMIZATION -> MaterialTheme.colorScheme.primary; RecommendationCategory.ROAMING -> Color(0xFF7C3AED); RecommendationCategory.CONNECTIVITY -> Color(0xFF15803D); RecommendationCategory.CONGESTION -> Color(0xFFB45309) }
private fun formatEvidence(value: Double): String = if (value >= 100) "%.0f".format(value) else "%.1f".format(value)
