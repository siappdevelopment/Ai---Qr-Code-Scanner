package com.qrcode.scanner.ui.screens.create

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.barcode.common.Barcode
import com.qrcode.scanner.data.history.HistoryEntity
import com.qrcode.scanner.data.history.HistoryRepositoryProvider
import com.qrcode.scanner.data.history.ScanPayloadMapper
import com.qrcode.scanner.ui.theme.BorderSubtle
import com.qrcode.scanner.ui.theme.CardSurface
import com.qrcode.scanner.ui.theme.CobaltPrimary
import com.qrcode.scanner.ui.theme.CobaltSoft
import com.qrcode.scanner.ui.theme.NestedSurface
import com.qrcode.scanner.ui.theme.PageBackground
import com.qrcode.scanner.ui.theme.PlusJakartaSans
import com.qrcode.scanner.ui.theme.QRCodeScannerTheme
import com.qrcode.scanner.ui.theme.TextPrimary
import com.qrcode.scanner.ui.theme.TextSecondary
import com.qrcode.scanner.ui.theme.TextTertiary
import com.qrcode.scanner.ui.theme.White
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Stitch: QR Preview & Export (White Theme) — 49aeabcad7d145c785c519fd8794262b
 *
 * Phase 7: real QR bitmap + Save to History. Customize / PNG / SVG / Print omitted.
 */
class QrPreviewActivity : ComponentActivity() {

    private var savedHistoryId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        savedHistoryId = savedInstanceState?.getLong(KEY_HISTORY_ID, -1L) ?: -1L

        val category = QrCategoryType.fromIntentExtra(
            intent.getStringExtra(QrCategoryType.EXTRA_QR_CATEGORY)
        )
        val payload = intent.getStringExtra(CreateQrIntents.EXTRA_PAYLOAD).orEmpty()
        val title = intent.getStringExtra(CreateQrIntents.EXTRA_DISPLAY_TITLE).orEmpty()
        val detectedType = intent.getStringExtra(CreateQrIntents.EXTRA_DETECTED_TYPE)
            ?: ScanPayloadMapper.TYPE_QR_CODE
        val eccName = intent.getStringExtra(CreateQrIntents.EXTRA_ECC_LEVEL)
            ?: QrBitmapEncoder.EccLevel.H.name
        val ecc = QrBitmapEncoder.EccLevel.entries
            .firstOrNull { it.name == eccName }
            ?: QrBitmapEncoder.EccLevel.H

        setContent {
            QRCodeScannerTheme {
                QrPreviewScreen(
                    category = category,
                    payload = payload,
                    displayTitle = title.ifBlank { category.displayTitle },
                    detectedType = detectedType,
                    ecc = ecc,
                    initialHistoryId = savedHistoryId,
                    onHistoryIdAssigned = { savedHistoryId = it },
                    onBack = { finish() },
                    onSavedAndDone = { historyId ->
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(CreateQrIntents.RESULT_SAVED_HISTORY_ID, historyId)
                        )
                        finish()
                    }
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_HISTORY_ID, savedHistoryId)
    }

    companion object {
        private const val KEY_HISTORY_ID = "key_preview_history_id"
    }
}

