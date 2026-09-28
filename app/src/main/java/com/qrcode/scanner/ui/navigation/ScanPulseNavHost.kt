package com.qrcode.scanner.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.qrcode.scanner.ui.components.ScanPulseBottomBar
import com.qrcode.scanner.ui.screens.common.PlaceholderScreen
import com.qrcode.scanner.ui.screens.create.CreateQrIntents
import com.qrcode.scanner.ui.screens.history.HistoryIntents
import com.qrcode.scanner.ui.screens.home.HomeScreen
import com.qrcode.scanner.ui.screens.scan.ScanIntents
import com.qrcode.scanner.ui.screens.scan.ScanScreen
import com.qrcode.scanner.ui.screens.settings.AboutScreen
import com.qrcode.scanner.ui.screens.settings.AppLanguage
import com.qrcode.scanner.ui.screens.settings.LanguageScreen
import com.qrcode.scanner.ui.screens.settings.SettingsScreen
import com.qrcode.scanner.ui.screens.splash.SplashScreen
import com.qrcode.scanner.ui.theme.PageBackground

private const val MAIN_GRAPH_ROUTE = "main_graph"

@Composable
fun ScanPulseNavHost(
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute.showsBottomNavigation()
    val isScan = currentRoute == AppDestination.Scan.route
    var tabBeforeScan by remember { mutableStateOf(AppDestination.Home.route) }
    LaunchedEffect(currentRoute) {
        if (
            currentRoute != null &&
            currentRoute != AppDestination.Scan.route &&
            currentRoute in rootDestinations
        ) {
            tabBeforeScan = currentRoute
        }
    }

    fun openRootTab(route: String) {
        navController.navigate(route) {
            popUpTo(MAIN_GRAPH_ROUTE) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = if (isScan) Color.Transparent else PageBackground,
        contentColor = Color.Unspecified,
        bottomBar = {
            if (showBottomBar) {
                ScanPulseBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { destination -> openRootTab(destination.route) },
                    onScanClick = { openRootTab(AppDestination.Scan.route) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Splash.route,
            modifier = if (isScan) {
                Modifier.fillMaxSize()
            } else {
                Modifier.padding(innerPadding)
            }
        ) {
            composable(AppDestination.Splash.route) {
                SplashScreen(
                    onFinished = {
                        navController.navigate(MAIN_GRAPH_ROUTE) {
                            popUpTo(AppDestination.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            navigation(
                route = MAIN_GRAPH_ROUTE,
                startDestination = AppDestination.Scan.route
            ) {
                composable(AppDestination.Home.route) {
                    val context = LocalContext.current
                    HomeScreen(
                        onOpenScanner = {
                            navController.navigate(AppDestination.Scan.route) {
                                popUpTo(MAIN_GRAPH_ROUTE) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onScanBarcode = {
                            navController.navigate(AppDestination.Scan.route) {
                                popUpTo(MAIN_GRAPH_ROUTE) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onScanGallery = {
                            context.startActivity(
                                ScanIntents.openGalleryCrop(
                                    context = context,
                                    imageUri = null,
                                    scanMode = ScanIntents.MODE_BATCH
                                )
                            )
                        },
                        onCreateQr = {
                            context.startActivity(CreateQrIntents.openHub(context))
                        },
                        onOpenHistory = {
                            context.startActivity(HistoryIntents.openHistory(context))
                        },
                        onOpenFavorites = {
                            context.startActivity(HistoryIntents.openFavorites(context))
                        },
                        onViewAllHistory = {
                            context.startActivity(HistoryIntents.openHistory(context))
                        },
                        onRecentItemClick = { historyId ->
                            context.startActivity(HistoryIntents.openDetail(context, historyId))
                        }
                    )
                }
                composable(AppDestination.Scan.route) {
                    ScanScreen(
                        onClose = { openRootTab(tabBeforeScan) },
                        safeContentPadding = innerPadding
                    )
                }
                composable(AppDestination.Settings.route) {
                    SettingsScreen(
                        onOpenAbout = {
                            navController.navigate(AppDestination.About.route)
                        },
                        onOpenLanguage = {
                            navController.navigate(AppDestination.Language.route)
                        }
                    )
                }
            }

            // Secondary placeholders (no bottom bar) — Create forms use Activities
            composable(AppDestination.CameraPermission.route) {
                PlaceholderScreen(title = "Camera Permission")
            }
            // Gallery crop / detection error / scan result use Activities (Phase 4–9)
            composable(AppDestination.QrCustomization.route) {
                PlaceholderScreen(title = "QR Customization")
            }
            composable(AppDestination.QrPreviewExport.route) {
                PlaceholderScreen(title = "QR Preview & Export")
            }
            composable(AppDestination.Language.route) {
                LanguageScreen(
                    onBack = { navController.popBackStack() },
                    onApply = { language ->
                        // Return to Settings first so recreation restores Settings, not Language.
                        navController.popBackStack()
                        AppLanguage.apply(language)
                    }
                )
            }
            composable(AppDestination.About.route) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
