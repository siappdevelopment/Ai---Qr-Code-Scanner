package com.qrcode.scanner.ui.screens.create

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
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.SentimentSatisfied
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qrcode.scanner.app.R
import com.qrcode.scanner.ui.components.AppBackButton
import com.qrcode.scanner.ui.components.appHeaderBackground
import com.qrcode.scanner.ui.theme.BorderSubtle
import com.qrcode.scanner.ui.theme.CardSurface
import com.qrcode.scanner.ui.theme.CobaltPrimary
import com.qrcode.scanner.ui.theme.NestedSurface
import com.qrcode.scanner.ui.theme.PageBackground
import com.qrcode.scanner.ui.theme.PlusJakartaSans
import com.qrcode.scanner.ui.theme.TextPrimary
import com.qrcode.scanner.ui.theme.TextSecondary
import com.qrcode.scanner.ui.theme.White
import com.qrcode.scanner.ui.theme.forDarkUi

/**
 * Stitch source: Create QR Category Hub (White Theme)
 * Screen ID: 79741836d19e4053a015ef8e4e2cf53f
 *
 * Font (from Stitch CSS): Plus Jakarta Sans
 * No gradients / shadows / elevation. BottomNavigation from ScanPulseBottomBar.
 */
@Composable
fun CreateHubScreen(
    onBack: () -> Unit = {},
    onCategoryClick: (QrCategoryType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CreateFilter.All) }
    val context = LocalContext.current

    val visibleCategories by remember(searchQuery, selectedFilter, context) {
        derivedStateOf {
            CreateCategory.entries.filter { category ->
                val matchesFilter =
                    selectedFilter == CreateFilter.All || category.filter == selectedFilter
                val q = searchQuery.trim()
                val title = context.getString(category.titleRes)
                val subtitle = context.getString(category.subtitleRes)
                val matchesSearch = q.isEmpty() ||
                    title.contains(q, ignoreCase = true) ||
                    subtitle.contains(q, ignoreCase = true)
                matchesFilter && matchesSearch
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CreateColors.SurfaceBg)
    ) {
        CreateTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClear = { searchQuery = "" }
            )

            FilterChipsRow(
                selected = selectedFilter,
                onSelect = { selectedFilter = it }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (visibleCategories.isEmpty()) {
                NoFormatsFound(
                    onReset = {
                        searchQuery = ""
                        selectedFilter = CreateFilter.All
                    }
                )
            } else {
                val standard = visibleCategories.filter { !it.social && !it.barcode }
                val social = visibleCategories.filter { it.social }.sortedBy { it.socialOrder }
                val barcodes = visibleCategories.filter { it.barcode }.sortedBy { it.barcodeOrder }
                if (standard.isNotEmpty()) {
                    CategoryGrid(
                        categories = standard,
                        onCategoryClick = onCategoryClick
                    )
                }
                if (social.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.create_hub_section_social),
                            color = CreateColors.TextMain,
                            fontFamily = PlusJakartaSans,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        SocialGrid(
                            categories = social,
                            onCategoryClick = onCategoryClick
                        )
                    }
                }
                if (barcodes.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.create_hub_section_barcode),
                            color = CreateColors.TextMain,
                            fontFamily = PlusJakartaSans,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        BarcodeGrid(
                            categories = barcodes,
                            onCategoryClick = onCategoryClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateTopBar(onBack: () -> Unit) {
    Column(
        modifier = Modifier.appHeaderBackground()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppBackButton(onClick = onBack)
            Text(
                text = stringResource(R.string.create_hub_title),
                color = CreateColors.TextMain,
                fontFamily = PlusJakartaSans,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 24.sp
            )
        }
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
            .background(CardSurface, RoundedCornerShape(12.dp))
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
                            text = stringResource(R.string.create_hub_search_placeholder),
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
                    contentDescription = stringResource(R.string.cd_clear_search),
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
                    text = stringResource(filter.labelRes),
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
private fun SocialGrid(
    categories: List<CreateCategory>,
    onCategoryClick: (QrCategoryType) -> Unit
) {
    CategoryGrid(categories = categories, onCategoryClick = onCategoryClick)
}

@Composable
private fun BarcodeGrid(
    categories: List<CreateCategory>,
    onCategoryClick: (QrCategoryType) -> Unit
) {
    CategoryGrid(categories = categories, onCategoryClick = onCategoryClick)
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
            .background(CardSurface, RoundedCornerShape(16.dp))
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
                    .background(category.iconBg.forDarkUi(), RoundedCornerShape(12.dp))
                    .border(1.dp, category.iconBorder.forDarkUi(), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val iconRes = category.iconRes
                if (iconRes != null) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        colorFilter = if (category.barcode) ColorFilter.tint(TextPrimary) else null,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = category.iconTint.forDarkUi(),
                        modifier = Modifier.size(22.dp)
                    )
                }
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
                text = stringResource(category.titleRes),
                color = CreateColors.TextMain,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(category.subtitleRes),
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
            .background(CardSurface, RoundedCornerShape(16.dp))
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
            text = stringResource(R.string.create_hub_no_formats_title),
            color = CreateColors.TextMain,
            fontFamily = PlusJakartaSans,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.create_hub_no_formats_body),
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
                text = stringResource(R.string.create_hub_reset_search),
                color = White,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private enum class CreateFilter(@StringRes val labelRes: Int, val key: String) {
    All(R.string.create_filter_all, "all"),
    Web(R.string.create_filter_web, "web"),
    Comms(R.string.create_filter_comms, "comms"),
    Utilities(R.string.create_filter_utilities, "utilities")
}

private enum class CreateCategory(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val filter: CreateFilter,
    val icon: ImageVector,
    val iconBg: Color,
    val iconBorder: Color,
    val iconTint: Color,
    val categoryType: QrCategoryType,
    val social: Boolean = false,
    val mark: String? = null,
    val socialOrder: Int = 0,
    val barcode: Boolean = false,
    val barcodeOrder: Int = 0,
    val iconRes: Int? = null
) {
    Website(
        titleRes = R.string.create_cat_website_title,
        subtitleRes = R.string.create_cat_website_subtitle,
        filter = CreateFilter.Web,
        icon = Icons.Outlined.Language,
        iconBg = Color(0xFFEFF6FF),
        iconBorder = Color(0xFFDBEAFE),
        iconTint = Color(0xFF2563EB),
        categoryType = QrCategoryType.WEBSITE
    ),
    Wifi(
        titleRes = R.string.create_cat_wifi_title,
        subtitleRes = R.string.create_cat_wifi_subtitle,
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.Wifi,
        iconBg = Color(0xFFECFDF5),
        iconBorder = Color(0xFFD1FAE5),
        iconTint = Color(0xFF059669),
        categoryType = QrCategoryType.WIFI
    ),
    Contact(
        titleRes = R.string.create_cat_contact_title,
        subtitleRes = R.string.create_cat_contact_subtitle,
        filter = CreateFilter.Comms,
        icon = Icons.Outlined.Person,
        iconBg = Color(0xFFEEF2FF),
        iconBorder = Color(0xFFE0E7FF),
        iconTint = Color(0xFF4F46E5),
        categoryType = QrCategoryType.CONTACT
    ),
    PlainText(
        titleRes = R.string.create_cat_plain_text_title,
        subtitleRes = R.string.create_cat_plain_text_subtitle,
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.Description,
        iconBg = Color(0xFFF1F5F9),
        iconBorder = Color(0xFFE2E8F0),
        iconTint = Color(0xFF334155),
        categoryType = QrCategoryType.PLAIN_TEXT
    ),
    Phone(
        titleRes = R.string.create_cat_phone_title,
        subtitleRes = R.string.create_cat_phone_subtitle,
        filter = CreateFilter.Comms,
        icon = Icons.Outlined.Call,
        iconBg = Color(0xFFECFDF5),
        iconBorder = Color(0xFFD1FAE5),
        iconTint = Color(0xFF059669),
        categoryType = QrCategoryType.PHONE
    ),
    Email(
        titleRes = R.string.create_cat_email_title,
        subtitleRes = R.string.create_cat_email_subtitle,
        filter = CreateFilter.Comms,
        icon = Icons.Outlined.Email,
        iconBg = Color(0xFFEFF6FF),
        iconBorder = Color(0xFFDBEAFE),
        iconTint = Color(0xFF2563EB),
        categoryType = QrCategoryType.EMAIL
    ),
    Sms(
        titleRes = R.string.create_cat_sms_title,
        subtitleRes = R.string.create_cat_sms_subtitle,
        filter = CreateFilter.Comms,
        icon = Icons.Outlined.Sms,
        iconBg = Color(0xFFF0F9FF),
        iconBorder = Color(0xFFE0F2FE),
        iconTint = Color(0xFF0284C7),
        categoryType = QrCategoryType.SMS
    ),
    WhatsApp(
        titleRes = R.string.create_cat_whatsapp_title,
        subtitleRes = R.string.create_cat_whatsapp_subtitle,
        filter = CreateFilter.Web,
        icon = Icons.Outlined.Chat,
        iconBg = Color(0xFFF0FDFA),
        iconBorder = Color(0xFFCCFBF1),
        iconTint = Color(0xFF25D366),
        categoryType = QrCategoryType.WHATSAPP,
        social = true,
        socialOrder = 1,
        iconRes = R.drawable.ic_whatsapp
    ),
    Location(
        titleRes = R.string.create_cat_location_title,
        subtitleRes = R.string.create_cat_location_subtitle,
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.LocationOn,
        iconBg = Color(0xFFFFF1F2),
        iconBorder = Color(0xFFFFE4E6),
        iconTint = Color(0xFFE11D48),
        categoryType = QrCategoryType.LOCATION
    ),
    Calendar(
        titleRes = R.string.create_cat_calendar_title,
        subtitleRes = R.string.create_cat_calendar_subtitle,
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.CalendarToday,
        iconBg = Color(0xFFFFFBEB),
        iconBorder = Color(0xFFFEF3C7),
        iconTint = Color(0xFFD97706),
        categoryType = QrCategoryType.CALENDAR
    ),
    AppLink(
        titleRes = R.string.create_cat_app_link_title,
        subtitleRes = R.string.create_cat_app_link_subtitle,
        filter = CreateFilter.Web,
        icon = Icons.Outlined.Apps,
        iconBg = Color(0xFFF5F3FF),
        iconBorder = Color(0xFFEDE9FE),
        iconTint = Color(0xFF7C3AED),
        categoryType = QrCategoryType.APP_LINK
    ),
    Facebook(
        titleRes = R.string.create_cat_facebook_title,
        subtitleRes = R.string.create_cat_facebook_subtitle,
        filter = CreateFilter.Web,
        icon = Icons.Outlined.Language,
        iconBg = Color(0xFFEFF6FF),
        iconBorder = Color(0xFFDBEAFE),
        iconTint = Color(0xFF1877F2),
        categoryType = QrCategoryType.FACEBOOK,
        social = true,
        socialOrder = 0,
        iconRes = R.drawable.ic_facebook
    ),
    YouTube(
        titleRes = R.string.create_cat_youtube_title,
        subtitleRes = R.string.create_cat_youtube_subtitle,
        filter = CreateFilter.Web,
        icon = Icons.Outlined.PlayArrow,
        iconBg = Color(0xFFFEF2F2),
        iconBorder = Color(0xFFFEE2E2),
        iconTint = Color(0xFFFF0000),
        categoryType = QrCategoryType.YOUTUBE,
        social = true,
        socialOrder = 2,
        iconRes = R.drawable.ic_youtube
    ),
    Twitter(
        titleRes = R.string.create_cat_twitter_title,
        subtitleRes = R.string.create_cat_twitter_subtitle,
        filter = CreateFilter.Web,
        icon = Icons.Outlined.Language,
        iconBg = Color(0xFFF1F5F9),
        iconBorder = Color(0xFFE2E8F0),
        iconTint = Color(0xFF111111),
        categoryType = QrCategoryType.TWITTER,
        social = true,
        socialOrder = 3,
        iconRes = R.drawable.ic_twitter
    ),
    TikTok(
        titleRes = R.string.create_cat_tiktok_title,
        subtitleRes = R.string.create_cat_tiktok_subtitle,
        filter = CreateFilter.Web,
        icon = Icons.Outlined.MusicNote,
        iconBg = Color(0xFFF1F5F9),
        iconBorder = Color(0xFFE2E8F0),
        iconTint = Color(0xFF111111),
        categoryType = QrCategoryType.TIKTOK,
        social = true,
        socialOrder = 4,
        iconRes = R.drawable.ic_tiktok
    ),
    Instagram(
        titleRes = R.string.create_cat_instagram_title,
        subtitleRes = R.string.create_cat_instagram_subtitle,
        filter = CreateFilter.Web,
        icon = Icons.Outlined.PhotoCamera,
        iconBg = Color(0xFFFDF2F8),
        iconBorder = Color(0xFFFCE7F3),
        iconTint = Color(0xFFE1306C),
        categoryType = QrCategoryType.INSTAGRAM,
        social = true,
        socialOrder = 5,
        iconRes = R.drawable.ic_instagram
    ),
    Paypal(
        titleRes = R.string.create_cat_paypal_title,
        subtitleRes = R.string.create_cat_paypal_subtitle,
        filter = CreateFilter.Web,
        icon = Icons.Outlined.Language,
        iconBg = Color(0xFFEFF6FF),
        iconBorder = Color(0xFFDBEAFE),
        iconTint = Color(0xFF003087),
        categoryType = QrCategoryType.PAYPAL,
        social = true,
        socialOrder = 6,
        iconRes = R.drawable.ic_paypal
    ),
    Snapchat(
        titleRes = R.string.create_cat_snapchat_title,
        subtitleRes = R.string.create_cat_snapchat_subtitle,
        filter = CreateFilter.Web,
        icon = Icons.Outlined.SentimentSatisfied,
        iconBg = Color(0xFFFEF9C3),
        iconBorder = Color(0xFFFEF08A),
        iconTint = Color(0xFF111111),
        categoryType = QrCategoryType.SNAPCHAT,
        social = true,
        socialOrder = 7,
        iconRes = R.drawable.ic_snapchat
    ),
    LinkedIn(
        titleRes = R.string.create_cat_linkedin_title,
        subtitleRes = R.string.create_cat_linkedin_subtitle,
        filter = CreateFilter.Web,
        icon = Icons.Outlined.Language,
        iconBg = Color(0xFFEFF6FF),
        iconBorder = Color(0xFFDBEAFE),
        iconTint = Color(0xFF0A66C2),
        categoryType = QrCategoryType.LINKEDIN,
        social = true,
        socialOrder = 8,
        iconRes = R.drawable.ic_linkedin
    ),
    Spotify(
        titleRes = R.string.create_cat_spotify_title,
        subtitleRes = R.string.create_cat_spotify_subtitle,
        filter = CreateFilter.Web,
        icon = Icons.Outlined.GraphicEq,
        iconBg = Color(0xFFECFDF5),
        iconBorder = Color(0xFFD1FAE5),
        iconTint = Color(0xFF1DB954),
        categoryType = QrCategoryType.SPOTIFY,
        social = true,
        socialOrder = 9,
        iconRes = R.drawable.ic_spotify
    ),
    Code128(
        titleRes = R.string.create_cat_code_128_title,
        subtitleRes = R.string.create_subtitle_barcode,
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.QrCode2,
        iconBg = Color(0xFFF1F5F9),
        iconBorder = Color(0xFFE2E8F0),
        iconTint = Color(0xFF111111),
        categoryType = QrCategoryType.CODE_128,
        barcode = true,
        barcodeOrder = 0,
        iconRes = R.drawable.ic_barcode_code128
    ),
    DataMatrix(
        titleRes = R.string.create_cat_data_matrix_title,
        subtitleRes = R.string.create_subtitle_barcode,
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.QrCode2,
        iconBg = Color(0xFFF1F5F9),
        iconBorder = Color(0xFFE2E8F0),
        iconTint = Color(0xFF111111),
        categoryType = QrCategoryType.DATA_MATRIX,
        barcode = true,
        barcodeOrder = 1,
        iconRes = R.drawable.ic_barcode_datamatrix
    ),
    Pdf417(
        titleRes = R.string.create_cat_pdf_417_title,
        subtitleRes = R.string.create_subtitle_barcode,
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.QrCode2,
        iconBg = Color(0xFFF1F5F9),
        iconBorder = Color(0xFFE2E8F0),
        iconTint = Color(0xFF111111),
        categoryType = QrCategoryType.PDF_417,
        barcode = true,
        barcodeOrder = 2,
        iconRes = R.drawable.ic_barcode_pdf417
    ),
    Aztec(
        titleRes = R.string.create_cat_aztec_title,
        subtitleRes = R.string.create_subtitle_barcode,
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.QrCode2,
        iconBg = Color(0xFFF1F5F9),
        iconBorder = Color(0xFFE2E8F0),
        iconTint = Color(0xFF111111),
        categoryType = QrCategoryType.AZTEC,
        barcode = true,
        barcodeOrder = 3,
        iconRes = R.drawable.ic_barcode_aztec
    ),
    Ean13(
        titleRes = R.string.create_cat_ean_13_title,
        subtitleRes = R.string.create_subtitle_barcode,
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.QrCode2,
        iconBg = Color(0xFFF1F5F9),
        iconBorder = Color(0xFFE2E8F0),
        iconTint = Color(0xFF111111),
        categoryType = QrCategoryType.EAN_13,
        barcode = true,
        barcodeOrder = 4,
        iconRes = R.drawable.ic_barcode_ean13
    ),
    Ean8(
        titleRes = R.string.create_cat_ean_8_title,
        subtitleRes = R.string.create_subtitle_barcode,
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.QrCode2,
        iconBg = Color(0xFFF1F5F9),
        iconBorder = Color(0xFFE2E8F0),
        iconTint = Color(0xFF111111),
        categoryType = QrCategoryType.EAN_8,
        barcode = true,
        barcodeOrder = 5,
        iconRes = R.drawable.ic_barcode_ean8
    ),
    UpcE(
        titleRes = R.string.create_cat_upc_e_title,
        subtitleRes = R.string.create_subtitle_barcode,
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.QrCode2,
        iconBg = Color(0xFFF1F5F9),
        iconBorder = Color(0xFFE2E8F0),
        iconTint = Color(0xFF111111),
        categoryType = QrCategoryType.UPC_E,
        barcode = true,
        barcodeOrder = 6,
        iconRes = R.drawable.ic_barcode_upce
    ),
    UpcA(
        titleRes = R.string.create_cat_upc_a_title,
        subtitleRes = R.string.create_subtitle_barcode,
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.QrCode2,
        iconBg = Color(0xFFF1F5F9),
        iconBorder = Color(0xFFE2E8F0),
        iconTint = Color(0xFF111111),
        categoryType = QrCategoryType.UPC_A,
        barcode = true,
        barcodeOrder = 7,
        iconRes = R.drawable.ic_barcode_upca
    ),
    Code93(
        titleRes = R.string.create_cat_code_93_title,
        subtitleRes = R.string.create_subtitle_barcode,
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.QrCode2,
        iconBg = Color(0xFFF1F5F9),
        iconBorder = Color(0xFFE2E8F0),
        iconTint = Color(0xFF111111),
        categoryType = QrCategoryType.CODE_93,
        barcode = true,
        barcodeOrder = 8,
        iconRes = R.drawable.ic_barcode_code93
    ),
    Code39(
        titleRes = R.string.create_cat_code_39_title,
        subtitleRes = R.string.create_subtitle_barcode,
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.QrCode2,
        iconBg = Color(0xFFF1F5F9),
        iconBorder = Color(0xFFE2E8F0),
        iconTint = Color(0xFF111111),
        categoryType = QrCategoryType.CODE_39,
        barcode = true,
        barcodeOrder = 9,
        iconRes = R.drawable.ic_barcode_code39
    ),
    Codabar(
        titleRes = R.string.create_cat_codabar_title,
        subtitleRes = R.string.create_subtitle_barcode,
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.QrCode2,
        iconBg = Color(0xFFF1F5F9),
        iconBorder = Color(0xFFE2E8F0),
        iconTint = Color(0xFF111111),
        categoryType = QrCategoryType.CODABAR,
        barcode = true,
        barcodeOrder = 10,
        iconRes = R.drawable.ic_barcode_codabar
    ),
    Itf(
        titleRes = R.string.create_cat_itf_title,
        subtitleRes = R.string.create_subtitle_barcode,
        filter = CreateFilter.Utilities,
        icon = Icons.Outlined.QrCode2,
        iconBg = Color(0xFFF1F5F9),
        iconBorder = Color(0xFFE2E8F0),
        iconTint = Color(0xFF111111),
        categoryType = QrCategoryType.ITF,
        barcode = true,
        barcodeOrder = 11,
        iconRes = R.drawable.ic_barcode_itf
    )
}

private object CreateColors {
    val SurfaceBg get() = PageBackground
    val SurfaceSubtle get() = NestedSurface
    val Border get() = BorderSubtle
    val TextMain get() = TextPrimary
    val TextMuted get() = TextSecondary
    val TextSecondary get() = com.qrcode.scanner.ui.theme.TextSecondary
    val ChipInactiveText get() = TextPrimary
}