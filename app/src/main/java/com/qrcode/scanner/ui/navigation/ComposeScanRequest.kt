package com.qrcode.scanner.ui.navigation

import android.content.Context
import android.content.Intent
import com.qrcode.scanner.MainActivity
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
        request()
        val intent = if (AppUtils.isDefaultHomeApp(context) || ScreenFlowConfig.getRedirectHomeLauncher()) {
            AppUtils.buildLauncherHomeIntent(context)
        } else {
            Intent(context, MainActivity::class.java).apply {
                putExtra(ScreenFlowNavigation.EXTRA_SKIP_STARTUP_SPLASH, true)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        }
        context.startActivity(intent)
    }
}
