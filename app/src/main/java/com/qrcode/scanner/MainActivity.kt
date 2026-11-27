package com.qrcode.scanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.qrcode.scanner.ui.theme.enableThemedEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.qrcode.scanner.launcher.common.StartupFlow
import com.qrcode.scanner.ui.navigation.ScanPulseNavHost
import com.qrcode.scanner.ui.theme.QRCodeScannerTheme

/**
 * AppCompatActivity so AppCompatDelegate per-app locales recreate correctly (Phase 12.19).
 * The icon entry keeps the existing Compose splash. Screen-flow returns skip that splash.
 */
class MainActivity : AppCompatActivity() {
    private var showStartupSplash = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            com.qrcode.scanner.launcher.common.AnalyticsEvents.log(this, com.qrcode.scanner.launcher.common.AnalyticsEvents.MAIN_VIEW)
        }
        enableThemedEdgeToEdge()
        showStartupSplash = StartupFlow.shouldShowSplash(intent)
        setContent {
            QRCodeScannerTheme {
                ScanPulseNavHost(showStartupSplash = showStartupSplash)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        StartupFlow.onRequestPermissionsResult(this, requestCode, grantResults)
    }

    override fun onDestroy() {
        StartupFlow.onHostDestroy(this)
        super.onDestroy()
    }
}
