package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.design.NetPulseCard
import com.example.ui.design.NetPulsePrimaryButton
import com.example.ui.design.NetPulseSecondaryButton
import com.example.ui.design.NetPulseSectionHeader
import com.example.ui.viewmodel.WifiWiseViewModel

@Composable
fun SettingsScreen(viewModel: WifiWiseViewModel, onLogout: () -> Unit) {
    val roamingEnabled by viewModel.roamingEngineEnabled.collectAsState()
    val backgroundEnabled by viewModel.autoOptimizeEnabled.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp).testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(12.dp)) }
        item {
            SettingsSection("Account") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Person, "Account", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("NetPulse account", style = MaterialTheme.typography.titleMedium)
                        Text(userEmail.ifBlank { "Not signed in" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    NetPulseSecondaryButton("Log out", onClick = { viewModel.logout(); onLogout() }, modifier = Modifier.height(42.dp).testTag("logout_button"), icon = Icons.Rounded.Logout)
                }
            }
        }
        item {
            SettingsSection("Prediction engine") {
                SettingToggleRow(
                    title = "Roaming recommendations",
                    description = "Shows advisory signal-degradation and candidate access-point guidance from observed Wi-Fi data.",
                    checked = roamingEnabled,
                    onCheckedChange = viewModel::toggleRoamingEngine,
                    testTag = "roaming_engine_toggle"
                )
            }
        }
        item {
            SettingsSection("Background optimization") {
                SettingToggleRow(
                    title = "Background observations",
                    description = "Enables the existing background scheduler preference. NetPulse does not automatically change Wi-Fi networks.",
                    checked = backgroundEnabled,
                    onCheckedChange = viewModel::toggleAutoOptimize,
                    testTag = "auto_optimize_toggle"
                )
            }
        }
        item {
            SettingsSection("Data & storage") {
                Text("Local scan and speed-test history is stored on this device using Room.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                NetPulseSecondaryButton("Clear scan history", viewModel::clearScanHistory, modifier = Modifier.fillMaxWidth().testTag("clear_history_button"), icon = Icons.Rounded.DeleteSweep)
            }
        }
        item {
            SettingsSection("Appearance") {
                Text("NetPulse uses a light Material 3 appearance by default. Dark theme support is available in the shared design system.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SettingsSection("About NetPulse") {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Info, "About", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Professional Wi-Fi discovery, analysis, speed testing, and advisory roaming intelligence. All network observations remain client-side.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Spacer(Modifier.height(14.dp)) }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    NetPulseCard {
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
fun SettingToggleRow(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, testTag: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(3.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.testTag(testTag), colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
    }
}
