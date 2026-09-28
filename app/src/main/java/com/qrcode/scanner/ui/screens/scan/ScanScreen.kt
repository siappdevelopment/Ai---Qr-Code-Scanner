package com.qrcode.scanner.ui.screens.scan

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.qrcode.scanner.data.history.HistoryRepositoryProvider
import kotlinx.coroutines.launch

/**
 * Scanner shown in the same root content area as Create.
 * Reuses [ScannerViewfinderScreen]. Does not open [ScannerActivity].
 */
@Composable
fun ScanScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    safeContentPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val historyRepository = remember { HistoryRepositoryProvider.get(context) }
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(view) {
            val window = (view.context as? Activity)?.window
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            val previousLightStatus = controller?.isAppearanceLightStatusBars
            controller?.isAppearanceLightStatusBars = false
            onDispose {
                if (previousLightStatus != null) {
                    controller.isAppearanceLightStatusBars = previousLightStatus
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ScannerViewfinderScreen(
            modifier = Modifier.fillMaxSize(),
            safeContentPadding = safeContentPadding,
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
