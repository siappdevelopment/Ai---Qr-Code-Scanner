package com.qrcode.scanner.ui.screens.create

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import com.qrcode.scanner.ui.theme.enableThemedEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.qrcode.scanner.app.R
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
import com.qrcode.scanner.ui.theme.NestedSurface
import com.qrcode.scanner.ui.components.AppBackButton
import com.qrcode.scanner.ui.components.appHeaderBackground
import com.qrcode.scanner.ui.theme.PageBackground
import com.qrcode.scanner.ui.theme.PlusJakartaSans
import com.qrcode.scanner.ui.theme.QRCodeScannerTheme
import com.qrcode.scanner.ui.components.ScreenWithAd
import com.qrcode.scanner.ui.components.runWithClickAd
import com.qrcode.scanner.ui.components.bindScreenBackAd
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
 * Phase 7: real QR + Save to History.
 * Phase 8: Customize → styled render + PNG / SVG / Print / Share.
 */
class QrPreviewActivity : ComponentActivity() {

    private var savedHistoryId: Long = -1L
    private var styleForSave: QrStyleConfig = QrStyleConfig.Default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableThemedEdgeToEdge()
        bindScreenBackAd("QrPreviewScreen")
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
        @Suppress("DEPRECATION")
        val restoredStyle = savedInstanceState?.getSerializable(KEY_STYLE) as? QrStyleConfig
            ?: QrStyleConfig.Default
        styleForSave = restoredStyle

        setContent {
            QRCodeScannerTheme {
                ScreenWithAd(screenKey = "QrPreviewScreen", nativeSize = "small") {
                QrPreviewScreen(
                    category = category,
                    payload = payload,
                    displayTitle = title.ifBlank { category.displayTitle(this@QrPreviewActivity) },
                    detectedType = detectedType,
                    ecc = ecc,
                    initialStyle = restoredStyle,
                    initialHistoryId = savedHistoryId,
                    onHistoryIdAssigned = { savedHistoryId = it },
                    onStyleChanged = { /* persisted via Activity saveInstance below through callback state */ },
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onSavedAndDone = { historyId ->
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(CreateQrIntents.RESULT_SAVED_HISTORY_ID, historyId)
                        )
                        finish()
                    },
                    onPersistStyle = { styleForSave = it }
                )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_HISTORY_ID, savedHistoryId)
        outState.putSerializable(KEY_STYLE, styleForSave)
    }

    companion object {
        private const val KEY_HISTORY_ID = "key_preview_history_id"
        private const val KEY_STYLE = "key_preview_style"
    }
}