@Composable
private fun QrPreviewScreen(
    category: QrCategoryType,
    payload: String,
    displayTitle: String,
    detectedType: String,
    ecc: QrBitmapEncoder.EccLevel,
    initialHistoryId: Long,
    onHistoryIdAssigned: (Long) -> Unit,
    onBack: () -> Unit,
    onSavedAndDone: (Long) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { HistoryRepositoryProvider.get(context) }
    val scope = rememberCoroutineScope()

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var encodeError by remember { mutableStateOf<String?>(null) }
    var historyId by remember { mutableLongStateOf(initialHistoryId) }
    var favorite by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(payload, ecc) {
        if (payload.isBlank()) {
            encodeError = "Nothing to preview"
            return@LaunchedEffect
        }
        encodeError = null
        bitmap = withContext(Dispatchers.Default) {
            runCatching { QrBitmapEncoder.encode(payload, sizePx = 512, ecc = ecc) }
                .onFailure { encodeError = it.message ?: "Unable to generate QR" }
                .getOrNull()
        }
        if (historyId > 0L) {
            favorite = repository.getById(historyId)?.isFavorite == true
        }
    }

    val typeIcon: ImageVector = when (detectedType) {
        ScanPayloadMapper.TYPE_WIFI -> Icons.Outlined.Wifi
        ScanPayloadMapper.TYPE_WEBSITE -> Icons.Outlined.Link
        else -> Icons.Outlined.CheckCircle
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PreviewIconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack
                )
                Text(
                    text = "QR Preview",
                    color = TextPrimary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            PreviewIconButton(
                icon = Icons.Outlined.Share,
                contentDescription = "Share",
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, payload)
                        putExtra(Intent.EXTRA_TITLE, displayTitle)
                    }
                    context.startActivity(Intent.createChooser(send, "Share QR payload"))
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(CobaltSoft)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Ready to Save",
                            color = CobaltPrimary,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "Level ${ecc.label}",
                        color = TextSecondary,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NestedSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            if (historyId <= 0L) {
                                Toast.makeText(
                                    context,
                                    "Save to History first to favorite",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@clickable
                            }
                            val next = !favorite
                            favorite = next
                            scope.launch {
                                repository.updateFavorite(historyId, next)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (favorite) CobaltPrimary else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSurface, RoundedCornerShape(18.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NestedSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        encodeError != null -> Text(
                            text = encodeError!!,
                            color = TextSecondary,
                            fontFamily = PlusJakartaSans,
                            fontSize = 14.sp
                        )
                        bitmap != null -> Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = "Generated QR code",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        else -> Text(
                            text = "Generating…",
                            color = TextTertiary,
                            fontFamily = PlusJakartaSans,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = CobaltPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = category.displayTitle.uppercase(),
                        color = CobaltPrimary,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        letterSpacing = 0.6.sp
                    )
                }
                Text(
                    text = displayTitle,
                    color = TextPrimary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = payload,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NestedSurface)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        tint = CobaltPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "Valid QR payload • UTF-8",
                        color = TextSecondary,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp
                    )
                }
            }

            // Primary Save — Phase 7 persistence (Customize/export excluded)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (saving || bitmap == null) NestedSurface else CobaltPrimary)
                    .clickable(
                        enabled = !saving && bitmap != null && payload.isNotBlank(),
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        saving = true
                        scope.launch {
                            try {
                                val id = if (historyId > 0L) {
                                    historyId
                                } else {
                                    repository.insertScanAvoidingDuplicate(
                                        rawValue = payload,
                                        barcodeFormat = Barcode.FORMAT_QR_CODE,
                                        barcodeFormatName = "QR_CODE",
                                        detectedType = detectedType,
                                        source = HistoryEntity.SOURCE_CREATED
                                    ).also {
                                        historyId = it
                                        onHistoryIdAssigned(it)
                                    }
                                }
                                Toast.makeText(context, "Saved to History", Toast.LENGTH_SHORT)
                                    .show()
                                onSavedAndDone(id)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    e.message ?: "Unable to save",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                saving = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (historyId > 0L) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = if (saving || bitmap == null) TextTertiary else White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (saving) "Saving…" else "Save to History",
                        color = if (saving || bitmap == null) TextTertiary else White,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Code Specifications",
                    color = TextPrimary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    SpecCell("Symbology", "QR Model 2", Modifier.weight(1f))
                    SpecCell("Matrix", "512 × 512", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    SpecCell("Redundancy", "Level ${ecc.label}", Modifier.weight(1f))
                    SpecCell("Encoding", "Byte / UTF-8", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SpecCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = TextTertiary,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = TextPrimary,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun PreviewIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TextPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}
