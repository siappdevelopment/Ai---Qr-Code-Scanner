package com.qrcode.scanner.ui.screens.history

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrcode.scanner.data.history.HistoryRepositoryProvider
import com.qrcode.scanner.ui.screens.scan.ScanResultScreen
import com.qrcode.scanner.ui.theme.CobaltPrimary
import com.qrcode.scanner.ui.theme.PageBackground
import com.qrcode.scanner.ui.theme.PlusJakartaSans
import com.qrcode.scanner.ui.theme.QRCodeScannerTheme
import com.qrcode.scanner.ui.theme.TextPrimary
import com.qrcode.scanner.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * History detail — loads a Room record by id.
 * Stitch source: History Detail d4f5784dce7449ba8de7aa3e9007b047 (generalized for all types).
 */
class HistoryDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val historyId = intent.getLongExtra(HistoryIntents.EXTRA_HISTORY_ID, -1L)
        setContent {
            QRCodeScannerTheme {
                HistoryDetailRoute(
                    historyId = historyId,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
private fun HistoryDetailRoute(
    historyId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { HistoryRepositoryProvider.get(context) }
    val scope = rememberCoroutineScope()

    if (historyId <= 0L) {
        MissingHistory(onBack = onBack)
        return
    }

    val entity by repository.observeById(historyId).collectAsStateWithLifecycle(initialValue = null)
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(historyId) {
        repository.getById(historyId)
        loaded = true
    }

    when {
        !loaded && entity == null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PageBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CobaltPrimary)
            }
        }
        entity == null -> MissingHistory(onBack = onBack)
        else -> {
            val item = entity!!
            ScanResultScreen(
                rawValue = item.rawValue,
                formatName = item.barcodeFormatName,
                format = item.barcodeFormat,
                historyId = item.id,
                initialFavorite = item.isFavorite,
                scannedAtMillis = item.timestamp,
                title = "History Detail",
                onBack = onBack,
                onFavoriteChange = { favorite ->
                    scope.launch {
                        repository.updateFavorite(item.id, favorite)
                        Toast.makeText(
                            context,
                            if (favorite) "Saved to your favorites" else "Removed from favorites",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
    }
}

@Composable
private fun MissingHistory(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Scan not found",
            color = TextPrimary,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp
        )
        Text(
            text = "This history item may have been deleted.",
            color = TextSecondary,
            fontFamily = PlusJakartaSans,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )
        TextButton(onClick = onBack) {
            Text(
                text = "Go back",
                color = CobaltPrimary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
