package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.WifiRecommendation
import com.example.ui.viewmodel.WifiWiseViewModel

@Composable
fun RecommendationsScreen(
    viewModel: WifiWiseViewModel
) {
    val recommendations by viewModel.recommendations.collectAsState()
    val pendingRecs = recommendations.filter { !it.isApplied }
    val appliedRecs = recommendations.filter { it.isApplied }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("recommendations_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Text(
                text = "WiFi Optimization",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Dynamic tuning recommended by WiFiWise Engine",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // Active Tasks Section
        if (pendingRecs.isNotEmpty()) {
            item {
                Text(
                    text = "Pending Actions (${pendingRecs.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(pendingRecs) { rec ->
                RecommendationCard(
                    recommendation = rec,
                    onApply = { viewModel.applyRecommendation(rec.id) }
                )
            }
        } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Success Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Spectrum Fully Optimized",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "No pending WiFi health recommendations. Your signal paths are running smoothly.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // History of Applied Targets
        if (appliedRecs.isNotEmpty()) {
            item {
                Text(
                    text = "Applied Optimizations (${appliedRecs.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }

            items(appliedRecs) { rec ->
                RecommendationCard(
                    recommendation = rec,
                    onApply = {}
                )
            }
        }
    }
}

@Composable
fun RecommendationCard(
    recommendation: WifiRecommendation,
    onApply: () -> Unit
) {
    val cardBgColor by animateColorAsState(
        targetValue = if (recommendation.isApplied) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surface,
        label = "color"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(getCategoryBg(recommendation.category)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(recommendation.category),
                            contentDescription = "Category Icon",
                            tint = getCategoryColor(recommendation.category),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = recommendation.category.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = getCategoryColor(recommendation.category)
                    )
                }

                // Priority Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(getPriorityBg(recommendation.priority))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = recommendation.priority.name,
                        color = getPriorityColor(recommendation.priority),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = recommendation.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (recommendation.isApplied) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = recommendation.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (recommendation.isApplied) 0.5f else 1.0f),
                lineHeight = 16.sp
            )

            if (!recommendation.isApplied) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text(text = "Apply Tune Optimization", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Success Check",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Optimization Active",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun getCategoryIcon(category: WifiRecommendation.Category): ImageVector {
    return when (category) {
        WifiRecommendation.Category.SECURITY -> Icons.Filled.Security
        WifiRecommendation.Category.PERFORMANCE -> Icons.Filled.Speed
        WifiRecommendation.Category.COVERAGE -> Icons.Filled.NetworkCheck
    }
}

@Composable
fun getCategoryColor(category: WifiRecommendation.Category): Color {
    return when (category) {
        WifiRecommendation.Category.SECURITY -> MaterialTheme.colorScheme.error
        WifiRecommendation.Category.PERFORMANCE -> MaterialTheme.colorScheme.primary
        WifiRecommendation.Category.COVERAGE -> MaterialTheme.colorScheme.secondary
    }
}

@Composable
fun getCategoryBg(category: WifiRecommendation.Category): Color {
    return getCategoryColor(category).copy(alpha = 0.12f)
}

@Composable
fun getPriorityColor(priority: WifiRecommendation.Priority): Color {
    return when (priority) {
        WifiRecommendation.Priority.HIGH -> MaterialTheme.colorScheme.error
        WifiRecommendation.Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary
        WifiRecommendation.Priority.LOW -> MaterialTheme.colorScheme.secondary
    }
}

@Composable
fun getPriorityBg(priority: WifiRecommendation.Priority): Color {
    return getPriorityColor(priority).copy(alpha = 0.12f)
}
