package com.example.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RecommendationsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SpeedTestScreen
import com.example.ui.screens.WifiAnalyzerScreen
import com.example.ui.screens.WifiScannerScreen
import com.example.ui.screens.NetworkJourneyScreen
import com.example.ui.screens.WifiHeatmapScreen
import com.example.ui.viewmodel.WifiWiseViewModel

data class BottomNavItem<T : Any>(
    val name: String,
    val route: T,
    val icon: ImageVector,
    val testTag: String
)

private data class DrawerNavItem<T : Any>(
    val name: String,
    val route: T,
    val icon: ImageVector,
    val testTag: String
)

private data class DrawerSection(
    val title: String,
    val items: List<DrawerNavItem<out Any>>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: WifiWiseViewModel
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Define items for the bottom navigation bar (exactly 5)
    val bottomNavItems = listOf(
        BottomNavItem("Dashboard", DashboardDestination, Icons.Rounded.Dashboard, "tab_dashboard"),
        BottomNavItem("Scanner", WifiScannerDestination, Icons.Rounded.Radar, "tab_scanner"),
        BottomNavItem("Analyzer", WifiAnalyzerDestination, Icons.Rounded.NetworkCheck, "tab_analyzer"),
        BottomNavItem("Recommendations", RecommendationsDestination, Icons.Rounded.Security, "tab_recommendations"),
        BottomNavItem("Settings", SettingsDestination, Icons.Rounded.Settings, "tab_settings")
    )

    // Drawer Sections and Items
    val coreItems = listOf(
        DrawerNavItem("Dashboard", DashboardDestination, Icons.Rounded.Dashboard, "drawer_dashboard"),
        DrawerNavItem("Wi-Fi Scanner", WifiScannerDestination, Icons.Rounded.Radar, "drawer_scanner"),
        DrawerNavItem("Network Analyzer", WifiAnalyzerDestination, Icons.Rounded.NetworkCheck, "drawer_analyzer"),
        DrawerNavItem("Recommendations", RecommendationsDestination, Icons.Rounded.Security, "drawer_recommendations")
    )

    val toolItems = listOf(
        DrawerNavItem("Speed Test", SpeedTestDestination, Icons.Rounded.Speed, "drawer_speed_test"),
        DrawerNavItem("Roaming Prediction", AnalyticsDestination, Icons.Rounded.WifiTethering, "drawer_prediction"),
        DrawerNavItem("Network Journey", NetworkJourneyDestination, Icons.Rounded.History, "drawer_journey"),
        DrawerNavItem("Signal Map", WifiHeatmapDestination, Icons.Rounded.Wifi, "drawer_heatmap"),
        DrawerNavItem("Analytics", AnalyticsDestination, Icons.Rounded.Analytics, "drawer_analytics")
    )

    val systemItems = listOf(
        DrawerNavItem("Settings", SettingsDestination, Icons.Rounded.Settings, "drawer_settings")
    )

    val drawerSections = listOf(
        DrawerSection("CORE", coreItems),
        DrawerSection("TOOLS", toolItems),
        DrawerSection("SYSTEM", systemItems)
    )

    // Check if bottom bar / top bar should be visible on the current screen
    val showBars = currentDestination != null &&
            !currentDestination.hasRoute(SplashDestination::class) &&
            !currentDestination.hasRoute(LoginDestination::class)

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showBars,
        drawerContent = {
            if (showBars) {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                    modifier = Modifier.width(310.dp)
                ) {
                    Column(modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
                        DrawerHeader()
                        drawerSections.forEachIndexed { index, section ->
                            if (index > 0) Spacer(Modifier.height(8.dp))
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
                            )
                            section.items.forEach { item ->
                                val isSelected = currentDestination?.hasRoute(item.route::class) == true
                                NavigationDrawerItem(
                                    label = { Text(item.name) },
                                    selected = isSelected,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        if (!isSelected) {
                                            navController.navigate(item.route) {
                                                popUpTo(DashboardDestination) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = { Icon(item.icon, contentDescription = null) },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp).testTag(item.testTag),
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            if (index < drawerSections.size - 1) {
                                Spacer(Modifier.height(16.dp))
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 28.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (showBars) {
                    TopAppBar(
                        title = {
                            Text(
                                text = when {
                                    currentDestination?.hasRoute(DashboardDestination::class) == true -> "NetPulse"
                                    currentDestination?.hasRoute(WifiScannerDestination::class) == true -> "Scanner"
                                    currentDestination?.hasRoute(WifiAnalyzerDestination::class) == true -> "Analyzer"
                                    currentDestination?.hasRoute(RecommendationsDestination::class) == true -> "Recommendations"
                                    currentDestination?.hasRoute(SettingsDestination::class) == true -> "Settings"
                                    currentDestination?.hasRoute(SpeedTestDestination::class) == true -> "Speed Test"
                                    currentDestination?.hasRoute(NetworkJourneyDestination::class) == true -> "Journey"
                                    currentDestination?.hasRoute(WifiHeatmapDestination::class) == true -> "Signal Map"
                                    currentDestination?.hasRoute(AnalyticsDestination::class) == true -> "Analytics"
                                    currentDestination?.hasRoute(HistoryDestination::class) == true -> "History"
                                    else -> "NetPulse"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Rounded.Menu, contentDescription = "Open navigation drawer")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            },
            bottomBar = {
                if (showBars) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        NavigationBar(
                            modifier = Modifier
                                .clip(RoundedCornerShape(28.dp))
                                .shadow(10.dp, RoundedCornerShape(28.dp))
                                .testTag("bottom_nav_bar"),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp
                        ) {
                            bottomNavItems.forEach { item ->
                                val isSelected = currentDestination?.hasRoute(item.route::class) == true
                                val iconScale by animateFloatAsState(
                                    targetValue = if (isSelected) 1f else 0.9f,
                                    label = "bottomNavIconScale"
                                )
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
                                            contentDescription = item.name,
                                            modifier = Modifier.scale(iconScale)
                                        )
                                    },
                                    label = { Text(text = item.name, style = MaterialTheme.typography.labelMedium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
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
                            onNavigateToSpeedTest = { navController.navigate(SpeedTestDestination) },
                            onNavigateToJourney = { navController.navigate(NetworkJourneyDestination) },
                            onNavigateToHeatmap = { navController.navigate(WifiHeatmapDestination) },
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

                    // Secondary feature: deliberately outside primary bottom navigation.
                    composable<SpeedTestDestination> {
                        SpeedTestScreen(viewModel = viewModel)
                    }

                    composable<NetworkJourneyDestination> { NetworkJourneyScreen(viewModel) }
                    composable<WifiHeatmapDestination> { WifiHeatmapScreen(viewModel) }

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
}

@Composable
private fun DrawerHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 32.dp)
    ) {
        Text(
            text = "NETPULSE",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Wi-Fi Intelligence",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
