package com.qrcode.scanner.ui.navigation

/**
 * App destinations. Root tabs show shared BottomNavigation from
 * Stitch "History Empty State (White Theme)" e63b148524174fd1ab5cd0ea369e5e1e.
 */
sealed class AppDestination(val route: String) {
    // Launch
    data object Splash : AppDestination("splash")

    // Root tabs (BottomNavigation visible)
    data object Home : AppDestination("home")
    data object Create : AppDestination("create")
    data object Scan : AppDestination("scan")
    data object History : AppDestination("history")
    data object Settings : AppDestination("settings")

    // Secondary (no BottomNavigation)
    data object CameraPermission : AppDestination("camera_permission")
    data object GalleryCrop : AppDestination("gallery_crop")
    data object DetectionError : AppDestination("detection_error")
    data object ScanResult : AppDestination("scan_result")
    data object QrCustomization : AppDestination("qr_customization")
    data object QrPreviewExport : AppDestination("qr_preview_export")
    data object HistoryDetail : AppDestination("history_detail")
    data object Language : AppDestination("language")
}

val rootDestinations: Set<String> = setOf(
    AppDestination.Home.route,
    AppDestination.Create.route,
    AppDestination.Scan.route,
    AppDestination.History.route,
    AppDestination.Settings.route
)

fun String?.showsBottomNavigation(): Boolean =
    this != null && this in rootDestinations
