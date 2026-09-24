package com.qrcode.scanner.ui.screens.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrcode.scanner.data.history.HistoryEntity
import com.qrcode.scanner.data.history.HistoryRepositoryProvider
import com.qrcode.scanner.data.history.ScanPayloadMapper
import com.qrcode.scanner.ui.theme.BorderSubtle
import com.qrcode.scanner.ui.theme.CardSurface
import com.qrcode.scanner.ui.theme.CobaltPrimary
import com.qrcode.scanner.ui.theme.CobaltSoft
import com.qrcode.scanner.ui.theme.Destructive
import com.qrcode.scanner.ui.theme.NestedSurface
import com.qrcode.scanner.ui.theme.PageBackground
import com.qrcode.scanner.ui.theme.PlusJakartaSans
import com.qrcode.scanner.ui.theme.TextPrimary
import com.qrcode.scanner.ui.theme.TextSecondary
import com.qrcode.scanner.ui.theme.TextTertiary
import com.qrcode.scanner.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Stitch: History & Activity (White Theme) — f47f3c476e894ddc97271223d29106e0
 * Empty: e63b148524174fd1ab5cd0ea369e5e1e
 * Clear dialog: 8916777bbd734cc2bb269d0e099f12dc
 *
 * White + Electric Cobalt. No gradients / shadows / Profile / Ask AI.
 */
private enum class HistoryFilter { All, Scanned, Created }

@Composable
fun HistoryScreen(
    onOpenScanner: () -> Unit = {},
    onOpenDetail: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { HistoryRepositoryProvider.get(context) }
    val scope = rememberCoroutineScope()

    val allItems by repository.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val scannedCount by repository.observeScannedCount()
        .collectAsStateWithLifecycle(initialValue = 0)
    val createdCount by repository.observeCreatedCount()
        .collectAsStateWithLifecycle(initialValue = 0)

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(HistoryFilter.All) }
    var showClearDialog by remember { mutableStateOf(false) }

    val filtered = remember(allItems, query, filter) {
        allItems
            .asSequence()
            .filter { item ->
                when (filter) {
                    HistoryFilter.All -> true
                    HistoryFilter.Scanned -> item.source == HistoryEntity.SOURCE_SCANNED
                    HistoryFilter.Created -> item.source == HistoryEntity.SOURCE_CREATED
                }
            }
            .filter { item ->
                if (query.isBlank()) true
                else {
                    val q = query.trim()
                    item.rawValue.contains(q, ignoreCase = true) ||
                        item.detectedType.contains(q, ignoreCase = true) ||
                        item.barcodeFormatName.contains(q, ignoreCase = true) ||
                        ScanPayloadMapper.titleFor(item).contains(q, ignoreCase = true)
                }
            }
            .toList()
    }

    val sections = remember(filtered) { groupByDay(filtered) }
    val isEmpty = allItems.isEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Scan History",
                    color = TextPrimary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    letterSpacing = (-0.2).sp
                )
                Text(
                    text = if (isEmpty) {
                        "Offline Vault Active · 0 Scans Recorded"
                    } else {
                        "${allItems.size} items stored locally"
                    },
                    color = TextSecondary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isEmpty) NestedSurface else CobaltSoft)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                    .clickable(
                        enabled = !isEmpty,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showClearDialog = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Clear",
                    color = if (isEmpty) TextTertiary else CobaltPrimary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }

        if (isEmpty) {
            HistoryEmptyState(onLaunchScanner = onOpenScanner)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    enabled = true
                )
                FilterTabs(
                    filter = filter,
                    allCount = allItems.size,
                    scannedCount = scannedCount,
                    createdCount = createdCount,
                    onFilterChange = { filter = it }
                )
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No matches for your search",
                        color = TextSecondary,
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    sections.forEach { section ->
                        item(key = "header_${section.label}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = section.label,
                                    color = TextPrimary,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = section.countLabel,
                                    color = TextSecondary,
                                    fontFamily = PlusJakartaSans,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        items(section.items, key = { it.id }) { entity ->
                            HistoryActivityCard(
                                entity = entity,
                                onClick = { onOpenDetail(entity.id) },
                                onCopy = {
                                    copyText(context, entity.rawValue)
                                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                },
                                onShare = { shareText(context, entity.rawValue) },
                                onDelete = {
                                    scope.launch { repository.deleteById(entity.id) }
                                },
                                onToggleFavorite = {
                                    scope.launch {
                                        repository.updateFavorite(entity.id, !entity.isFavorite)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        ClearHistoryDialog(
            itemCount = allItems.size,
            onConfirm = {
                showClearDialog = false
                scope.launch {
                    repository.clearHistoryKeepingFavorites()
                    Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showClearDialog = false }
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(White)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            enabled = enabled,
            singleLine = true,
            cursorBrush = SolidColor(CobaltPrimary),
            textStyle = TextStyle(
                color = TextPrimary,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = "Search codes, URLs, titles...",
                        color = TextTertiary,
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp
                    )
                }
                inner()
            }
        )
        if (query.isNotEmpty()) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Clear search",
                tint = TextTertiary,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onQueryChange("") }
            )
        }
    }
}

