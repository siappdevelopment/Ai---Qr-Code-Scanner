package com.qrcode.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.qrcode.scanner.ui.navigation.ScanPulseNavHost
import com.qrcode.scanner.ui.theme.QRCodeScannerTheme

class MainActivity : ComponentActivity() {
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
