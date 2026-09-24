package com.qrcode.scanner.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.qrcode.scanner.ui.screens.create.CreateHubScreen
import com.qrcode.scanner.ui.screens.create.CreateQrIntents
import com.qrcode.scanner.ui.screens.history.HistoryIntents
import com.qrcode.scanner.ui.screens.history.HistoryScreen
import com.qrcode.scanner.ui.screens.home.HomeScreen
import com.qrcode.scanner.ui.screens.scan.ScanIntents
import com.qrcode.scanner.ui.screens.scan.ScanScreen
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

    Scaffold(
        containerColor = PageBackground,
        contentColor = Color.Unspecified,
        bottomBar = {
            if (showBottomBar) {
                ScanPulseBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(MAIN_GRAPH_ROUTE) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Splash.route,
            modifier = Modifier.padding(innerPadding)
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
                startDestination = AppDestination.Home.route
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
                            navController.navigate(AppDestination.GalleryCrop.route)
                        },
                        onCreateQr = {
                            navController.navigate(AppDestination.Create.route) {
                                popUpTo(MAIN_GRAPH_ROUTE) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onBatchScanner = {
                            navController.navigate(AppDestination.Scan.route) {
                                popUpTo(MAIN_GRAPH_ROUTE) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onViewAllHistory = {
                            navController.navigate(AppDestination.History.route) {
                                popUpTo(MAIN_GRAPH_ROUTE) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onRecentItemClick = { historyId ->
                            context.startActivity(HistoryIntents.openDetail(context, historyId))
                        }
                    )
                }
                composable(AppDestination.Create.route) {
                    val context = LocalContext.current
                    CreateHubScreen(
                        onCategoryClick = { type ->
                            context.startActivity(CreateQrIntents.openCategory(context, type))
                        }
                    )
                }
                composable(AppDestination.Scan.route) {
                    ScanScreen()
                }
                composable(AppDestination.History.route) {
                    val context = LocalContext.current
                    HistoryScreen(
                        onOpenScanner = {
                            context.startActivity(ScanIntents.openScanner(context))
                        },
                        onOpenDetail = { historyId ->
                            context.startActivity(HistoryIntents.openDetail(context, historyId))
                        }
                    )
                }
                composable(AppDestination.Settings.route) {
                    SettingsScreen()
                }
            }

            // Secondary placeholders (no bottom bar) — Create forms use Activities
            composable(AppDestination.CameraPermission.route) {
                PlaceholderScreen(title = "Camera Permission")
            }
            composable(AppDestination.GalleryCrop.route) {
                PlaceholderScreen(title = "Gallery Crop")
            }
            composable(AppDestination.DetectionError.route) {
                PlaceholderScreen(title = "Detection Error")
            }
            composable(AppDestination.ScanResult.route) {
                PlaceholderScreen(title = "Scan Result / Detail")
            }
            composable(AppDestination.QrCustomization.route) {
                PlaceholderScreen(title = "QR Customization")
            }
            composable(AppDestination.QrPreviewExport.route) {
                PlaceholderScreen(title = "QR Preview & Export")
            }
            composable(AppDestination.HistoryDetail.route) {
                PlaceholderScreen(title = "History Detail")
            }
            composable(AppDestination.Language.route) {
                PlaceholderScreen(title = "Language")
            }
        }
    }
}
