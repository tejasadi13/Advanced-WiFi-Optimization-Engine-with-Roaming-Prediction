package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.WifiNetwork
import com.example.ui.theme.NetPulseSuccess
import com.example.ui.theme.NetPulseWarning
import com.example.ui.viewmodel.WifiWiseViewModel
import java.text.DateFormat
import java.util.Date

private enum class ScannerSort(val label: String) {
    STRONGEST("Strongest"),
    ALPHABETICAL("Alphabetical"),
    FIVE_G_FIRST("5 GHz First"),
    CONNECTED_FIRST("Connected First")
}

@Composable
fun WifiScannerScreen(viewModel: WifiWiseViewModel) {
    val nearbyNetworks by viewModel.nearbyNetworks.collectAsState()
    val savedNetworks by viewModel.savedNetworks.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val lastScanTimestamp by viewModel.lastNearbyScanTimestamp.collectAsState()
    val scanError by viewModel.nearbyScanError.collectAsState()
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }
    var selectedSort by remember { mutableStateOf(ScannerSort.STRONGEST) }

    val networkGroups = remember(nearbyNetworks, selectedSort) {
        nearbyNetworks
            .groupBy { it.ssid.ifBlank { "Hidden Network" } }
            .map { (ssid, networks) ->
                WifiNetworkGroup(ssid, networks.sortedWith(compareByDescending<WifiNetwork> { it.isConnected }.thenByDescending { it.rssi }))
            }
            .sortedWith(
                when (selectedSort) {
                    ScannerSort.STRONGEST -> compareByDescending<WifiNetworkGroup> { it.isConnected }.thenByDescending { it.strongestRssi }
                    ScannerSort.ALPHABETICAL -> compareBy { it.ssid.lowercase() }
                    ScannerSort.FIVE_G_FIRST -> compareByDescending<WifiNetworkGroup> { it.isFiveOrSixGHz }.thenByDescending { it.strongestRssi }
                    ScannerSort.CONNECTED_FIRST -> compareByDescending<WifiNetworkGroup> { it.isConnected }.thenByDescending { it.strongestRssi }
                }
            )
    }
    val savedBssids = remember(savedNetworks) { savedNetworks.map { it.bssid }.toSet() }
    val bestNetwork = nearbyNetworks.maxByOrNull { it.rssi }
    val rotation by rememberInfiniteTransition(label = "scanner_rotation").animateFloat(
        0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "scanner_rotation_angle"
    )

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp).testTag("wifi_scanner_screen")
    ) {
        ScannerHeader(
            networkCount = networkGroups.size,
            connected = nearbyNetworks.count { it.isConnected },
            timestamp = lastScanTimestamp,
            isScanning = isScanning,
            rotation = rotation,
            onScan = viewModel::triggerManualScan
        )

        if (isScanning) {
            ScannerLoadingStrip()
        }

        Spacer(Modifier.height(14.dp))
        when {
            isScanning && nearbyNetworks.isEmpty() -> ScannerLoadingState()
            scanError != null && nearbyNetworks.isEmpty() -> ScannerErrorState(scanError.orEmpty(), viewModel::triggerManualScan)
            nearbyNetworks.isEmpty() -> ScannerEmptyState(viewModel::triggerManualScan)
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    item { BestNetworkBanner(bestNetwork) }
                    item { SortControls(selectedSort) { selectedSort = it } }
                    scanError?.let { error -> item { ScannerInlineError(error, viewModel::triggerManualScan) } }
                    items(networkGroups, key = { it.ssid }) { group ->
                        WifiNetworkGroupCard(
                            group = group,
                            expanded = expandedGroups[group.ssid] == true,
                            savedBssids = savedBssids,
                            onToggleExpanded = { expandedGroups[group.ssid] = expandedGroups[group.ssid] != true },
                            onToggleSave = viewModel::toggleSaveNetwork
                        )
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ScannerHeader(networkCount: Int, connected: Int, timestamp: Long?, isScanning: Boolean, rotation: Float, onScan: () -> Unit) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            if (isScanning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScannerRadarAnimation(isScanning = true, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Actively scanning...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(4.dp))
            }
            Text("Real-time Wi-Fi discovery", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                ScannerNetworkCountChip(networkCount, isScanning)
                ScannerStatChip("$connected", "Connected")
                ScannerStatChip(timestamp?.let { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it)) } ?: "—", "Last scan")
            }
        }
        Spacer(Modifier.width(10.dp))
        Button(
            onClick = onScan,
            enabled = !isScanning,
            shape = RoundedCornerShape(16.dp),
            contentPadding = ButtonDefaults.ContentPadding,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.testTag("scan_button")
        ) {
            Icon(Icons.Rounded.Autorenew, "Scan", modifier = Modifier.size(19.dp).rotate(if (isScanning) rotation else 0f))
            Spacer(Modifier.width(6.dp))
            Text(if (isScanning) "Scanning" else "Scan")
        }
    }
}

