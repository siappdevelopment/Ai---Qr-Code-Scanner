package com.qrcode.scanner.ui.screens.scan

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.qrcode.scanner.ui.theme.enableThemedEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.barcode.common.Barcode
import com.qrcode.scanner.ui.theme.BorderSubtle
import com.qrcode.scanner.ui.theme.CardSurface
import com.qrcode.scanner.ui.theme.CobaltPrimary
import com.qrcode.scanner.ui.theme.CobaltSoft
import com.qrcode.scanner.ui.components.AppBackButton
import com.qrcode.scanner.ui.components.appHeaderBackground
import com.qrcode.scanner.ui.theme.PageBackground
import com.qrcode.scanner.ui.theme.PlusJakartaSans
import com.qrcode.scanner.ui.theme.QRCodeScannerTheme
import com.qrcode.scanner.ui.components.ScreenWithAd
import com.qrcode.scanner.ui.theme.TextPrimary
import com.qrcode.scanner.ui.theme.TextSecondary
import com.qrcode.scanner.ui.theme.White

/**
 * Continuous Batch session results (in-memory snapshot via Intent).
 * History rows are already written at session finish — this screen only reviews.
 * No BottomNavigation.
 * White + Electric Cobalt — no gradients / shadows / Profile / Ask AI.
 */
class ContinuousBatchResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableThemedEdgeToEdge()
        val items = readBatchItems(intent.extras)
        // Empty/invalid Intent — exit safely (normal finish path never opens empty).
        if (items.isEmpty()) {
            finish()
            return
        }
        setContent {
            QRCodeScannerTheme {
                ScreenWithAd(screenKey = "OtherScreen") {
                ContinuousBatchResultScreen(
                    items = items,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onOpenItem = { item ->
                        // Only open already-persisted History rows (Phase 12.10 policy).
                        if (item.historyId <= 0L) return@ContinuousBatchResultScreen
                        startActivity(
                            ScanIntents.openScanResult(
                                context = this,
                                rawValue = item.rawValue,
                                format = item.barcodeFormat,
                                formatName = item.barcodeFormatName,
                                historyId = item.historyId,
                                // Batch Results is the review point — no Auto-Open on detail.
                                skipAutoOpen = true
                            )
                        )
                        // Do not finish — Back from detail returns here.
                    }
                )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun readBatchItems(extras: Bundle?): List<ContinuousBatchItem> {
        if (extras == null) return emptyList()
        val raw: Any? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getSerializable(ScanIntents.EXTRA_BATCH_ITEMS, ArrayList::class.java)
        } else {
            extras.getSerializable(ScanIntents.EXTRA_BATCH_ITEMS)
        }
        return (raw as? ArrayList<*>)
            ?.filterIsInstance<ContinuousBatchItem>()
            .orEmpty()
    }
}

@Composable
private fun ContinuousBatchResultScreen(
    items: List<ContinuousBatchItem>,
    onBack: () -> Unit,
    onOpenItem: (ContinuousBatchItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .appHeaderBackground()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppBackButton(onClick = onBack)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Batch Results",
                color = TextPrimary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                letterSpacing = (-0.2).sp
            )
        }

        Text(
            text = if (items.size == 1) {
                "1 code accepted"
            } else {
                "${items.size} codes accepted"
            },
            color = TextSecondary,
            fontFamily = PlusJakartaSans,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(
                items = items,
                key = { index, item ->
                    "${item.rawValue}|${item.barcodeFormat}|$index"
                }
            ) { _, item ->
                ContinuousBatchResultRow(
                    item = item,
                    onClick = { onOpenItem(item) }
                )
            }
        }
    }
}

@Composable
private fun ContinuousBatchResultRow(
    item: ContinuousBatchItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isQrFamily = item.barcodeFormat == Barcode.FORMAT_QR_CODE ||
        item.barcodeFormat == Barcode.FORMAT_AZTEC ||
        item.barcodeFormat == Barcode.FORMAT_DATA_MATRIX
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface, RoundedCornerShape(16.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CobaltSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isQrFamily) {
                    Icons.Outlined.QrCode2
                } else {
                    Icons.Outlined.ViewWeek
                },
                contentDescription = null,
                tint = CobaltPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.barcodeFormatName,
                color = CobaltPrimary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
            Text(
                text = item.rawValue,
                color = TextPrimary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp)
        )
    }
}
