package com.qrcode.scanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.qrcode.scanner.launcher.common.AdPlacement
import com.qrcode.scanner.launcher.common.StartupFlow
import com.qrcode.scanner.launcher.common.WidgetNavigation
import com.qrcode.scanner.launcher.helpers.DefaultHomePopupHost
import com.qrcode.scanner.launcher.helpers.ReminderAlarmHelper
import com.qrcode.scanner.ui.navigation.ScanPulseNavHost
import com.qrcode.scanner.ui.theme.QRCodeScannerTheme

/**
 * AppCompatActivity so AppCompatDelegate per-app locales recreate correctly (Phase 12.19).
 * The icon entry keeps the existing Compose splash. Screen-flow returns skip that splash.
 */
class MainActivity : AppCompatActivity() {
    private var showStartupSplash = false
    private val defaultHomePopupHost = DefaultHomePopupHost(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        defaultHomePopupHost.register()
        AdPlacement.requestCallEndIpCountryIfNeeded(this)
        enableEdgeToEdge()
        showStartupSplash = StartupFlow.shouldShowSplash(intent)
        ReminderAlarmHelper.handleReminderLaunchIntent(this, intent)
        if (!showStartupSplash) {
            WidgetNavigation.openComposeDestinationFromIntent(this)
        }
        setContent {
            QRCodeScannerTheme {
                ScanPulseNavHost(showStartupSplash = showStartupSplash)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!showStartupSplash) {
            defaultHomePopupHost.onHostResume()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ReminderAlarmHelper.handleReminderLaunchIntent(this, intent)
        if (showStartupSplash) {
            WidgetNavigation.captureTargetScreen(this, intent)
        } else {
            WidgetNavigation.openComposeDestinationFromIntent(this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        StartupFlow.onRequestPermissionsResult(this, requestCode, grantResults)
    }

    override fun onDestroy() {
        defaultHomePopupHost.release()
        StartupFlow.onHostDestroy(this)
        super.onDestroy()
    }
}
