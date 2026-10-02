package com.qrcode.scanner.launcher.fragments

import android.app.Activity
import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.qrcode.scanner.ui.theme.ScanPulsePalette

/**
 * Home and Settings status bar follows the header surface.
 * The right-side page hides the system navigation bar until the user swipes it in.
 */
object LauncherQrSystemBars {
    fun applyHeaderStatusBar(activity: Activity, useDark: Boolean) {
        val window = activity.window
        val palette = if (useDark) ScanPulsePalette.Dark else ScanPulsePalette.Light
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
        }
        @Suppress("DEPRECATION")
        window.statusBarColor = palette.cardSurface.toArgb()
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = !useDark
    }

    /** Hides the navigation bar until the user swipes it in. Status bar stays visible. */
    fun hideNavigationBar(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        val controller = activity.window.insetsController ?: return
        controller.hide(WindowInsets.Type.navigationBars())
        controller.systemBarsBehavior =
            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    fun hideNavigationBarUntilSwipe(activity: Activity) {
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.navigationBars())
    }

    fun restore(activity: Activity) {
        val componentActivity = activity as? ComponentActivity ?: return
        componentActivity.enableEdgeToEdge()
        WindowCompat.getInsetsController(componentActivity.window, componentActivity.window.decorView)
            .show(WindowInsetsCompat.Type.navigationBars())
    }
}
