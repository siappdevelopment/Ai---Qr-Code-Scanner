package com.qrcode.scanner.ui.screens.create

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.qrcode.scanner.ui.components.ScreenWithAd
import com.qrcode.scanner.ui.components.bindScreenBackAd
import com.qrcode.scanner.ui.components.runWithClickAd
import com.qrcode.scanner.ui.theme.QRCodeScannerTheme
import com.qrcode.scanner.ui.theme.enableThemedEdgeToEdge

/**
 * Hosts the existing Create hub. System back finishes and returns to Home.
 */
class CreateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        enableThemedEdgeToEdge()
        bindScreenBackAd("CreateHubScreen")
        setContent {
            QRCodeScannerTheme {
                var searching by remember { mutableStateOf(false) }
                ScreenWithAd(screenKey = "CreateHubScreen", nativeSize = "small", showAd = !searching) {
                    CreateHubScreen(
                        onBack = { onBackPressedDispatcher.onBackPressed() },
                        onSearchActiveChange = { searching = it },
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
