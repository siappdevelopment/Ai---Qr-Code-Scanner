package com.qrcode.scanner.ui.screens.create

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qrcode.scanner.ui.theme.CobaltPrimary
import com.qrcode.scanner.ui.theme.PlusJakartaSans
import com.qrcode.scanner.ui.theme.White

/**
 * Stitch source: Create QR Category Hub (White Theme)
 * Screen ID: 79741836d19e4053a015ef8e4e2cf53f
 *
 * Font (from Stitch CSS): Plus Jakarta Sans
 * No gradients / shadows / elevation. BottomNavigation from ScanPulseBottomBar.
 */
@Composable
fun CreateHubScreen(
    onCategoryClick: (QrCategoryType) -> Unit = {},
    onTuneClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CreateFilter.All) }

    val visibleCategories by remember(searchQuery, selectedFilter) {
        derivedStateOf {
            CreateCategory.entries.filter { category ->
                val matchesFilter =
                    selectedFilter == CreateFilter.All || category.filter == selectedFilter
                val q = searchQuery.trim()
                val matchesSearch = q.isEmpty() ||
                    category.title.contains(q, ignoreCase = true) ||
                    category.subtitle.contains(q, ignoreCase = true)
                matchesFilter && matchesSearch
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CreateColors.SurfaceBg)
    ) {
        CreateTopBar(onTuneClick = onTuneClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Create QR Code",
                    color = CreateColors.TextMain,
                    fontFamily = PlusJakartaSans,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 32.sp
                )
                Text(
                    text = "Choose a content format to generate a custom scannable code",
                    color = CreateColors.TextMuted,
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 20.sp
                )
            }

            SearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClear = { searchQuery = "" }
            )

            FilterChipsRow(
                selected = selectedFilter,
                onSelect = { selectedFilter = it }
            )

            if (visibleCategories.isEmpty()) {
                NoFormatsFound(
                    onReset = {
                        searchQuery = ""
                        selectedFilter = CreateFilter.All
                    }
                )
            } else {
                CategoryGrid(
                    categories = visibleCategories,
                    onCategoryClick = onCategoryClick
                )
            }
        }
    }
}

