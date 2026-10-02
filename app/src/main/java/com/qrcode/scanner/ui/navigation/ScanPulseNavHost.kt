package com.qrcode.scanner.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.qrcode.scanner.launcher.activities.LanguageActivity
import com.qrcode.scanner.launcher.activities.LauncherHomeActivity
import com.qrcode.scanner.launcher.common.ScreenInterAds
import com.qrcode.scanner.launcher.common.StartupFlow
import com.qrcode.scanner.launcher.common.StartupNavigation
import com.qrcode.scanner.launcher.common.WidgetNavigation
import com.qrcode.scanner.launcher.fragments.LauncherQrSystemBars
import com.qrcode.scanner.ui.components.HomeBottomAdSlot
import com.qrcode.scanner.ui.components.ScanPulseBottomBar
import com.qrcode.scanner.ui.components.ScreenWithAd
import com.qrcode.scanner.ui.components.runWithClickAd
import com.qrcode.scanner.ui.screens.common.PlaceholderScreen
import com.qrcode.scanner.ui.screens.create.CreateQrIntents
import com.qrcode.scanner.ui.screens.history.HistoryIntents
import com.qrcode.scanner.ui.screens.home.HomeScreen
import com.qrcode.scanner.ui.screens.scan.ScanIntents
import com.qrcode.scanner.ui.screens.scan.ScanScreen
import com.qrcode.scanner.ui.screens.settings.AboutScreen
import com.qrcode.scanner.ui.screens.settings.SettingsScreen
import com.qrcode.scanner.ui.screens.splash.SplashScreen
import com.qrcode.scanner.ui.theme.CardSurface
import com.qrcode.scanner.ui.theme.PageBackground
import com.qrcode.scanner.ui.theme.ScanPulsePalette
import com.qrcode.scanner.ui.theme.ScanPulseThemeState

private const val MAIN_GRAPH_ROUTE = "main_graph"

private fun Context.findHostActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }
        current = current.baseContext
    }
    return null
}

@Composable
fun ScanPulseNavHost(
    showStartupSplash: Boolean = true,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute.showsBottomNavigation()
    val isScan = currentRoute == AppDestination.Scan.route
    val matchHeaderStatus = currentRoute != null &&
        currentRoute != AppDestination.Scan.route &&
        currentRoute != AppDestination.Splash.route
    val hostActivity = LocalContext.current.findHostActivity()
    val useDark = ScanPulseThemeState.palette == ScanPulsePalette.Dark
    SideEffect {
        val launcher = hostActivity as? LauncherHomeActivity ?: return@SideEffect
        if (launcher.launcherCurrentItem != com.qrcode.scanner.launcher.adapters.LauncherPagerAdapter.PAGE_RIGHT) {
            return@SideEffect
        }
        if (isScan) {
            LauncherQrSystemBars.applyScanStatusBar(launcher)
        } else if (matchHeaderStatus) {
            LauncherQrSystemBars.applyHeaderStatusBar(launcher, useDark)
        }
    }
    val inLauncher = hostActivity is LauncherHomeActivity
    val reopenSettings = remember { ThemeNavigation.consumeReopenSettings() }
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

    LaunchedEffect(reopenSettings) {
        if (reopenSettings) {
            openRootTab(AppDestination.Settings.route)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, navController) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && ComposeScanRequest.consume()) {
                openRootTab(AppDestination.Scan.route)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun openRootTabWithAd(route: String) {
        val activity = hostActivity
        if (activity == null) {
            openRootTab(route)
            return
        }
        ScreenInterAds.onBottomNav(activity, Runnable { openRootTab(route) })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = if (isScan) Color.Transparent else PageBackground,
            contentColor = Color.Unspecified,
            contentWindowInsets = if (inLauncher) WindowInsets.statusBars else WindowInsets.systemBars,
            bottomBar = {
                if (showBottomBar) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ScanPulseBottomBar(
                            currentRoute = currentRoute,
                            onNavigate = { destination -> openRootTabWithAd(destination.route) },
                            onScanClick = { openRootTabWithAd(AppDestination.Scan.route) },
                            includeNavigationBarPadding = !inLauncher
                        )
                        HomeBottomAdSlot()
                    }
                }
            }
        ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (showStartupSplash) {
                AppDestination.Splash.route
            } else {
                MAIN_GRAPH_ROUTE
            },
            modifier = if (isScan) {
                Modifier.fillMaxSize()
            } else {
                Modifier.padding(innerPadding)
            }
        ) {
            composable(AppDestination.Splash.route) {
                val context = LocalContext.current
                var adRoot by remember { mutableStateOf<View?>(null) }
                SplashScreen(onAdRootReady = { adRoot = it })
                LaunchedEffect(adRoot) {
                    val root = adRoot ?: return@LaunchedEffect
                    val activity = context.findHostActivity() ?: return@LaunchedEffect
                    StartupFlow.begin(activity, root) {
                        val stayInQrApp = StartupNavigation.continueAfterVisibleSplash(activity)
                        if (stayInQrApp) {
                            WidgetNavigation.openPendingComposeDestination(activity)
                            navController.navigate(MAIN_GRAPH_ROUTE) {
                                popUpTo(AppDestination.Splash.route) { inclusive = true }
                            }
                        }
                    }
                }
            }

            navigation(
                route = MAIN_GRAPH_ROUTE,
                startDestination = if (reopenSettings) {
                    AppDestination.Settings.route
                } else {
                    AppDestination.Scan.route
                }
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
                            context.runWithClickAd("MainScreen") {
                                context.startActivity(CreateQrIntents.openHub(context))
                            }
                        },
                        onOpenHistory = {
                            context.runWithClickAd("MainScreen") {
                                context.startActivity(HistoryIntents.openHistory(context))
                            }
                        },
                        onOpenFavorites = {
                            context.runWithClickAd("MainScreen") {
                                context.startActivity(HistoryIntents.openFavorites(context))
                            }
                        },
                        onViewAllHistory = {
                            context.runWithClickAd("MainScreen") {
                                context.startActivity(HistoryIntents.openHistory(context))
                            }
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
                    val context = LocalContext.current
                    SettingsScreen(
                        onOpenAbout = {
                            navController.navigate(AppDestination.About.route)
                        },
                        onOpenLanguage = {
                            context.runWithClickAd("SettingsFragmentScreen") {
                                context.startActivity(
                                    Intent(context, LanguageActivity::class.java)
                                        .putExtra(LanguageActivity.EXTRA_FROM_APP_SETTINGS, true)
                                )
                            }
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
            composable(AppDestination.About.route) {
                ScreenWithAd(screenKey = "OtherScreen") {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
            }
        }
        }
        if (matchHeaderStatus) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(CardSurface)
            )
        }
    }
}
