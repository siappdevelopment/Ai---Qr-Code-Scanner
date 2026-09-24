package com.qrcode.scanner.ui.screens.home

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AddToPhotos
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.FilterNone
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrcode.scanner.data.history.HistoryEntity
import com.qrcode.scanner.data.history.HistoryRepository
import com.qrcode.scanner.data.history.HistoryRepositoryProvider
import com.qrcode.scanner.data.history.ScanPayloadMapper
import com.qrcode.scanner.ui.theme.CobaltAccent
import com.qrcode.scanner.ui.theme.CobaltPrimary
import com.qrcode.scanner.ui.theme.PlusJakartaSans
import com.qrcode.scanner.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Stitch source: Home Dashboard (White Theme)
 * Screen ID: 3e63f2d09c78452fa9ff5323e3aff6fd
 *
 * Font (from Stitch CSS): Plus Jakarta Sans, Inter fallback.
 * Solid colors only; borders instead of shadows/elevation; no gradients.
 * Ask AI omitted. BottomNavigation from ScanPulseBottomBar.
 */
@Composable
fun HomeScreen(
    onOpenScanner: () -> Unit = {},
    onScanBarcode: () -> Unit = {},
    onScanGallery: () -> Unit = {},
    onCreateQr: () -> Unit = {},
    onBatchScanner: () -> Unit = {},
    onViewAllHistory: () -> Unit = {},
    onRecentItemClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var flashlightOn by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeColors.Page)
    ) {
        HomeTopBar()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            BrandUtilityRow(
                flashlightOn = flashlightOn,
                onToggleFlashlight = { flashlightOn = !flashlightOn }
            )
            InstantScannerHero(onOpenScanner = onOpenScanner)
            QuickToolsSection(
                onScanBarcode = onScanBarcode,
                onScanGallery = onScanGallery,
                onCreateQr = onCreateQr,
                onBatchScanner = onBatchScanner
            )
            RecentScansSection(
                onViewAll = onViewAllHistory,
                onItemClick = onRecentItemClick
            )
        }
    }
}

@Composable
private fun HomeTopBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(HomeColors.IconWell, RoundedCornerShape(8.dp))
                        .border(1.dp, HomeColors.IconWellBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        tint = CobaltPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Home",
                    color = HomeColors.OnSurface,
                    fontFamily = PlusJakartaSans,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp
                )
            }
            // Ask AI (auto_awesome) intentionally omitted.
            Spacer(modifier = Modifier.size(44.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(HomeColors.Outline)
        )
    }
}

