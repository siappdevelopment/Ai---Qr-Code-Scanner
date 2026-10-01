package com.qrcode.scanner.ui.screens.create

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.qrcode.scanner.ui.theme.QRCodeScannerTheme
import com.qrcode.scanner.ui.components.ScreenWithAd

/**
 * Hosts the existing Create hub. System back finishes and returns to Home.
 */
class CreateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QRCodeScannerTheme {
                ScreenWithAd(screenKey = "CreateHubScreen") {
                val context = LocalContext.current
                CreateHubScreen(
                    modifier = Modifier.navigationBarsPadding(),
                    onBack = { finish() },
                    onCategoryClick = { type ->
                        context.startActivity(CreateQrIntents.openCategory(context, type))
                    }
                )
                }
            }
        }
    }
}
