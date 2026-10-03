package com.qrcode.scanner.ui.screens.scan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.qrcode.scanner.ui.theme.enableThemedEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.qrcode.scanner.data.history.HistoryRepositoryProvider
import com.qrcode.scanner.ui.theme.QRCodeScannerTheme
import kotlinx.coroutines.launch

/**
 * Full-screen camera scanner.
 * Stitch source: Camera Scanner Viewfinder — b12400fc841b48708d9fabb9c528a652
 * Adapted to White + Electric Cobalt (no gradients / shadows / Profile / Ask AI).
 * No BottomNavigation (secondary Intent Activity).
 */
class ScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableThemedEdgeToEdge()
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
                    },
                    onFinishContinuousBatch = { items ->
                        if (items.isEmpty()) {
                            finish()
                        } else {
                            // Persist History once per accepted item, then open review UI.
                            lifecycleScope.launch {
                                val repository = HistoryRepositoryProvider.get(this@ScannerActivity)
                                val persisted = ContinuousBatchHistory.persistAcceptedItems(
                                    repository = repository,
                                    items = items
                                )
                                startActivity(
                                    ScanIntents.openContinuousBatchResult(
                                        context = this@ScannerActivity,
                                        items = persisted
                                    )
                                )
                                // End scanner session so CameraX is torn down under results.
                                finish()
                            }
                        }
                    }
                )
            }
        }
    }
}