@Composable
private fun BrandUtilityRow(
    flashlightOn: Boolean,
    onToggleFlashlight: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "ScanPulse",
                color = HomeColors.OnSurface,
                fontFamily = PlusJakartaSans,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 24.sp
            )
            // Stitch PRO / Premium badge (only subscription-related UI on this Home screen)
            Box(
                modifier = Modifier
                    .background(HomeColors.ProBadgeBg, RoundedCornerShape(999.dp))
                    .border(1.dp, CobaltAccent.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PRO",
                    color = CobaltPrimary,
                    fontFamily = PlusJakartaSans,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    lineHeight = 14.sp
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UtilityIconButton(
                onClick = onToggleFlashlight,
                background = if (flashlightOn) CobaltPrimary else White
            ) {
                Icon(
                    imageVector = if (flashlightOn) Icons.Outlined.FlashlightOn else Icons.Outlined.FlashlightOff,
                    contentDescription = "Flashlight",
                    tint = if (flashlightOn) White else HomeColors.OnSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Box {
                UtilityIconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = HomeColors.OnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 10.dp)
                        .size(8.dp)
                        .background(CobaltPrimary, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun UtilityIconButton(
    onClick: () -> Unit,
    background: Color = White,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(background, RoundedCornerShape(12.dp))
            .border(1.dp, HomeColors.Outline, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun InstantScannerHero(onOpenScanner: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(24.dp))
            .border(1.dp, HomeColors.Outline, RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScannerReticle()
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Instant Scanner",
            color = HomeColors.OnSurface,
            fontFamily = PlusJakartaSans,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Point at any QR code or standard barcode",
            color = HomeColors.OnSurfaceVariant,
            fontFamily = PlusJakartaSans,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(CobaltPrimary, RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpenScanner
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = null,
                tint = White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Open Camera Scanner",
                color = White,
                fontFamily = PlusJakartaSans,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun ScannerReticle() {
    Box(
        modifier = Modifier.size(112.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            val arm = 20.dp.toPx()
            val inset = 4.dp.toPx()
            val cobalt = Color(0xFF0033CC)
            val cyan = Color(0xFF00B4FF)

            fun corner(x: Float, y: Float, dx1: Float, dy1: Float, dx2: Float, dy2: Float, color: Color) {
                drawLine(color, Offset(x, y), Offset(x + dx1, y + dy1), stroke, StrokeCap.Round)
                drawLine(color, Offset(x, y), Offset(x + dx2, y + dy2), stroke, StrokeCap.Round)
            }

            corner(inset, inset, arm, 0f, 0f, arm, cobalt)
            corner(size.width - inset, inset, -arm, 0f, 0f, arm, cobalt)
            corner(inset, size.height - inset, arm, 0f, 0f, -arm, cyan)
            corner(size.width - inset, size.height - inset, -arm, 0f, 0f, -arm, cyan)

            val lineY = size.height * 0.48f
            drawLine(
                color = cyan,
                start = Offset(8.dp.toPx(), lineY),
                end = Offset(size.width - 8.dp.toPx(), lineY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(HomeColors.IconWell, RoundedCornerShape(12.dp))
                .border(1.dp, HomeColors.IconWellBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.QrCode2,
                contentDescription = null,
                tint = CobaltPrimary,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun QuickToolsSection(
    onScanBarcode: () -> Unit,
    onScanGallery: () -> Unit,
    onCreateQr: () -> Unit,
    onBatchScanner: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "QUICK TOOLS",
            color = HomeColors.OnSurfaceVariant,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            lineHeight = 16.sp
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickToolCard(
                    title = "Scan Barcode",
                    subtitle = "UPC & EAN tags",
                    icon = Icons.Outlined.DocumentScanner,
                    onClick = onScanBarcode,
                    modifier = Modifier.weight(1f)
                )
                QuickToolCard(
                    title = "Scan Gallery",
                    subtitle = "From screenshots",
                    icon = Icons.Outlined.Image,
                    onClick = onScanGallery,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickToolCard(
                    title = "Create QR",
                    subtitle = "Links, Wi-Fi & vCard",
                    icon = Icons.Outlined.AddToPhotos,
                    onClick = onCreateQr,
                    modifier = Modifier.weight(1f)
                )
                QuickToolCard(
                    title = "Batch Scanner",
                    subtitle = "Multi-code mode",
                    icon = Icons.Outlined.FilterNone,
                    onClick = onBatchScanner,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Stitch Quick Tool card: min-h-[96px] + p-space-md (16dp).
 * Fixed height(96.dp) clipped title/subtitle — use heightIn(min) instead.
 */
@Composable
private fun QuickToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(min = 96.dp)
            .background(White, RoundedCornerShape(18.dp))
            .border(1.dp, HomeColors.Outline, RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(HomeColors.IconWell, RoundedCornerShape(12.dp))
                .border(1.dp, HomeColors.IconWellBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CobaltPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = HomeColors.OnSurface,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp
            )
            Text(
                text = subtitle,
                color = HomeColors.Meta,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Stitch Home shows 3 recent items (Coffee Shop / GitHub / FedEx in mock).
 * Wired to Room — never shows fake default data.
 */
@Composable
private fun RecentScansSection(
    onViewAll: () -> Unit,
    onItemClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { HistoryRepositoryProvider.get(context) }
    val recent by repository
        .observeRecent(HistoryRepository.RECENT_HOME_LIMIT)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Scans",
                color = HomeColors.OnSurface,
                fontFamily = PlusJakartaSans,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 24.sp
            )
            Row(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onViewAll
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View all",
                    color = CobaltPrimary,
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = CobaltPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (recent.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White, RoundedCornerShape(18.dp))
                    .border(1.dp, HomeColors.Outline, RoundedCornerShape(18.dp))
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(HomeColors.IconWell, RoundedCornerShape(12.dp))
                        .border(1.dp, HomeColors.IconWellBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = CobaltPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "No recent scans yet",
                    color = HomeColors.OnSurface,
                    fontFamily = PlusJakartaSans,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Codes you scan will appear here for quick access.",
                    color = HomeColors.OnSurfaceVariant,
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White, RoundedCornerShape(18.dp))
                    .border(1.dp, HomeColors.Outline, RoundedCornerShape(18.dp))
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                recent.forEachIndexed { index, entity ->
                    RecentScanRow(
                        entity = entity,
                        onClick = { onItemClick(entity.id) }
                    )
                    if (index < recent.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(1.dp)
                                .background(HomeColors.Outline)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentScanRow(
    entity: HistoryEntity,
    onClick: () -> Unit
) {
    val title = ScanPayloadMapper.titleFor(entity)
    val subtitle = ScanPayloadMapper.subtitleFor(entity)
    val timeLabel = remember(entity.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entity.timestamp))
    }
    val rowIcon = when (entity.detectedType) {
        ScanPayloadMapper.TYPE_WIFI -> Icons.Outlined.Wifi
        ScanPayloadMapper.TYPE_WEBSITE -> Icons.Outlined.Language
        ScanPayloadMapper.TYPE_BARCODE -> Icons.Outlined.ViewWeek
        ScanPayloadMapper.TYPE_QR_CODE -> Icons.Outlined.QrCode2
        else -> Icons.Outlined.Description
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(HomeColors.IconWell, RoundedCornerShape(12.dp))
                .border(1.dp, HomeColors.IconWellBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = rowIcon,
                contentDescription = null,
                tint = CobaltPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = HomeColors.OnSurface,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = HomeColors.OnSurfaceVariant,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = timeLabel,
            color = HomeColors.Meta,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private object HomeColors {
    val Page = Color(0xFFF7F9FA)
    val OnSurface = Color(0xFF111827)
    val OnSurfaceVariant = Color(0xFF4B5563)
    val Outline = Color(0xFFE5E8EB)
    val IconWell = Color(0xFFF0F5FF)
    val IconWellBorder = Color(0xFFD0E2FF)
    val ProBadgeBg = Color(0xFFEBF3FF)
    val Meta = Color(0xFF6B7280)
}