package com.qrcode.scanner.ui.screens.history

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.qrcode.scanner.ui.screens.scan.ScanIntents
import com.qrcode.scanner.ui.theme.QRCodeScannerTheme
import com.qrcode.scanner.ui.components.ScreenWithAd

/**
 * Hosts the existing History screen. System back finishes and returns to Home.
 */
class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QRCodeScannerTheme {
                ScreenWithAd(screenKey = "HistoryScreen") {
                val context = LocalContext.current
                HistoryScreen(
                    modifier = Modifier.navigationBarsPadding(),
                    onBack = { finish() },
                    onOpenScanner = {
                        context.startActivity(ScanIntents.openScanner(context))
                    },
                    onOpenDetail = { historyId ->
                        context.startActivity(HistoryIntents.openDetail(context, historyId))
                    }
                )
                }
            }
        }
    }
}
