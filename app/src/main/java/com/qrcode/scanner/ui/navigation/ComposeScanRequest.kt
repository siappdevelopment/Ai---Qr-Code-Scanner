package com.qrcode.scanner.ui.navigation

import android.content.Context
import android.content.Intent
import com.qrcode.scanner.MainActivity
import com.qrcode.scanner.launcher.activities.LauncherHomeActivity
import com.qrcode.scanner.launcher.common.AppUtils
import com.qrcode.scanner.launcher.common.ScreenFlowNavigation
import com.qrcode.scanner.launcher.remote.ScreenFlowConfig

/**
 * History asks the existing Scan page to open. The scanner activity is not used.
 */
object ComposeScanRequest {
    @Volatile
    private var pending: Boolean = false

    fun request() {
        pending = true
    }

    fun isPending(): Boolean = pending

    fun consume(): Boolean {
        if (!pending) {
            return false
        }
        pending = false
        return true
    }

    /**
     * Opens the app on its Scan fragment. The host is started explicitly, so this also works when the caller
     * has nothing under it (History opened from the Call End screen, which closes itself after launching).
     * Host: the launcher when it is the default Home (or redirects to it), otherwise MainActivity.
     */
    fun openScan(context: Context) {
        context.startActivity(scanIntent(context))
    }

    /**
     * Recent screen "Scan": the launcher opens the page exactly as a right swipe does (from its Home page, sliding). When
     * the app is not hosted by the launcher, the app's Scan page is opened as usual.
     */
    fun swipeScanIntent(context: Context): Intent {
        if (AppUtils.isDefaultHomeApp(context) || ScreenFlowConfig.getRedirectHomeLauncher()) {
            return Intent(context, LauncherHomeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(LauncherHomeActivity.EXTRA_SWIPE_TO_SCAN, true)
            }
        }
        return scanIntent(context)
    }

    /** Asks for the Scan page and returns the intent that opens its host (for callers that start it themselves). */
    fun scanIntent(context: Context): Intent {
        request()
        return if (AppUtils.isDefaultHomeApp(context) || ScreenFlowConfig.getRedirectHomeLauncher()) {
            // Plain intent to the launcher, not the HOME intent: a HOME intent makes LauncherHomeActivity first
            // return to its wallpaper Home page (and clear the task) before it slides to the QR page, which showed
            // a white/black frame. Without the HOME category it goes straight to the QR page (see onResume).
            Intent(context, LauncherHomeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        } else {
            Intent(context, MainActivity::class.java).apply {
                putExtra(ScreenFlowNavigation.EXTRA_SKIP_STARTUP_SPLASH, true)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        }
    }
}
