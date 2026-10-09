package com.qrcode.scanner.launcher.fragments

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
        // Some devices (Redmi) leave the status bar area uncovered after a theme change and show the
        // transparent window (wallpaper) there. Paint it so the bar always matches the header.
        headerWindowColor = palette.cardSurface.toArgb()
        applyHeaderWindowBackground(activity)
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = !useDark
    }

    private var headerWindowColor: Int? = null

    private fun applyHeaderWindowBackground(activity: Activity) {
        val color = headerWindowColor ?: return
        activity.window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(color))
    }

    /** Back to the transparent wallpaper window (home page, Scan page, or while swiping away). */
    fun clearHeaderWindowBackground(activity: Activity) {
        activity.window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
    }

    /** Re-applies the header window colour after a page swipe settles on the QR page. */
    fun reapplyHeaderWindowBackground(activity: Activity) {
        applyHeaderWindowBackground(activity)
    }

    /** Scan camera sits under the status bar, so the bar must not keep the previous screen color. */
    fun applyScanStatusBar(activity: Activity) {
        val window = activity.window
        headerWindowColor = null
        clearHeaderWindowBackground(activity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
        }
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = false
    }

    /**
     * MainActivity only (app not set as default launcher): transparent status bar,
     * camera edge-to-edge, system nav swipe-to-show. Must not be used on LauncherHomeActivity.
     */
    fun applyStandaloneAppScanBars(activity: Activity) {
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            window.navigationBarDividerColor = Color.TRANSPARENT
        }
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.show(WindowInsetsCompat.Type.statusBars())
        controller.hide(WindowInsetsCompat.Type.navigationBars())
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

    /**
     * Wallpaper home page. The bar stays visible, but it must not paint white
     * over the wallpaper on Android 15+.
     */
    fun showTransparentNavigationBar(activity: Activity) {
        val componentActivity = activity as? ComponentActivity ?: return
        headerWindowColor = null
        clearHeaderWindowBackground(activity)
        val window = componentActivity.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        componentActivity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightNavigationBars = false
        controller.isAppearanceLightStatusBars = false
        controller.show(WindowInsetsCompat.Type.navigationBars())
    }

    fun restore(activity: Activity) {
        showTransparentNavigationBar(activity)
    }
}
