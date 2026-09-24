package com.qrcode.scanner.ui.screens.scan

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.ViewWeek
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Stitch source: Scan Result & Details — a6b012429174465a85b4e14fc9ebc961
 * Visual theme: White + Electric Cobalt (no gradients / shadows / Profile / Ask AI).
 * Persists to Room on first valid result; favorite writes through the repository.
 */
class ScanResultActivity : ComponentActivity() {

    private var savedHistoryId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        savedHistoryId = savedInstanceState?.getLong(KEY_HISTORY_ID, -1L) ?: -1L
        val rawValue = intent.getStringExtra(ScanIntents.EXTRA_RAW_VALUE).orEmpty()
        val formatName = intent.getStringExtra(ScanIntents.EXTRA_BARCODE_FORMAT_NAME)
            ?: "UNKNOWN"
        val format = intent.getIntExtra(ScanIntents.EXTRA_BARCODE_FORMAT, -1)
        setContent {
            QRCodeScannerTheme {
                ScanResultRoute(
                    rawValue = rawValue,
                    formatName = formatName,
                    format = format,
                    initialHistoryId = savedHistoryId,
                    onHistoryIdAssigned = { savedHistoryId = it },
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_HISTORY_ID, savedHistoryId)
    }

    companion object {
        private const val KEY_HISTORY_ID = "key_history_id"
    }
}

@Composable
private fun ScanResultRoute(
    rawValue: String,
    formatName: String,
    format: Int,
    initialHistoryId: Long,
    onHistoryIdAssigned: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { HistoryRepositoryProvider.get(context) }
    val scope = rememberCoroutineScope()
    var historyId by remember { mutableLongStateOf(initialHistoryId) }
    var favorite by remember { mutableStateOf(false) }
    var scannedAtMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var ready by remember { mutableStateOf(initialHistoryId > 0L) }

    val detectedType = remember(rawValue, format, formatName) {
        ScanPayloadMapper.detectType(rawValue, format, formatName)
    }

    LaunchedEffect(rawValue, format, formatName, initialHistoryId) {
        if (rawValue.isBlank()) {
            ready = true
            return@LaunchedEffect
        }
        if (historyId > 0L) {
            val existing = repository.getById(historyId)
            if (existing != null) {
                favorite = existing.isFavorite
                scannedAtMillis = existing.timestamp
            }
            ready = true
            return@LaunchedEffect
        }
        val id = repository.insertScanAvoidingDuplicate(
            rawValue = rawValue,
            barcodeFormat = format,
            barcodeFormatName = formatName,
            detectedType = detectedType
        )
        historyId = id
        onHistoryIdAssigned(id)
        val saved = repository.getById(id)
        if (saved != null) {
            favorite = saved.isFavorite
            scannedAtMillis = saved.timestamp
        }
        ready = true
    }

    if (!ready) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PageBackground)
        )
        return
    }

    ScanResultScreen(
        rawValue = rawValue,
        formatName = formatName,
        format = format,
        historyId = historyId,
        initialFavorite = favorite,
        scannedAtMillis = scannedAtMillis,
        onBack = onBack,
        onFavoriteChange = { next ->
            favorite = next
            scope.launch {
                repository.updateFavorite(historyId, next)
                Toast.makeText(
                    context,
                    if (next) "Saved to your favorites" else "Removed from favorites",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )
}

private enum class ResultKind(
    val label: String,
    val icon: ImageVector
) {
    Website("Website / URL", Icons.Outlined.Language),
    Wifi("Wi-Fi Network", Icons.Outlined.Wifi),
    PlainText("Plain Text", Icons.Outlined.Description),
    QrCode("QR Code", Icons.Outlined.QrCode2),
    Barcode("Barcode", Icons.Outlined.ViewWeek)
}

private fun resolveKind(detectedType: String): ResultKind = when (detectedType) {
    ScanPayloadMapper.TYPE_WEBSITE -> ResultKind.Website
    ScanPayloadMapper.TYPE_WIFI -> ResultKind.Wifi
    ScanPayloadMapper.TYPE_BARCODE -> ResultKind.Barcode
    ScanPayloadMapper.TYPE_QR_CODE -> ResultKind.QrCode
    else -> ResultKind.PlainText
}

@Composable
fun ScanResultScreen(
    rawValue: String,
    formatName: String,
    format: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    historyId: Long = -1L,
    initialFavorite: Boolean = false,
    scannedAtMillis: Long = System.currentTimeMillis(),
    title: String = "Scan Result",
    onFavoriteChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val detectedType = remember(rawValue, format, formatName) {
        ScanPayloadMapper.detectType(rawValue, format, formatName)
    }
    val kind = remember(detectedType) { resolveKind(detectedType) }
    val isUrl = kind == ResultKind.Website
    var favorite by remember(historyId) { mutableStateOf(initialFavorite) }
    LaunchedEffect(initialFavorite) { favorite = initialFavorite }
    val scannedAt = remember(scannedAtMillis) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(scannedAtMillis))
    }
    val displayValue = rawValue.ifBlank { "(empty)" }
    val copyLabel = if (isUrl) "Copy Link" else "Copy"
    val openLabel = if (isUrl) "Open in Browser" else null

    fun toggleFavorite() {
        val next = !favorite
        favorite = next
        onFavoriteChange(next)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HeaderIconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Go back",
                    onClick = onBack
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    letterSpacing = (-0.2).sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderIconButton(
                    icon = if (favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Add to favorites",
                    tint = if (favorite) CobaltPrimary else TextSecondary,
                    onClick = { toggleFavorite() }
                )
                HeaderIconButton(
                    icon = Icons.Outlined.Share,
                    contentDescription = "Share result",
                    onClick = { shareResult(context, displayValue, isUrl) }
                )
                HeaderIconButton(
                    icon = Icons.Outlined.MoreVert,
                    contentDescription = "More options",
                    onClick = { /* Stitch control retained */ }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSurface, RoundedCornerShape(18.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .background(CobaltSoft, RoundedCornerShape(999.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = kind.icon,
                            contentDescription = null,
                            tint = CobaltPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = kind.label,
                            color = CobaltPrimary,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = scannedAt,
                            color = TextSecondary,
                            fontFamily = PlusJakartaSans,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NestedSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PAYLOAD CONTENT",
                            color = TextSecondary,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            letterSpacing = 0.6.sp
                        )
                        if (isUrl) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = displayValue,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                if (isUrl) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CobaltSoft, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            tint = CobaltPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Website / URL payload detected",
                            color = TextPrimary,
                            fontFamily = PlusJakartaSans,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (openLabel != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CobaltPrimary)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            openUrl(context, displayValue)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = openLabel,
                            color = White,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Launch,
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryAction(
                    label = copyLabel,
                    icon = Icons.Outlined.ContentCopy,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        copyToClipboard(context, displayValue)
                        Toast.makeText(
                            context,
                            if (isUrl) "Link copied to clipboard" else "Copied to clipboard",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                SecondaryAction(
                    label = "Share",
                    icon = Icons.Outlined.Share,
                    modifier = Modifier.weight(1f),
                    onClick = { shareResult(context, displayValue, isUrl) }
                )
                SecondaryAction(
                    label = if (favorite) "Saved" else "Save Fav",
                    icon = if (favorite) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    modifier = Modifier.weight(1f),
                    onClick = { toggleFavorite() }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "PAYLOAD TECHNICALS",
                    color = TextSecondary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 0.6.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TechCell(
                        label = "Format",
                        value = formatName,
                        modifier = Modifier.weight(1f)
                    )
                    TechCell(
                        label = "Type",
                        value = detectedType.take(12),
                        modifier = Modifier.weight(1f)
                    )
                    TechCell(
                        label = "Length",
                        value = "${rawValue.length} chars",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderIconButton(
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

@Composable
private fun SecondaryAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(NestedSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CobaltPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = TextPrimary,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TechCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(NestedSurface)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = TextTertiary,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("scan_result", text))
}

private fun shareResult(context: Context, text: String, isUrl: Boolean) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_TITLE, if (isUrl) "Scanned URL" else "Scan Result")
    }
    context.startActivity(Intent.createChooser(send, "Share"))
}

private fun openUrl(context: Context, raw: String) {
    try {
        val uri = Uri.parse(ScanPayloadMapper.normalizeUrl(raw))
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
    }
}
