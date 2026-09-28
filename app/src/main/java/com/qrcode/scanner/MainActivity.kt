package com.qrcode.scanner

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.qrcode.scanner.ui.navigation.ScanPulseNavHost
import com.qrcode.scanner.ui.theme.QRCodeScannerTheme

/**
 * AppCompatActivity so AppCompatDelegate per-app locales recreate correctly (Phase 12.19).
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QRCodeScannerTheme {
                ScanPulseNavHost()
            }
        }
    }
}
