package com.qrcode.scanner.ui.screens.create

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Modifier
import com.qrcode.scanner.ui.theme.QRCodeScannerTheme
import com.qrcode.scanner.ui.components.ScreenWithAd
import com.qrcode.scanner.ui.components.bindScreenBackAd
import com.qrcode.scanner.ui.components.runWithClickAd

/**
 * Hosts the existing Create hub. System back finishes and returns to Home.
 */
class CreateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bindScreenBackAd("CreateHubScreen")
        setContent {
            QRCodeScannerTheme {
                ScreenWithAd(screenKey = "CreateHubScreen", nativeSize = "small") {
                CreateHubScreen(
                    modifier = Modifier.navigationBarsPadding(),
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onCategoryClick = { type ->
                        runWithClickAd("CreateHubScreen") {
                            startActivity(CreateQrIntents.openCategory(this@CreateActivity, type))
                        }
                    }
                )
                }
            }
        }
    }
}