@Composable
private fun QrPreviewScreen(
    category: QrCategoryType,
    payload: String,
    displayTitle: String,
    detectedType: String,
    ecc: QrBitmapEncoder.EccLevel,
    initialStyle: QrStyleConfig,
    initialHistoryId: Long,
    onHistoryIdAssigned: (Long) -> Unit,
    onStyleChanged: (QrStyleConfig) -> Unit,
    onBack: () -> Unit,
    onSavedAndDone: (Long) -> Unit,
    onPersistStyle: (QrStyleConfig) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { HistoryRepositoryProvider.get(context) }
    val scope = rememberCoroutineScope()

    var style by remember { mutableStateOf(initialStyle) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var exportBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var encodeError by remember { mutableStateOf<String?>(null) }
    var historyId by remember { mutableLongStateOf(initialHistoryId) }
    var saving by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var logoBmp by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(style) {
        onStyleChanged(style)
        onPersistStyle(style)
    }

    val customizeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        @Suppress("DEPRECATION")
        val next = result.data?.getSerializableExtra(CreateQrIntents.EXTRA_STYLE) as? QrStyleConfig
            ?: return@rememberLauncherForActivityResult
        style = next
    }

    LaunchedEffect(style.customLogoUri, style.centerIcon) {
        if (style.centerIcon != QrStyleConfig.CenterIcon.CUSTOM) {
            logoBmp = null
            return@LaunchedEffect
        }
        val uriStr = style.customLogoUri ?: return@LaunchedEffect
        logoBmp = withContext(Dispatchers.IO) {
            QrLogoStore.load(context, uriStr)
        }
    }

    LaunchedEffect(payload, ecc, style, logoBmp, category) {
        if (payload.isBlank()) {
            encodeError = context.getString(R.string.qr_preview_nothing_to_preview)
            bitmap = null
            exportBitmap = null
            return@LaunchedEffect
        }
        encodeError = null
        if (BarcodeSymbology.isBarcode(category)) {
            val rendered = withContext(Dispatchers.Default) {
                runCatching { BarcodeBitmapEncoder.encode(payload, category) }
            }
            rendered.onSuccess { image ->
                encodeError = null
                bitmap = image
                exportBitmap = image
            }.onFailure { error ->
                encodeError = error.message
                    ?: context.getString(R.string.qr_preview_unable_encode_barcode)
                bitmap = null
                exportBitmap = null
            }
            return@LaunchedEffect
        }
        val preview = withContext(Dispatchers.Default) {
            QrStyledRenderer.render(
                payload = payload,
                style = style,
                ecc = ecc,
                sizePx = 512,
                centerLogo = if (style.centerIcon == QrStyleConfig.CenterIcon.CUSTOM) logoBmp else null
            )
        }
        encodeError = preview.errorMessage
        bitmap = preview.bitmap

        if (preview.isSuccess) {
            exportBitmap = withContext(Dispatchers.Default) {
                QrStyledRenderer.render(
                    payload = payload,
                    style = style,
                    ecc = ecc,
                    sizePx = 1024,
                    centerLogo = if (style.centerIcon == QrStyleConfig.CenterIcon.CUSTOM) logoBmp else null
                ).bitmap
            }
        } else {
            exportBitmap = null
        }
    }

    val typeIcon: ImageVector = when (detectedType) {
        ScanPayloadMapper.TYPE_WIFI -> Icons.Outlined.Wifi
        ScanPayloadMapper.TYPE_WEBSITE -> Icons.Outlined.Link
        else -> Icons.Outlined.CheckCircle
    }

    fun withExportBmp(block: (Bitmap) -> Unit) {
        val bmp = exportBitmap ?: bitmap
        if (bmp == null) {
            Toast.makeText(context, context.getString(R.string.qr_preview_toast_not_ready), Toast.LENGTH_SHORT)
                .show()
            return
        }
        block(bmp)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .appHeaderBackground()
                .height(56.dp)
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppBackButton(onClick = onBack)
            Text(
                text = stringResource(R.string.qr_preview_title),
                color = TextPrimary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            PreviewIconButton(
                icon = Icons.Outlined.Share,
                contentDescription = stringResource(R.string.cd_share),
                onClick = {
                    withExportBmp { bmp ->
                        val result = QrExportHelper.shareBitmap(context, bmp, displayTitle)
                        if (!result.success) {
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                            contentDescription = stringResource(R.string.cd_generated_qr_code),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        else -> Text(
                            text = stringResource(R.string.qr_preview_generating),
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
                        text = category.displayTitle(context).uppercase(),
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
            }

            if (!BarcodeSymbology.isBarcode(category)) {
            // Customize Style & Colors
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(NestedSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .clickable(
                        enabled = payload.isNotBlank() && encodeError == null,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        context.runWithClickAd("QrPreviewScreen") {
                            customizeLauncher.launch(
                                CreateQrIntents.openCustomization(
                                    context = context,
                                    payload = payload,
                                    eccLevel = ecc.name,
                                    style = style
                                )
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = CobaltPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.qr_preview_customize_style),
                        color = TextPrimary,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (exporting || bitmap == null) NestedSurface else CobaltPrimary)
                    .clickable(
                        enabled = bitmap != null && !exporting,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        withExportBmp { bmp ->
                            exporting = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    QrExportHelper.savePngToGallery(context, bmp)
                                }
                                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                exporting = false
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
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = if (bitmap != null && !exporting) White else TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (exporting) {
                            stringResource(R.string.qr_preview_saving)
                        } else {
                            stringResource(R.string.qr_preview_save_image)
                        },
                        color = if (bitmap != null && !exporting) White else TextTertiary,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }

            // Primary Save — Phase 7 persistence (payload unchanged by style)
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
                        context.runWithClickAd("QrPreviewScreen") {
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
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.qr_preview_toast_generated),
                                    Toast.LENGTH_SHORT
                                ).show()
                                onSavedAndDone(id)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    e.message ?: context.getString(R.string.qr_preview_toast_unable_save),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                saving = false
                            }
                        }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
//                    Icon(
//                        imageVector = if (historyId > 0L) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
//                        contentDescription = null,
//                        tint = if (saving || bitmap == null) TextTertiary else White,
//                        modifier = Modifier.size(20.dp)
//                    )
                    Text(
                        text = if (saving) {
                            stringResource(R.string.qr_preview_creating)
                        } else {
                            stringResource(R.string.qr_preview_create)
                        },
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
                    text = stringResource(R.string.qr_preview_code_specifications),
                    color = TextPrimary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    SpecCell(
                        stringResource(R.string.qr_preview_spec_export),
                        stringResource(R.string.qr_preview_spec_export_size),
                        Modifier.weight(1f)
                    )
                    SpecCell(
                        stringResource(R.string.qr_preview_spec_style),
                        stringResource(style.bodyPattern.labelRes),
                        Modifier.weight(1f)
                    )
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
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PreviewIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = TextPrimary
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
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}