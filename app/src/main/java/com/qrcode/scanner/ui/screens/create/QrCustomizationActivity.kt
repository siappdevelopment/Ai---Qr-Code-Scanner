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
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RestartAlt
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
        enableEdgeToEdge()
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
                QrCustomizationScreen(
                    payload = payload,
                    ecc = ecc,
                    initialStyle = initialStyle,
                    onBack = { finish() },
                    onApply = { style ->
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(CreateQrIntents.EXTRA_STYLE, style)
                        )
                        finish()
                    }
                )
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
                val decoded = BitmapFactory.decodeStream(stream)
                logoBmp = decoded?.let { QrStyledRenderer.prepareLogo(it, 256) }
                style = style.copy(
                    centerIcon = QrStyleConfig.CenterIcon.CUSTOM,
                    customLogoUri = uri.toString()
                )
            }
        }.onFailure {
            Toast.makeText(context, "Unable to load logo", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(style, payload, ecc, logoBmp) {
        if (payload.isBlank()) {
            error = "Nothing to customize"
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

    // Load existing custom logo URI once
    LaunchedEffect(initialStyle.customLogoUri) {
        val uriStr = initialStyle.customLogoUri ?: return@LaunchedEffect
        if (initialStyle.centerIcon != QrStyleConfig.CenterIcon.CUSTOM) return@LaunchedEffect
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { stream ->
                logoBmp = BitmapFactory.decodeStream(stream)?.let {
                    QrStyledRenderer.prepareLogo(it, 256)
                }
            }
        }
    }

    val canvasBg = if (style.previewDarkCanvas) Color(0xFF0F172A) else NestedSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .navigationBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .appHeaderBackground()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppBackButton(onClick = onBack)
                Text(
                    text = "Customize QR",
                    color = TextPrimary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Light / Dark canvas + Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(NestedSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ThemeChip(
                        selected = !style.previewDarkCanvas,
                        icon = Icons.Outlined.LightMode,
                        label = "Light",
                        onClick = { style = style.copy(previewDarkCanvas = false) }
                    )
                    ThemeChip(
                        selected = style.previewDarkCanvas,
                        icon = Icons.Outlined.DarkMode,
                        label = "Dark",
                        onClick = { style = style.copy(previewDarkCanvas = true) }
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NestedSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            style = QrStyleConfig.Default
                            logoBmp = null
                            Toast.makeText(context, "Reset to Electric Cobalt", Toast.LENGTH_SHORT)
                                .show()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Outlined.RestartAlt, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Text("Reset", color = TextSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                }
            }

            Text(
                text = "Live preview",
                color = TextTertiary,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp
            )

            // Live preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(canvasBg)
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
                        contentDescription = "Styled QR preview",
                        modifier = Modifier.fillMaxSize(0.92f),
                        contentScale = ContentScale.Fit
                    )
                    else -> Text(
                        text = "Rendering…",
                        color = TextTertiary,
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp
                    )
                }
            }

            // Body Pattern
            SectionCard(title = "Body Pattern", trailing = style.bodyPattern.label) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QrStyleConfig.BodyPattern.entries.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { pat ->
                                SelectChip(
                                    label = pat.label,
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
            SectionCard(title = "Corner Eye Style", trailing = style.eyeStyle.label) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QrStyleConfig.EyeStyle.entries.forEach { eye ->
                        SelectChip(
                            label = eye.label,
                            selected = style.eyeStyle == eye,
                            modifier = Modifier.weight(1f),
                            onClick = { style = style.copy(eyeStyle = eye) }
                        )
                    }
                }
            }

            // Color Palette — solid only
            SectionCard(title = "Color Palette", trailing = style.paletteName) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PaletteSwatch(
                        color = Color(QrStyleConfig.COLOR_COBALT),
                        selected = style.paletteKey == QrStyleConfig.PALETTE_COBALT,
                        onClick = {
                            style = style.copy(
                                paletteKey = QrStyleConfig.PALETTE_COBALT,
                                paletteName = "Electric Cobalt",
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
                                paletteName = "Deep Sapphire",
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
                                paletteName = "Neon Cyan",
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
                                paletteName = "Monochrome Slate",
                                foregroundColor = QrStyleConfig.COLOR_SLATE,
                                backgroundColor = QrStyleConfig.COLOR_WHITE
                            )
                        }
                    )
                    // Custom = reuse cobalt as base; simple alternate pick via long-press not available —
                    // offer a few solid extras as "Custom" deep blue / black
                    PaletteSwatch(
                        color = Color(0xFF002080),
                        selected = style.paletteKey == QrStyleConfig.PALETTE_CUSTOM,
                        onClick = {
                            style = style.copy(
                                paletteKey = QrStyleConfig.PALETTE_CUSTOM,
                                paletteName = "Custom (#002080)",
                                foregroundColor = 0xFF002080.toInt(),
                                backgroundColor = QrStyleConfig.COLOR_WHITE
                            )
                        },
                        showPlus = true
                    )
                }
            }

            // Center icon
            SectionCard(title = "Center Icon", trailing = style.centerIcon.label) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconOption(
                        label = "None",
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
                        label = "Wi-Fi",
                        selected = style.centerIcon == QrStyleConfig.CenterIcon.WIFI,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            style = style.copy(centerIcon = QrStyleConfig.CenterIcon.WIFI, customLogoUri = null)
                        }
                    ) {
                        Icon(Icons.Outlined.Wifi, null, tint = CobaltPrimary, modifier = Modifier.size(22.dp))
                    }
                    IconOption(
                        label = "Globe",
                        selected = style.centerIcon == QrStyleConfig.CenterIcon.GLOBE,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            style = style.copy(centerIcon = QrStyleConfig.CenterIcon.GLOBE, customLogoUri = null)
                        }
                    ) {
                        Icon(Icons.Outlined.Public, null, tint = CobaltPrimary, modifier = Modifier.size(22.dp))
                    }
                    IconOption(
                        label = "Custom",
                        selected = style.centerIcon == QrStyleConfig.CenterIcon.CUSTOM,
                        modifier = Modifier.weight(1f),
                        onClick = { pickLogo.launch("image/*") }
                    ) {
                        Icon(Icons.Outlined.Upload, null, tint = CobaltPrimary, modifier = Modifier.size(22.dp))
                    }
                }
            }

            // Frame
            SectionCard(title = "Frame Template", trailing = style.frame.label) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                .padding(12.dp)
                        ) {
                            Text(
                                text = frame.label,
                                color = if (selected) CobaltPrimary else TextPrimary,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = frame.subtitle,
                                color = TextSecondary,
                                fontFamily = PlusJakartaSans,
                                fontSize = 12.sp
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

            // Apply → return to Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                        text = "Save & Export Custom QR",
                        color = if (previewBmp == null || error != null) TextTertiary else White,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
            Text(
                text = "Returns to Preview where you can save, share, or export.",
                color = TextTertiary,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ThemeChip(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) White else Color.Transparent)
            .border(1.dp, if (selected) BorderSubtle else Color.Transparent, RoundedCornerShape(999.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            null,
            tint = if (selected) CobaltPrimary else TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            label,
            color = if (selected) TextPrimary else TextSecondary,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    trailing: String,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = trailing,
                color = CobaltPrimary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(140.dp)
            )
        }
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
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) CobaltSoft else NestedSurface)
            .border(1.dp, if (selected) CobaltPrimary else BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) CobaltPrimary else TextPrimary,
            fontFamily = PlusJakartaSans,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 2
        )
    }
}

@Composable
private fun PaletteSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    showPlus: Boolean = false
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
        } else if (showPlus) {
            Icon(Icons.Outlined.Palette, null, tint = White, modifier = Modifier.size(18.dp))
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