@Composable
private fun FilterTabs(
    filter: HistoryFilter,
    allCount: Int,
    scannedCount: Int,
    createdCount: Int,
    onFilterChange: (HistoryFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NestedSurface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterTab(
            label = "All ($allCount)",
            selected = filter == HistoryFilter.All,
            onClick = { onFilterChange(HistoryFilter.All) },
            modifier = Modifier.weight(1f)
        )
        FilterTab(
            label = "Scanned ($scannedCount)",
            selected = filter == HistoryFilter.Scanned,
            onClick = { onFilterChange(HistoryFilter.Scanned) },
            modifier = Modifier.weight(1f)
        )
        FilterTab(
            label = "Created ($createdCount)",
            selected = filter == HistoryFilter.Created,
            onClick = { onFilterChange(HistoryFilter.Created) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FilterTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) CobaltPrimary else Color.Transparent)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) White else TextSecondary,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HistoryActivityCard(
    entity: HistoryEntity,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val title = ScanPayloadMapper.titleFor(entity)
    val subtitle = ScanPayloadMapper.subtitleFor(entity)
    val badge = ScanPayloadMapper.badgeLabel(entity)
    val timeLabel = formatCardTime(entity.timestamp)
    val icon = iconFor(entity.detectedType)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CobaltSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CobaltPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
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
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeLabel,
                        color = TextSecondary,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(CobaltSoft)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            color = CobaltPrimary,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                    if (entity.isFavorite) {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = "Favorite",
                            tint = CobaltPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardActionChip(
                label = "Copy",
                icon = Icons.Outlined.ContentCopy,
                onClick = onCopy
            )
            CardActionChip(
                label = "Share",
                icon = Icons.Outlined.Share,
                onClick = onShare,
                filled = ScanPayloadMapper.looksLikeUrl(entity.rawValue)
            )
            Spacer(modifier = Modifier.weight(1f))
            Box {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "More",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { menuOpen = true }
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor = White
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (entity.isFavorite) "Remove favorite" else "Add favorite",
                                fontFamily = PlusJakartaSans
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onToggleFavorite()
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Star, contentDescription = null, tint = CobaltPrimary)
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Delete",
                                color = Destructive,
                                fontFamily = PlusJakartaSans
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = Destructive)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CardActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    filled: Boolean = false
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (filled) CobaltPrimary else NestedSurface)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (filled) White else TextPrimary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = if (filled) White else TextPrimary,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun HistoryEmptyState(onLaunchScanner: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(White)
                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = CobaltPrimary,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Offline Vault Active · 0 Scans Recorded",
            color = TextSecondary,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 0.4.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No Scan History Yet",
            color = TextPrimary,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "QR codes and barcodes you scan with the camera, or custom codes you generate, will automatically be saved here securely for quick offline access.",
            color = TextSecondary,
            fontFamily = PlusJakartaSans,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CobaltPrimary)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onLaunchScanner
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCodeScanner,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Launch Scanner Now",
                    color = White,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

@Composable
private fun ClearHistoryDialog(
    itemCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x660F172A))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(White)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {}
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = null,
                            tint = Destructive,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Clear Scan & Create History?",
                            color = TextPrimary,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$itemCount stored items",
                                color = TextSecondary,
                                fontFamily = PlusJakartaSans,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Text(
                    text = "This will permanently remove non-favorite history items from your local device. Pinned favorites remain saved.",
                    color = TextSecondary,
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Text(
                    text = "Items cannot be restored once purged.",
                    color = TextTertiary,
                    fontFamily = PlusJakartaSans,
                    fontSize = 12.sp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Destructive)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onConfirm
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteForever,
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Clear All History",
                            color = White,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NestedSurface)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onDismiss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        color = TextPrimary,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

private data class DaySection(
    val label: String,
    val countLabel: String,
    val items: List<HistoryEntity>
)

private fun groupByDay(items: List<HistoryEntity>): List<DaySection> {
    if (items.isEmpty()) return emptyList()
    val cal = Calendar.getInstance()
    val todayStart = dayStart(cal)
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayStart = dayStart(cal)

    val today = mutableListOf<HistoryEntity>()
    val yesterday = mutableListOf<HistoryEntity>()
    val older = mutableListOf<HistoryEntity>()

    items.forEach { item ->
        when {
            item.timestamp >= todayStart -> today += item
            item.timestamp >= yesterdayStart -> yesterday += item
            else -> older += item
        }
    }

    return buildList {
        if (today.isNotEmpty()) {
            add(
                DaySection(
                    label = "Today",
                    countLabel = "${today.size} ${if (today.size == 1) "scan" else "scans"}",
                    items = today
                )
            )
        }
        if (yesterday.isNotEmpty()) {
            add(
                DaySection(
                    label = "Yesterday",
                    countLabel = "${yesterday.size} ${if (yesterday.size == 1) "item" else "items"}",
                    items = yesterday
                )
            )
        }
        if (older.isNotEmpty()) {
            add(
                DaySection(
                    label = "Earlier",
                    countLabel = "${older.size} ${if (older.size == 1) "item" else "items"}",
                    items = older
                )
            )
        }
    }
}

private fun dayStart(cal: Calendar): Long {
    val c = cal.clone() as Calendar
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

private fun formatCardTime(timestamp: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))

private fun iconFor(detectedType: String): ImageVector = when (detectedType) {
    ScanPayloadMapper.TYPE_WIFI -> Icons.Outlined.Wifi
    ScanPayloadMapper.TYPE_WEBSITE -> Icons.Outlined.Language
    ScanPayloadMapper.TYPE_BARCODE -> Icons.Outlined.ViewWeek
    ScanPayloadMapper.TYPE_QR_CODE -> Icons.Outlined.QrCode2
    else -> Icons.Outlined.Description
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("history", text))
}

private fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share"))
}
