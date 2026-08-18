package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.SpeedTestPhase
import com.example.domain.model.SpeedTestResult
import com.example.ui.viewmodel.WifiWiseViewModel
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun SpeedTestScreen(viewModel: WifiWiseViewModel) {
    val state by viewModel.speedTestState.collectAsState()
    val history by viewModel.speedTestHistory.collectAsState()
    val context = LocalContext.current
    val isRunning = state.phase in setOf(SpeedTestPhase.CONNECTING, SpeedTestPhase.PING, SpeedTestPhase.DOWNLOAD, SpeedTestPhase.UPLOAD)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Measure real connection performance over your current Wi-Fi network", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SpeedGauge(
                value = state.downloadMbps,
                phase = state.phase,
                progress = state.progress,
                networkName = state.networkName
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = viewModel::startSpeedTest,
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.Speed, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isRunning) "Testing…" else "Start test")
                }
                if (state.result != null) {
                    OutlinedButton(
                        onClick = {
                            val result = state.result ?: return@OutlinedButton
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, result.asShareText())
                            }, "Share speed test"))
                        },
                        modifier = Modifier.height(54.dp),
                        shape = RoundedCornerShape(17.dp)
                    ) { Icon(Icons.Rounded.IosShare, "Share") }
                }
            }
        }
        item {
            AnimatedVisibility(visible = state.phase == SpeedTestPhase.FAILED) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(16.dp)) {
                    Text(state.errorMessage ?: "Speed test failed.", color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(16.dp))
                }
            }
        }
        if (state.result != null) {
            item { ResultCard(state.result!!) }
        }
        item {
            Text("Test history", style = MaterialTheme.typography.titleLarge)
        }
        if (history.isEmpty()) {
            item { Text("Completed tests will appear here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(history, key = { it.timestamp }) { HistoryRow(it) }
        }
    }
}

@Composable
private fun SpeedGauge(value: Double?, phase: SpeedTestPhase, progress: Float, networkName: String?) {
    val displayValue = value?.toFloat() ?: 0f
    val needle by animateFloatAsState(displayValue.coerceIn(0f, 1000f) / 1000f, label = "speed_needle")
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Box(Modifier.size(250.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 18.dp.toPx()
                    val diameter = size.minDimension - stroke
                    val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                    drawArc(trackColor, 135f, 270f, false, topLeft, Size(diameter, diameter), style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(primaryColor, 135f, 270f * needle, false, topLeft, Size(diameter, diameter), style = Stroke(stroke, cap = StrokeCap.Round))
                    val angle = Math.toRadians((135 + 270 * needle).toDouble())
                    val radius = diameter / 2 - stroke
                    val center = Offset(size.width / 2, size.height / 2)
                    val end = Offset(center.x + kotlin.math.cos(angle).toFloat() * radius, center.y + kotlin.math.sin(angle).toFloat() * radius)
                    drawLine(primaryColor, center, end, 5.dp.toPx(), StrokeCap.Round)
                    drawCircle(primaryColor, 9.dp.toPx(), center)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (value != null) String.format("%.1f", value) else "—", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text("Mbps", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(phaseLabel(phase), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            if (networkName != null) Text(networkName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (phase != SpeedTestPhase.IDLE && phase != SpeedTestPhase.COMPLETED && phase != SpeedTestPhase.FAILED) {
                Spacer(Modifier.height(8.dp))
                CircularProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun ResultCard(result: SpeedTestResult) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)), modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Wifi, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("${rating(result.downloadMbps, result.uploadMbps)} connection", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(result.timestamp)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Metric("Download", "${format(result.downloadMbps)} Mbps", Modifier.weight(1f))
                Metric("Upload", "${format(result.uploadMbps)} Mbps", Modifier.weight(1f))
                Metric("Ping", "${format(result.pingMs)} ms", Modifier.weight(1f))
                Metric("Jitter", "${format(result.jitterMs)} ms", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Text("Duration ${result.durationMs / 1000.0}s  ·  ${result.networkName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}

@Composable
private fun HistoryRow(result: SpeedTestResult) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(result.networkName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(result.timestamp)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${format(result.downloadMbps)} / ${format(result.uploadMbps)} Mbps", style = MaterialTheme.typography.labelLarge)
                Text("${format(result.pingMs)} ms ping", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun phaseLabel(phase: SpeedTestPhase): String = when (phase) {
    SpeedTestPhase.IDLE -> "Ready to test"
    SpeedTestPhase.CONNECTING -> "Connecting"
    SpeedTestPhase.PING -> "Ping"
    SpeedTestPhase.DOWNLOAD -> "Download"
    SpeedTestPhase.UPLOAD -> "Upload"
    SpeedTestPhase.COMPLETED -> "Completed"
    SpeedTestPhase.FAILED -> "Test unavailable"
}

private fun format(value: Double): String = if (value >= 100) value.roundToInt().toString() else String.format("%.1f", value)

private fun rating(download: Double, upload: Double): String = when {
    download >= 100 && upload >= 20 -> "Excellent"
    download >= 25 && upload >= 5 -> "Good"
    download >= 5 && upload >= 1 -> "Average"
    else -> "Poor"
}

private fun SpeedTestResult.asShareText(): String = buildString {
    appendLine("NetPulse Internet Speed Test")
    appendLine("Network: $networkName")
    appendLine("Download: ${format(downloadMbps)} Mbps")
    appendLine("Upload: ${format(uploadMbps)} Mbps")
    appendLine("Ping: ${format(pingMs)} ms")
    appendLine("Jitter: ${format(jitterMs)} ms")
    appendLine("Duration: ${durationMs / 1000.0}s")
    appendLine("Tested: ${DateFormat.getDateTimeInstance().format(Date(timestamp))}")
}
