package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.WifiNetwork
import com.example.ui.viewmodel.WifiWiseViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun WifiScannerScreen(
    viewModel: WifiWiseViewModel
) {
    val nearbyNetworks by viewModel.nearbyNetworks.collectAsState()
    val savedNetworks by viewModel.savedNetworks.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val lastScanTimestamp by viewModel.lastNearbyScanTimestamp.collectAsState()
    val scanError by viewModel.nearbyScanError.collectAsState()
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }

    val networkGroups = remember(nearbyNetworks) {
        nearbyNetworks
            .groupBy { network -> network.ssid.ifBlank { "Hidden Network" } }
            .map { (ssid, networks) ->
                WifiNetworkGroup(
                    ssid = ssid,
                    accessPoints = networks.sortedWith(
                        compareByDescending<WifiNetwork> { it.isConnected }
                            .thenByDescending { it.rssi }
                    )
                )
            }
            .sortedWith(
                compareByDescending<WifiNetworkGroup> { it.isConnected }
                    .thenByDescending { it.strongestRssi }
            )
    }
    val savedBssids = remember(savedNetworks) {
        savedNetworks.map { it.bssid }.toSet()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scan_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("wifi_scanner_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WiFi Scanner",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${nearbyNetworks.size} access points · ${networkGroups.size} networks",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    text = lastScanTimestamp?.let { timestamp ->
                        "Last scan: ${
                            DateFormat.getTimeInstance(DateFormat.MEDIUM)
                                .format(Date(timestamp))
                        }"
                    } ?: "No completed scan yet",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            Button(
                onClick = viewModel::triggerManualScan,
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("scan_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Autorenew,
                    contentDescription = "Scan Icon",
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(if (isScanning) rotationAngle else 0f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isScanning) "Scanning..." else "Scan")
            }
        }

        if (isScanning) {
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isScanning && nearbyNetworks.isEmpty() -> ScannerLoadingState()
            scanError != null && nearbyNetworks.isEmpty() -> ScannerErrorState(
                message = scanError.orEmpty(),
                onRetry = viewModel::triggerManualScan
            )
            nearbyNetworks.isEmpty() -> ScannerEmptyState()
            else -> {
                Column(modifier = Modifier.weight(1f)) {
                    scanError?.let { error ->
                        ScannerInlineError(
                            message = error,
                            onRetry = viewModel::triggerManualScan
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(networkGroups, key = { it.ssid }) { group ->
                            val expanded = expandedGroups[group.ssid] == true
                            WifiNetworkGroupCard(
                                group = group,
                                expanded = expanded,
                                savedBssids = savedBssids,
                                onToggleExpanded = {
                                    expandedGroups[group.ssid] = !expanded
                                },
                                onToggleSave = viewModel::toggleSaveNetwork
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiNetworkGroupCard(
    group: WifiNetworkGroup,
    expanded: Boolean,
    savedBssids: Set<String>,
    onToggleExpanded: () -> Unit,
    onToggleSave: (WifiNetwork) -> Unit
) {
    val representative = group.representative
    val signalQuality = signalQualityPercentage(representative.rssi)
    val representativeSaved = representative.bssid in savedBssids

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (group.isConnected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (group.isConnected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SignalIcon(
                signalQuality = signalQuality,
                isConnected = group.isConnected
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = group.ssid,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (group.isConnected) {
                        ConnectedBadge()
                    }
                }
                Text(
                    text = "${group.accessPoints.size} access point${
                        if (group.accessPoints.size == 1) "" else "s"
                    } · $signalQuality% signal",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${representative.rssi} dBm · Ch ${representative.channel} · ${
                        bandLabel(representative.frequency)
                    }",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { onToggleSave(representative) }) {
                Icon(
                    imageVector = if (representativeSaved) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Filled.FavoriteBorder
                    },
                    contentDescription = if (representativeSaved) {
                        "Remove access point from favorites"
                    } else {
                        "Add access point to favorites"
                    },
                    tint = if (representativeSaved) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse access points" else "Expand access points",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 10.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                HorizontalDivider()
                group.accessPoints.forEach { network ->
                    AccessPointRow(
                        network = network,
                        isSaved = network.bssid in savedBssids,
                        onToggleSave = { onToggleSave(network) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AccessPointRow(
    network: WifiNetwork,
    isSaved: Boolean,
    onToggleSave: () -> Unit
) {
    val signalQuality = signalQualityPercentage(network.rssi)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = network.bssid,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (network.isConnected) {
                    ConnectedBadge()
                }
            }
            Text(
                text = "RSSI ${network.rssi} dBm ($signalQuality%) · Ch ${network.channel} · ${
                    bandLabel(network.frequency)
                }",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Security: ${network.securityType}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onToggleSave) {
            Icon(
                imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isSaved) "Remove from favorites" else "Add to favorites",
                tint = if (isSaved) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
        }
    }
}

@Composable
private fun SignalIcon(
    signalQuality: Int,
    isConnected: Boolean
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (isConnected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Wifi,
            contentDescription = "$signalQuality percent signal",
            tint = if (isConnected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = (signalQuality / 100f).coerceIn(0.25f, 1f)
                )
            },
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ConnectedBadge() {
    Spacer(modifier = Modifier.width(6.dp))
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = "Connected",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ScannerLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text("Scanning nearby WiFi networks…")
        }
    }
}

@Composable
private fun ScannerEmptyState() {
    ScannerMessageState(
        title = "No nearby networks found",
        message = "Check that WiFi and Location Services are enabled."
    )
}

@Composable
private fun ScannerErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text("Unable to scan nearby networks", fontWeight = FontWeight.Bold)
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun ScannerInlineError(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun ScannerMessageState(
    title: String,
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Wifi,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class WifiNetworkGroup(
    val ssid: String,
    val accessPoints: List<WifiNetwork>
) {
    val representative: WifiNetwork
        get() = accessPoints.firstOrNull { it.isConnected }
            ?: accessPoints.maxBy { it.rssi }

    val isConnected: Boolean
        get() = accessPoints.any { it.isConnected }

    val strongestRssi: Int
        get() = accessPoints.maxOf { it.rssi }
}

fun signalQualityPercentage(rssi: Int): Int {
    return (2 * (rssi + 100)).coerceIn(0, 100)
}

private fun bandLabel(frequency: Int): String {
    return when {
        frequency >= 5955 -> "6 GHz"
        frequency >= 4900 -> "5 GHz"
        else -> "2.4 GHz"
    }
}
