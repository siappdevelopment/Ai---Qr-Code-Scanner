package com.qrcode.scanner.ui.screens.scan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.qrcode.scanner.data.history.HistoryRepositoryProvider
import kotlinx.coroutines.launch

/**
 * Scanner shown in the same root content area as Create.
 * Reuses [ScannerViewfinderScreen]. Does not open [ScannerActivity].
 */
@Composable
fun ScanScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val historyRepository = remember { HistoryRepositoryProvider.get(context) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .consumeWindowInsets(WindowInsets.systemBars)
    ) {
        ScannerViewfinderScreen(
            modifier = Modifier.fillMaxSize(),
            onBack = onClose,
            onBarcodeDetected = { rawValue, format, formatName ->
                context.startActivity(
                    ScanIntents.openScanResult(
                        context = context,
                        rawValue = rawValue,
                        format = format,
                        formatName = formatName
                    )
                )
            },
            onFinishContinuousBatch = { items ->
                if (items.isEmpty()) {
                    onClose()
                } else {
                    scope.launch {
                        val persisted = ContinuousBatchHistory.persistAcceptedItems(
                            repository = historyRepository,
                            items = items
                        )
                        context.startActivity(
                            ScanIntents.openContinuousBatchResult(
                                context = context,
                                items = persisted
                            )
                        )
                        onClose()
                    }
                }
            }
        )
    }
}
