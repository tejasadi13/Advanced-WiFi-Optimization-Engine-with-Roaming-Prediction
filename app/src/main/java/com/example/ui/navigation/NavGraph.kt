package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RecommendationsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.WifiAnalyzerScreen
import com.example.ui.screens.WifiScannerScreen
import com.example.ui.viewmodel.WifiWiseViewModel

data class BottomNavItem<T : Any>(
    val name: String,
    val route: T,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: WifiWiseViewModel
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Define items for the bottom navigation bar
    val bottomNavItems = listOf(
        BottomNavItem("Dashboard", DashboardDestination, Icons.Filled.Dashboard, "tab_dashboard"),
        BottomNavItem("Scanner", WifiScannerDestination, Icons.Filled.Radar, "tab_scanner"),
        BottomNavItem("Analyzer", WifiAnalyzerDestination, Icons.Filled.NetworkCheck, "tab_analyzer"),
        BottomNavItem("Alerts", RecommendationsDestination, Icons.Filled.Security, "tab_recommendations"),
        BottomNavItem("Settings", SettingsDestination, Icons.Filled.Settings, "tab_settings")
    )

    // Check if bottom bar should be visible on the current screen
    val showBottomBar = currentDestination != null &&
            !currentDestination.hasRoute(SplashDestination::class) &&
            !currentDestination.hasRoute(LoginDestination::class)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentDestination?.hasRoute(item.route::class) == true
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    navController.navigate(item.route) {
                                        popUpTo(DashboardDestination) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.name
                                )
                            },
                            label = { Text(text = item.name, fontSize = 11.sp) },
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = SplashDestination
            ) {
                // 1. Splash Screen
                composable<SplashDestination> {
                    SplashScreen(
                        viewModel = viewModel,
                        onNavigateNext = {
                            if (isLoggedIn) {
                                navController.navigate(DashboardDestination) {
                                    popUpTo(SplashDestination) { inclusive = true }
                                }
                            } else {
                                navController.navigate(LoginDestination) {
                                    popUpTo(SplashDestination) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                // 2. Login Screen
                composable<LoginDestination> {
                    LoginScreen(
                        viewModel = viewModel,
                        onLoginSuccess = {
                            navController.navigate(DashboardDestination) {
                                popUpTo(LoginDestination) { inclusive = true }
                            }
                        }
                    )
                }

                // 3. Dashboard Screen
                composable<DashboardDestination> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToScanner = { navController.navigate(WifiScannerDestination) },
                        onNavigateToAnalyzer = { navController.navigate(WifiAnalyzerDestination) },
                        onNavigateToRecommendations = { navController.navigate(RecommendationsDestination) },
                        onNavigateToHistory = { navController.navigate(HistoryDestination) },
                        onNavigateToAnalytics = { navController.navigate(AnalyticsDestination) },
                        onNavigateToSettings = { navController.navigate(SettingsDestination) }
                    )
                }

                // 4. WiFi Scanner
                composable<WifiScannerDestination> {
                    WifiScannerScreen(viewModel = viewModel)
                }

                // 5. WiFi Analyzer
                composable<WifiAnalyzerDestination> {
                    WifiAnalyzerScreen(viewModel = viewModel)
                }

                // 6. Recommendations
                composable<RecommendationsDestination> {
                    RecommendationsScreen(viewModel = viewModel)
                }

                // 7. History (details route)
                composable<HistoryDestination> {
                    HistoryScreen(viewModel = viewModel)
                }

                // 8. Analytics (details route)
                composable<AnalyticsDestination> {
                    AnalyticsScreen(viewModel = viewModel)
                }

                // 9. Settings
                composable<SettingsDestination> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onLogout = {
                            navController.navigate(LoginDestination) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