@Composable
private fun ScannerStatChip(value: String, label: String) {
    AssistChip(
        onClick = {},
        label = { Text("$value  $label", style = MaterialTheme.typography.labelMedium, maxLines = 1) },
        shape = RoundedCornerShape(10.dp),
        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun ScannerLoadingStrip() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScannerRadarAnimation(isScanning = true, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Scanning nearby networks...", style = MaterialTheme.typography.labelLarge)
            Text("Please wait...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BestNetworkBanner(network: WifiNetwork?) {
    if (network == null) return
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
            ).padding(18.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Speed, null, tint = Color.White, modifier = Modifier.size(27.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("★  Best Available Network", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
                Text(network.ssid.ifBlank { "Hidden Network" }, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${network.rssi} dBm  ·  ${bandLabel(network.frequency)}  ·  ${network.securityType}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.82f))
                Text(if (network.isConnected) "Connected choice" else "Strongest nearby signal", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.78f))
            }
            Icon(Icons.Rounded.ArrowForward, null, tint = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun SortControls(selected: ScannerSort, onSelect: (ScannerSort) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        ScannerSort.values().forEach { sort ->
            FilterChip(
                selected = selected == sort,
                onClick = { onSelect(sort) },
                label = { Text(sort.label, style = MaterialTheme.typography.labelMedium) },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    }
}

@Composable
private fun WifiNetworkGroupCard(group: WifiNetworkGroup, expanded: Boolean, savedBssids: Set<String>, onToggleExpanded: () -> Unit, onToggleSave: (WifiNetwork) -> Unit) {
    val representative = group.representative
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = spring()).clickable(onClick = onToggleExpanded),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = if (group.isConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f) else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SignalBars(representative.rssi, group.isConnected)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(group.ssid, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (group.isConnected) ConnectedBadge()
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NetworkChip(bandLabel(representative.frequency), MaterialTheme.colorScheme.primary)
                        NetworkChip(representative.securityType, MaterialTheme.colorScheme.secondary)
                    }
                }
                IconButton(onClick = { onToggleSave(representative) }) {
                    Icon(if (representative.bssid in savedBssids) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (representative.bssid in savedBssids) "Remove favorite" else "Add favorite", tint = if (representative.bssid in savedBssids) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Rounded.ExpandMore, if (expanded) "Collapse" else "Expand", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.rotate(if (expanded) 180f else 0f))
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${representative.rssi} dBm", style = MaterialTheme.typography.labelLarge, color = signalColor(representative.rssi))
                Spacer(Modifier.width(8.dp))
                Text("${group.accessPoints.size} access point${if (group.accessPoints.size == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 14.dp)) {
                    group.accessPoints.forEach { network ->
                        AccessPointDetails(network, network.bssid in savedBssids) { onToggleSave(network) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessPointDetails(network: WifiNetwork, isSaved: Boolean, onToggleSave: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Wifi, null, tint = signalColor(network.rssi), modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(8.dp))
            Text(network.bssid, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onToggleSave, modifier = Modifier.size(32.dp)) {
                Icon(if (isSaved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, tint = if (isSaved) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            DetailValue("RSSI", "${network.rssi} dBm", Modifier.weight(1f))
            DetailValue("Frequency", "${network.frequency} MHz", Modifier.weight(1f))
            DetailValue("Channel", network.channel.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            if (network.isConnected) DetailValue("Link speed", "${network.estimatedSpeedMbps} Mbps", Modifier.weight(1f))
            DetailValue("Security", network.securityType, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DetailValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SignalBars(rssi: Int, connected: Boolean) {
    val quality = signalQualityPercentage(rssi)
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.size(width = 38.dp, height = 31.dp)) {
        repeat(5) { index ->
            val filled = quality >= (index + 1) * 20
            val animatedAlpha by animateFloatAsState(if (filled) 1f else 0.18f, animationSpec = tween(350), label = "signal_bar_alpha")
            Box(Modifier.width(5.dp).height((9 + index * 4).dp).clip(RoundedCornerShape(4.dp)).background(if (connected) MaterialTheme.colorScheme.primary.copy(alpha = animatedAlpha) else signalColor(rssi).copy(alpha = animatedAlpha)))
        }
    }
}

@Composable
private fun NetworkChip(label: String, color: Color) {
    AssistChip(onClick = {}, label = { Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1) }, shape = RoundedCornerShape(8.dp), colors = AssistChipDefaults.assistChipColors(containerColor = color.copy(alpha = 0.12f), labelColor = color), modifier = Modifier.height(28.dp))
}

@Composable
private fun ConnectedBadge() {
    Spacer(Modifier.width(6.dp))
    Box(Modifier.clip(RoundedCornerShape(7.dp)).background(NetPulseSuccess.copy(alpha = 0.14f)).padding(horizontal = 6.dp, vertical = 3.dp)) {
        Text("Connected", color = NetPulseSuccess, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ScannerLoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ScannerLoadingStrip()
            Spacer(Modifier.height(20.dp))
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ScannerEmptyState(onScan: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Box(Modifier.size(94.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Wifi, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(50.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text("No Networks Found", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text("Tap Scan to search nearby WiFi networks.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onScan, shape = RoundedCornerShape(16.dp)) { Text("Scan Again") }
        }
    }
}

@Composable
private fun ScannerErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Rounded.Wifi, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("Unable to scan nearby networks", style = MaterialTheme.typography.titleMedium)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onRetry, shape = RoundedCornerShape(16.dp)) { Text("Retry") }
        }
    }
}

@Composable
private fun ScannerInlineError(message: String, onRetry: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Button(onClick = onRetry, shape = RoundedCornerShape(12.dp)) { Text("Retry") }
        }
    }
}

private data class WifiNetworkGroup(val ssid: String, val accessPoints: List<WifiNetwork>) {
    val representative: WifiNetwork get() = accessPoints.firstOrNull { it.isConnected } ?: accessPoints.maxBy { it.rssi }
    val isConnected: Boolean get() = accessPoints.any { it.isConnected }
    val strongestRssi: Int get() = accessPoints.maxOf { it.rssi }
    val isFiveOrSixGHz: Boolean get() = accessPoints.any { it.frequency >= 4900 }
}

fun signalQualityPercentage(rssi: Int): Int = (2 * (rssi + 100)).coerceIn(0, 100)

@Composable
private fun signalColor(rssi: Int): Color = when {
    rssi >= -50 -> NetPulseSuccess
    rssi >= -65 -> MaterialTheme.colorScheme.primary
    rssi >= -80 -> NetPulseWarning
    else -> MaterialTheme.colorScheme.error
}

@Composable
private fun ScannerRadarAnimation(
    isScanning: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_radar")

    val ring1Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1_progress"
    )

    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1_alpha"
    )

    val ring2Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing, delayMillis = 1250),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2_progress"
    )

    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing, delayMillis = 1250),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2_alpha"
    )

    val centerPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "center_pulse"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (isScanning) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = size.center
                val maxRadius = size.minDimension / 2
                val minRadius = maxRadius * 0.4f

                drawCircle(
                    color = color.copy(alpha = ring1Alpha),
                    radius = minRadius + (ring1Progress * (maxRadius - minRadius)),
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                drawCircle(
                    color = color.copy(alpha = ring2Alpha),
                    radius = minRadius + (ring2Progress * (maxRadius - minRadius)),
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        Icon(
            imageVector = Icons.Rounded.Wifi,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .fillMaxSize(0.5f)
                .scale(if (isScanning) centerPulse else 1f)
        )
    }
}

@Composable
private fun ScannerNetworkCountChip(count: Int, isScanning: Boolean) {
    val backgroundColor by animateColorAsState(
        if (isScanning) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "chip_bg"
    )

    AssistChip(
        onClick = {},
        label = {
            AnimatedContent(
                targetState = isScanning,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.8f, animationSpec = tween(400)))
                        .togetherWith(fadeOut(animationSpec = tween(200)))
                },
                label = "count_chip_content"
            ) { scanning ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (scanning) {
                        ScannerRadarAnimation(isScanning = true, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Searching...", style = MaterialTheme.typography.labelMedium)
                    } else {
                        Text("$count Networks", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        shape = RoundedCornerShape(10.dp),
        colors = AssistChipDefaults.assistChipColors(containerColor = backgroundColor),
        modifier = Modifier.animateContentSize()
    )
}

private fun bandLabel(frequency: Int): String = when {
    frequency >= 5955 -> "6 GHz"
    frequency >= 4900 -> "5 GHz"
    else -> "2.4 GHz"
}
