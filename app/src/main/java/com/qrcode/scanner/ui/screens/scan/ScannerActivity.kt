package com.qrcode.scanner.ui.screens.scan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.qrcode.scanner.ui.theme.QRCodeScannerTheme

/**
 * Full-screen camera scanner.
 * Stitch source: Camera Scanner Viewfinder — b12400fc841b48708d9fabb9c528a652
 * Adapted to White + Electric Cobalt (no gradients / shadows / Profile / Ask AI).
 * No BottomNavigation (secondary Intent Activity).
 */
class ScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QRCodeScannerTheme {
                ScannerViewfinderScreen(
                    onBack = { finish() },
                    onBarcodeDetected = { rawValue, format, formatName ->
                        startActivity(
                            ScanIntents.openScanResult(
                                context = this,
                                rawValue = rawValue,
                                format = format,
                                formatName = formatName
                            )
                        )
                        // Keep Scanner on the back stack so Back from Result returns here.
                    }
                )
            }
        }
    }
}
