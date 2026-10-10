package com.qrcode.scanner.ui.screens.create

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import com.qrcode.scanner.app.R
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qrcode.scanner.ui.theme.BorderSubtle
import com.qrcode.scanner.ui.theme.CardSurface
import com.qrcode.scanner.ui.theme.CobaltPrimary
import com.qrcode.scanner.ui.theme.CobaltSoft
import com.qrcode.scanner.ui.theme.Destructive
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
import kotlinx.coroutines.withContext

/**
 * Stitch: QR Customization Studio (White Theme) — d8bdbae6905f48d6a8b01c77ef61ab85
 * Applies style and returns to Preview (Save/Export live on Preview).
 */
class QrCustomizationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableThemedEdgeToEdge()
        bindScreenBackAd("QrCustomizationScreen")
        val payload = intent.getStringExtra(CreateQrIntents.EXTRA_PAYLOAD).orEmpty()
        val eccName = intent.getStringExtra(CreateQrIntents.EXTRA_ECC_LEVEL)
            ?: QrBitmapEncoder.EccLevel.H.name
        val ecc = QrBitmapEncoder.EccLevel.entries.firstOrNull { it.name == eccName }
            ?: QrBitmapEncoder.EccLevel.H
        @Suppress("DEPRECATION")
        val initialStyle = intent.getSerializableExtra(CreateQrIntents.EXTRA_STYLE) as? QrStyleConfig
            ?: QrStyleConfig.Default

        setContent {
            QRCodeScannerTheme {
                ScreenWithAd(screenKey = "QrCustomizationScreen", nativeSize = "small") {
                QrCustomizationScreen(
                    payload = payload,
                    ecc = ecc,
                    initialStyle = initialStyle,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onApply = { style ->
                        runWithClickAd("QrCustomizationScreen") {
                            setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra(CreateQrIntents.EXTRA_STYLE, style)
                            )
                            finish()
                        }
                    }
                )
                }
            }
        }
    }
}