@Composable
private fun CreateTopBar(onTuneClick: () -> Unit) {
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
                        .size(36.dp)
                        .background(CobaltPrimary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddBox,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Create QR Code",
                    color = CreateColors.TextMain,
                    fontFamily = PlusJakartaSans,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(White, RoundedCornerShape(12.dp))
                    .border(1.dp, CreateColors.Border, RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTuneClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Tune",
                    tint = CreateColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CreateColors.Border)
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(White, RoundedCornerShape(12.dp))
            .border(1.dp, CreateColors.Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = CreateColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                cursorBrush = SolidColor(CobaltPrimary),
                textStyle = TextStyle(
                    color = CreateColors.TextMain,
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Search QR format...",
                            color = CreateColors.TextSecondary,
                            fontFamily = PlusJakartaSans,
                            fontSize = 14.sp
                        )
                    }
                    inner()
                }
            )
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Outlined.Cancel,
                    contentDescription = "Clear search",
                    tint = CreateColors.TextSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClear
                        )
                )
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    selected: CreateFilter,
    onSelect: (CreateFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CreateFilter.entries.forEach { filter ->
            val active = filter == selected
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .background(
                        if (active) CobaltPrimary else CreateColors.SurfaceSubtle,
                        RoundedCornerShape(999.dp)
                    )
                    .border(
                        1.dp,
                        if (active) CobaltPrimary else CreateColors.Border,
                        RoundedCornerShape(999.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(filter) }
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter.label,
                    color = if (active) White else CreateColors.ChipInactiveText,
                    fontFamily = PlusJakartaSans,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<CreateCategory>,
    onCategoryClick: (QrCategoryType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        categories.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { category ->
                    CategoryCard(
                        category = category,
                        onClick = { onCategoryClick(category.categoryType) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: CreateCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(min = 112.dp)
            .background(White, RoundedCornerShape(16.dp))
            .border(1.dp, CreateColors.Border, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(category.iconBg, RoundedCornerShape(12.dp))
                    .border(1.dp, category.iconBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = category.iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
                Icon(
                    imageVector = Icons.Outlined.NorthEast,
                    contentDescription = null,
                    tint = CreateColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = category.title,
                color = CreateColors.TextMain,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = category.subtitle,
                color = CreateColors.TextMuted,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NoFormatsFound(onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(16.dp))
            .border(1.dp, CreateColors.Border, RoundedCornerShape(16.dp))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(CreateColors.SurfaceSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.SearchOff,
                contentDescription = null,
                tint = CreateColors.TextSecondary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No formats found",
            color = CreateColors.TextMain,
            fontFamily = PlusJakartaSans,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Try typing another keyword or reset the active filter",
            color = CreateColors.TextMuted,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .height(36.dp)
                .background(CobaltPrimary, RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onReset
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Reset Search",
                color = White,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private enum class CreateFilter(val label: String, val key: String) {
    All("All Formats", "all"),
    Web("Web & Social", "web"),
    Comms("Communications", "comms"),
    Utilities("Utilities", "utilities")
}

private enum class CreateCategory(
    val title: String,
    val subtitle: String,
    val filter: CreateFilter,
    val icon: ImageVector,
    val iconBg: Color,
    val iconBorder: Color,
    val iconTint: Color,
    val categoryType: QrCategoryType
) {
    Website(
        title = "Website URL",
        subtitle = "Links, social profiles, web pages",
        filter = CreateFilter.Web,
        icon = Icons.Outlined.Language,
        iconBg = Color(0xFFEFF6FF),
        iconBorder = Color(0xFFDBEAFE),
        iconTint = Color(0xFF2563EB),
        categoryType = QrCategoryType.WEBSITE
    ),
    Wifi(
        title = "Wi-Fi Network",
        subtitle = "WPA/WPA2, SSID, auto-connect",
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.Wifi,
        iconBg = Color(0xFFECFDF5),
        iconBorder = Color(0xFFD1FAE5),
        iconTint = Color(0xFF059669),
        categoryType = QrCategoryType.WIFI
    ),
    Contact(
        title = "Contact / vCard",
        subtitle = "Name, phone, email, address",
        filter = CreateFilter.Comms,
        icon = Icons.Outlined.Person,
        iconBg = Color(0xFFEEF2FF),
        iconBorder = Color(0xFFE0E7FF),
        iconTint = Color(0xFF4F46E5),
        categoryType = QrCategoryType.CONTACT
    ),
    PlainText(
        title = "Plain Text",
        subtitle = "Notes, messages, raw data",
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.Description,
        iconBg = Color(0xFFF1F5F9),
        iconBorder = Color(0xFFE2E8F0),
        iconTint = Color(0xFF334155),
        categoryType = QrCategoryType.PLAIN_TEXT
    ),
    Phone(
        title = "Phone Call",
        subtitle = "Direct phone dialing",
        filter = CreateFilter.Comms,
        icon = Icons.Outlined.Call,
        iconBg = Color(0xFFECFDF5),
        iconBorder = Color(0xFFD1FAE5),
        iconTint = Color(0xFF059669),
        categoryType = QrCategoryType.PHONE
    ),
    Email(
        title = "Email Message",
        subtitle = "Pre-filled recipient & subject",
        filter = CreateFilter.Comms,
        icon = Icons.Outlined.Email,
        iconBg = Color(0xFFEFF6FF),
        iconBorder = Color(0xFFDBEAFE),
        iconTint = Color(0xFF2563EB),
        categoryType = QrCategoryType.EMAIL
    ),
    Sms(
        title = "SMS / Message",
        subtitle = "Direct text message",
        filter = CreateFilter.Comms,
        icon = Icons.Outlined.Sms,
        iconBg = Color(0xFFF0F9FF),
        iconBorder = Color(0xFFE0F2FE),
        iconTint = Color(0xFF0284C7),
        categoryType = QrCategoryType.SMS
    ),
    WhatsApp(
        title = "WhatsApp",
        subtitle = "Direct WhatsApp chat link",
        filter = CreateFilter.Comms,
        icon = Icons.Outlined.Chat,
        iconBg = Color(0xFFF0FDFA),
        iconBorder = Color(0xFFCCFBF1),
        iconTint = Color(0xFF0D9488),
        categoryType = QrCategoryType.WHATSAPP
    ),
    Location(
        title = "Location",
        subtitle = "Google Maps coordinates / pin",
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.LocationOn,
        iconBg = Color(0xFFFFF1F2),
        iconBorder = Color(0xFFFFE4E6),
        iconTint = Color(0xFFE11D48),
        categoryType = QrCategoryType.LOCATION
    ),
    Calendar(
        title = "Calendar Event",
        subtitle = "Title, date, time, reminder",
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.CalendarToday,
        iconBg = Color(0xFFFFFBEB),
        iconBorder = Color(0xFFFEF3C7),
        iconTint = Color(0xFFD97706),
        categoryType = QrCategoryType.CALENDAR
    ),
    AppLink(
        title = "App Link",
        subtitle = "Google Play & App Store links",
        filter = CreateFilter.Web,
        icon = Icons.Outlined.Apps,
        iconBg = Color(0xFFF5F3FF),
        iconBorder = Color(0xFFEDE9FE),
        iconTint = Color(0xFF7C3AED),
        categoryType = QrCategoryType.APP_LINK
    ),
    Barcode(
        title = "Barcode / EAN",
        subtitle = "EAN-13, UPC, Code 128 formats",
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.QrCode2,
        iconBg = Color(0xFFECFEFF),
        iconBorder = Color(0xFFCFFAFE),
        iconTint = Color(0xFF0891B2),
        categoryType = QrCategoryType.BARCODE
    )
}

private object CreateColors {
    val SurfaceBg = Color(0xFFF8FAFC)
    val SurfaceSubtle = Color(0xFFF1F5F9)
    val Border = Color(0xFFE2E8F0)
    val TextMain = Color(0xFF0F172A)
    val TextMuted = Color(0xFF475569)
    val TextSecondary = Color(0xFF64748B)
    val ChipInactiveText = Color(0xFF334155)
}