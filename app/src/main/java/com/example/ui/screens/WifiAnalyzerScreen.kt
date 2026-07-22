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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.WifiWiseViewModel

@Composable
fun WifiAnalyzerScreen(
    viewModel: WifiWiseViewModel
) {
    val liveNetworks by viewModel.liveNetworks.collectAsState()
    var selectedBandIs5G by remember { mutableStateOf(false) }

    val filteredNetworks = liveNetworks.filter {
        if (selectedBandIs5G) it.is5GHz else !it.is5GHz
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("wifi_analyzer_screen")
    ) {
        // Header
        Text(
            text = "WiFi Analyzer",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Analyze channel overlaps and spectral congestion",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Band Selector Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { selectedBandIs5G = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!selectedBandIs5G) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (!selectedBandIs5G) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("band_2.4g_button")
            ) {
                Text("2.4 GHz Band")
            }

            Button(
                onClick = { selectedBandIs5G = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedBandIs5G) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (selectedBandIs5G) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("band_5g_button")
            ) {
                Text("5 GHz Band")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Spectral Distribution Card (Canvas Visualizer)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Signal Distribution & Channel Overlaps",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Custom Graphic Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        // Draw Grid Lines (y-axis representing RSSI from -100 to -30)
                        val gridCount = 5
                        for (i in 0..gridCount) {
                            val y = height * i / gridCount
                            val rssiVal = -30 - (i * 70 / gridCount)
                            drawLine(
                                color = onSurfaceVariant.copy(alpha = 0.12f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Draw Channel Axis at the bottom
                        drawLine(
                            color = onSurfaceVariant.copy(alpha = 0.5f),
                            start = Offset(0f, height),
                            end = Offset(width, height),
                            strokeWidth = 2.dp.toPx()
                        )

                        // Draw Parabolic curves representing wifi signals in range
                        filteredNetworks.forEachIndexed { index, net ->
                            // Maps channel (e.g. 1..14 or 36..165) to x coordinate
                            val minChannel = if (selectedBandIs5G) 36f else 1f
                            val maxChannel = if (selectedBandIs5G) 165f else 14f
                            val channelRange = maxChannel - minChannel

                            val channelVal = net.channel.toFloat().coerceIn(minChannel, maxChannel)
                            val normalizedCh = (channelVal - minChannel) / channelRange
                            val xCenter = width * 0.1f + (width * 0.8f * normalizedCh)

                            // Parabola base width representing channel bandwidth
                            val paraWidth = if (selectedBandIs5G) width * 0.12f else width * 0.2f

                            // Parabola height maps to RSSI (-100 is bottom, -30 is top)
                            val normalizedRssi = ((net.rssi + 100f) / 70f).coerceIn(0f, 1f)
                            val peakHeight = height * (1f - (normalizedRssi * 0.85f))

                            // Construct parabolic Path
                            val path = Path().apply {
                                moveTo(xCenter - paraWidth, height)
                                cubicTo(
                                    xCenter - paraWidth / 2, height,
                                    xCenter - paraWidth / 2, peakHeight,
                                    xCenter, peakHeight
                                )
                                cubicTo(
                                    xCenter + paraWidth / 2, peakHeight,
                                    xCenter + paraWidth / 2, height,
                                    xCenter + paraWidth, height
                                )
                            }

                            val signalColors = listOf(primaryColor, secondaryColor, tertiaryColor)
                            val color = signalColors[index % signalColors.size]

                            // Draw Fill
                            drawPath(
                                path = path,
                                color = color.copy(alpha = 0.15f)
                            )

                            // Draw Stroke
                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // X-Axis Channel Markers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (selectedBandIs5G) {
                        listOf("Ch 36", "Ch 48", "Ch 64", "Ch 100", "Ch 149", "Ch 165").forEach {
                            Text(text = it, fontSize = 10.sp, color = onSurfaceVariant)
                        }
                    } else {
                        listOf("Ch 1", "Ch 3", "Ch 6", "Ch 9", "Ch 11", "Ch 13", "Ch 14").forEach {
                            Text(text = it, fontSize = 10.sp, color = onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interference recommendation card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Spectrum Health Assessment",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                val isCongested = filteredNetworks.size > 3
                Text(
                    text = if (isCongested) {
                        "High Spectral Congestion detected. Multiple access points are operating on overlapping channels. We highly recommend moving to 5GHz or selecting an orthogonal channel (Ch 1, 6, 11) for maximum data throughput."
                    } else {
                        "Minimal spectral congestion in this area. Signal pathways are healthy and free from major co-channel interference. Excellent conditions for low-latency streaming and gaming."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