@Composable
private fun QrCustomizationScreen(
    payload: String,
    ecc: QrBitmapEncoder.EccLevel,
    initialStyle: QrStyleConfig,
    onBack: () -> Unit,
    onApply: (QrStyleConfig) -> Unit
) {
    val context = LocalContext.current
    var style by remember { mutableStateOf(initialStyle) }
    var previewBmp by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var logoBmp by remember { mutableStateOf<Bitmap?>(null) }

    val pickLogo = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val decoded = BitmapFactory.decodeStream(stream) ?: return@use
                val prepared = QrStyledRenderer.prepareLogo(decoded, 256)
                val path = QrLogoStore.persist(context, prepared)
                logoBmp = prepared
                style = style.copy(
                    centerIcon = QrStyleConfig.CenterIcon.CUSTOM,
                    customLogoUri = path
                )
            }
        }.onFailure {
            Toast.makeText(context, context.getString(R.string.qr_customize_toast_unable_load_logo), Toast.LENGTH_SHORT)
                .show()
        }
    }

    LaunchedEffect(style, payload, ecc, logoBmp) {
        if (payload.isBlank()) {
            error = context.getString(R.string.qr_customize_nothing)
            previewBmp = null
            return@LaunchedEffect
        }
        val result = withContext(Dispatchers.Default) {
            val logo = when (style.centerIcon) {
                QrStyleConfig.CenterIcon.CUSTOM -> logoBmp
                else -> null
            }
            QrStyledRenderer.render(
                payload = payload,
                style = style,
                ecc = ecc,
                sizePx = 512,
                centerLogo = logo
            )
        }
        error = result.errorMessage
        previewBmp = result.bitmap
    }

    LaunchedEffect(initialStyle.customLogoUri, initialStyle.centerIcon) {
        if (initialStyle.centerIcon != QrStyleConfig.CenterIcon.CUSTOM) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
            QrLogoStore.load(context, initialStyle.customLogoUri)
        } ?: return@LaunchedEffect
        logoBmp = loaded
        val stored = initialStyle.customLogoUri
        if (!stored.isNullOrBlank() && !stored.startsWith("/")) {
            val path = withContext(Dispatchers.IO) { QrLogoStore.persist(context, loaded) }
            style = style.copy(customLogoUri = path)
        }
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
                .padding(start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppBackButton(onClick = onBack)
            Text(
                text = stringResource(R.string.qr_customize_title),
                color = TextPrimary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.action_reset),
                color = CobaltPrimary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    style = QrStyleConfig.Default
                    logoBmp = null
                    Toast.makeText(
                        context,
                        context.getString(R.string.qr_customize_toast_reset_done),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(NestedSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    error != null -> Text(
                        text = error!!,
                        color = Destructive,
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp
                    )
                    previewBmp != null -> Image(
                        bitmap = previewBmp!!.asImageBitmap(),
                        contentDescription = stringResource(R.string.cd_styled_qr_preview),
                        modifier = Modifier.fillMaxSize(0.92f),
                        contentScale = ContentScale.Fit
                    )
                    else -> Text(
                        text = stringResource(R.string.qr_customize_rendering),
                        color = TextTertiary,
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp
                    )
                }
            }

            // Body Pattern
            SectionCard(title = stringResource(R.string.qr_customize_section_body_pattern)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QrStyleConfig.BodyPattern.entries.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { pat ->
                                SelectChip(
                                    label = stringResource(pat.labelRes),
                                    selected = style.bodyPattern == pat,
                                    modifier = Modifier.weight(1f),
                                    onClick = { style = style.copy(bodyPattern = pat) }
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // Corner Eye
            SectionCard(title = stringResource(R.string.qr_customize_section_corner_eye)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QrStyleConfig.EyeStyle.entries.forEach { eye ->
                        SelectChip(
                            label = stringResource(eye.labelRes),
                            selected = style.eyeStyle == eye,
                            modifier = Modifier.weight(1f),
                            onClick = { style = style.copy(eyeStyle = eye) }
                        )
                    }
                }
            }

            // Color Palette — solid only
            SectionCard(title = stringResource(R.string.qr_customize_section_color_palette)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PaletteSwatch(
                        color = Color(QrStyleConfig.COLOR_BLACK),
                        selected = style.paletteKey == QrStyleConfig.PALETTE_BLACK,
                        onClick = {
                            style = style.copy(
                                paletteKey = QrStyleConfig.PALETTE_BLACK,
                                paletteName = context.getString(R.string.qr_palette_black),
                                foregroundColor = QrStyleConfig.COLOR_BLACK,
                                backgroundColor = QrStyleConfig.COLOR_WHITE
                            )
                        }
                    )
                    PaletteSwatch(
                        color = Color(QrStyleConfig.COLOR_COBALT),
                        selected = style.paletteKey == QrStyleConfig.PALETTE_COBALT,
                        onClick = {
                            style = style.copy(
                                paletteKey = QrStyleConfig.PALETTE_COBALT,
                                paletteName = context.getString(R.string.qr_palette_electric_cobalt),
                                foregroundColor = QrStyleConfig.COLOR_COBALT,
                                backgroundColor = QrStyleConfig.COLOR_WHITE
                            )
                        }
                    )
                    PaletteSwatch(
                        color = Color(QrStyleConfig.COLOR_SAPPHIRE),
                        selected = style.paletteKey == QrStyleConfig.PALETTE_SAPPHIRE,
                        onClick = {
                            style = style.copy(
                                paletteKey = QrStyleConfig.PALETTE_SAPPHIRE,
                                paletteName = context.getString(R.string.qr_palette_deep_sapphire),
                                foregroundColor = QrStyleConfig.COLOR_SAPPHIRE,
                                backgroundColor = QrStyleConfig.COLOR_WHITE
                            )
                        }
                    )
                    PaletteSwatch(
                        color = Color(QrStyleConfig.COLOR_CYAN),
                        selected = style.paletteKey == QrStyleConfig.PALETTE_CYAN,
                        onClick = {
                            style = style.copy(
                                paletteKey = QrStyleConfig.PALETTE_CYAN,
                                paletteName = context.getString(R.string.qr_palette_neon_cyan),
                                foregroundColor = QrStyleConfig.COLOR_CYAN,
                                backgroundColor = QrStyleConfig.COLOR_WHITE
                            )
                        }
                    )
                    PaletteSwatch(
                        color = Color(QrStyleConfig.COLOR_SLATE),
                        selected = style.paletteKey == QrStyleConfig.PALETTE_SLATE,
                        onClick = {
                            style = style.copy(
                                paletteKey = QrStyleConfig.PALETTE_SLATE,
                                paletteName = context.getString(R.string.qr_palette_monochrome_slate),
                                foregroundColor = QrStyleConfig.COLOR_SLATE,
                                backgroundColor = QrStyleConfig.COLOR_WHITE
                            )
                        }
                    )
                    ExtraPalettes.forEach { extra ->
                        PaletteSwatch(
                            color = Color(extra.color),
                            selected = style.paletteKey == extra.key,
                            onClick = {
                                style = style.copy(
                                    paletteKey = extra.key,
                                    paletteName = context.getString(extra.nameRes),
                                    foregroundColor = extra.color,
                                    backgroundColor = QrStyleConfig.COLOR_WHITE
                                )
                            }
                        )
                    }
                }
            }

            // Center icon
            SectionCard(title = stringResource(R.string.qr_customize_section_center_icon)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconOption(
                        label = stringResource(R.string.qr_icon_none),
                        selected = style.centerIcon == QrStyleConfig.CenterIcon.NONE,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            style = style.copy(centerIcon = QrStyleConfig.CenterIcon.NONE, customLogoUri = null)
                            logoBmp = null
                        }
                    ) {
                        Text("—", color = TextSecondary, fontWeight = FontWeight.Bold)
                    }
                    IconOption(
                        label = stringResource(R.string.qr_icon_wifi),
                        selected = style.centerIcon == QrStyleConfig.CenterIcon.WIFI,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            style = style.copy(centerIcon = QrStyleConfig.CenterIcon.WIFI, customLogoUri = null)
                        }
                    ) {
                        Icon(Icons.Outlined.Wifi, null, tint = CobaltPrimary, modifier = Modifier.size(22.dp))
                    }
                    IconOption(
                        label = stringResource(R.string.qr_icon_globe),
                        selected = style.centerIcon == QrStyleConfig.CenterIcon.GLOBE,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            style = style.copy(centerIcon = QrStyleConfig.CenterIcon.GLOBE, customLogoUri = null)
                        }
                    ) {
                        Icon(Icons.Outlined.Public, null, tint = CobaltPrimary, modifier = Modifier.size(22.dp))
                    }
                    IconOption(
                        label = stringResource(R.string.qr_icon_custom),
                        selected = style.centerIcon == QrStyleConfig.CenterIcon.CUSTOM,
                        modifier = Modifier.weight(1f),
                        onClick = { pickLogo.launch("image/*") }
                    ) {
                        Icon(Icons.Outlined.Upload, null, tint = CobaltPrimary, modifier = Modifier.size(22.dp))
                    }
                }
            }

            // Frame
            SectionCard(title = stringResource(R.string.qr_customize_section_frame)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    QrStyleConfig.FrameTemplate.entries.forEach { frame ->
                        val selected = style.frame == frame
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) CobaltSoft else NestedSurface)
                                .border(
                                    1.dp,
                                    if (selected) CobaltPrimary else BorderSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { style = style.copy(frame = frame) }
                                .padding(horizontal = 14.dp, vertical = 16.dp)
                        ) {
                            Text(
                                text = stringResource(frame.labelRes),
                                color = if (selected) CobaltPrimary else TextPrimary,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = stringResource(frame.subtitleRes),
                                color = TextSecondary,
                                fontFamily = PlusJakartaSans,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            if (error != null) {
                Text(
                    text = error!!,
                    color = Destructive,
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PageBackground)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (previewBmp == null || error != null) NestedSurface else CobaltPrimary)
                .clickable(
                    enabled = previewBmp != null && error == null,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onApply(style) },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = if (previewBmp == null || error != null) TextTertiary else White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.qr_customize_save_export),
                    color = if (previewBmp == null || error != null) TextTertiary else White,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

private data class ExtraPalette(val key: String, @StringRes val nameRes: Int, val color: Int)

private val ExtraPalettes = listOf(
    ExtraPalette("crimson", R.string.qr_palette_crimson, 0xFFDC2626.toInt()),
    ExtraPalette("emerald", R.string.qr_palette_emerald, 0xFF059669.toInt()),
    ExtraPalette("violet", R.string.qr_palette_violet, 0xFF7C3AED.toInt()),
    ExtraPalette("amber", R.string.qr_palette_amber, 0xFFD97706.toInt())
)

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface, RoundedCornerShape(16.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
        content()
    }
}

@Composable
private fun SelectChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .background(if (selected) CobaltSoft else NestedSurface, RoundedCornerShape(12.dp))
            .border(1.dp, if (selected) CobaltPrimary else BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) CobaltPrimary else TextPrimary,
            fontFamily = PlusJakartaSans,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            style = androidx.compose.ui.text.TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = true)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PaletteSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(if (selected) 48.dp else 44.dp)
            .clip(CircleShape)
            .border(2.dp, if (selected) CobaltPrimary else BorderSubtle, CircleShape)
            .padding(3.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(Icons.Outlined.Check, null, tint = White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun IconOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) CobaltSoft else NestedSurface)
            .border(1.dp, if (selected) CobaltPrimary else BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        iconContent()
        Text(
            text = label,
            color = if (selected) CobaltPrimary else TextPrimary,
            fontFamily = PlusJakartaSans,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}
