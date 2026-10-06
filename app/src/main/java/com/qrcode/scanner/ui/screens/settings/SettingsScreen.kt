package com.qrcode.scanner.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Vibration
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrcode.scanner.app.R
import com.qrcode.scanner.launcher.activities.LauncherSettingsActivity
import com.qrcode.scanner.data.history.HistoryRepositoryProvider
import com.qrcode.scanner.ui.navigation.ThemeNavigation
import com.qrcode.scanner.data.settings.AppThemeMode
import com.qrcode.scanner.data.settings.SettingsPreferences
import com.qrcode.scanner.data.settings.SettingsRepositoryProvider
import com.qrcode.scanner.launcher.common.AppUtils
import com.qrcode.scanner.launcher.common.ScreenNativeAds
import com.qrcode.scanner.ui.components.BigNativeAd
import com.qrcode.scanner.ui.screens.history.ClearHistoryDialog
import com.qrcode.scanner.ui.theme.BorderSubtle
import com.qrcode.scanner.ui.components.headerBottomStroke
import com.qrcode.scanner.ui.theme.CardSurface
import com.qrcode.scanner.ui.theme.CobaltPrimary
import com.qrcode.scanner.ui.theme.CobaltSoft
import com.qrcode.scanner.ui.theme.Destructive
import com.qrcode.scanner.ui.theme.PageBackground
import com.qrcode.scanner.ui.theme.PlusJakartaSans
import com.qrcode.scanner.ui.theme.TextPrimary
import com.qrcode.scanner.ui.theme.TextSecondary
import com.qrcode.scanner.ui.theme.TextTertiary
import com.qrcode.scanner.ui.theme.forDarkUi
import kotlinx.coroutines.launch

/**
 * Stitch: Settings Hub (White Theme) — 6a0638abd79a4fb0a225307a6daef6a6
 *
 * White + Electric Cobalt. No gradients / shadows / Profile / Ask AI.
 * BottomNavigation: shared ScanPulseBottomBar (unchanged).
 * Phase 12.15: Clear History reuses HistoryRepository.clearHistoryKeepingFavorites().
 */
@Composable
fun SettingsScreen(
    onOpenAbout: () -> Unit = {},
    onOpenLanguage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { SettingsRepositoryProvider.get(context) }
    val historyRepository = remember { HistoryRepositoryProvider.get(context) }
    val scope = rememberCoroutineScope()
    val preferences by repository.preferences.collectAsStateWithLifecycle(
        initialValue = SettingsPreferences()
    )
    val historyCount by historyRepository.observeCount()
        .collectAsStateWithLifecycle(initialValue = 0)
    var showClearDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        SettingsTopBar()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BigNativeAd(slot = ScreenNativeAds.Slot.SETTINGS)

            SettingsSectionCard(title = stringResource(R.string.settings_section_appearance)) {
                SettingsNavRow(
                    title = stringResource(R.string.settings_theme_title),
                    subtitle = when (preferences.appTheme) {
                        AppThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                        AppThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                    },
                    icon = Icons.Outlined.DarkMode,
                    enabled = true,
                    onClick = { showThemeDialog = true }
                )
                SettingsRowDivider()
                SettingsNavRow(
                    title = stringResource(R.string.settings_language_title),
                    // Only English UI is shipped; preference (system vs en-US) is set on Language screen.
                    subtitle = currentLanguageLabel(context),
                    icon = Icons.Outlined.Language,
                    enabled = true,
                    onClick = onOpenLanguage
                )
                SettingsRowDivider()
                SettingsNavRow(
                    title = stringResource(R.string.settings_launcher_title),
                    subtitle = stringResource(R.string.settings_launcher_subtitle),
                    icon = Icons.Outlined.Apps,
                    enabled = true,
                    onClick = {
                        context.startActivity(Intent(context, LauncherSettingsActivity::class.java))
                    }
                )
            }

            SettingsSectionCard(title = stringResource(R.string.settings_section_scanner)) {
                SettingsToggleRow(
                    title = stringResource(R.string.settings_vibrate_title),
                    subtitle = stringResource(R.string.settings_vibrate_subtitle),
                    icon = Icons.Outlined.Vibration,
                    checked = preferences.vibrateOnDetection,
                    onCheckedChange = { enabled ->
                        scope.launch { repository.setVibrateOnDetection(enabled) }
                    }
                )
                SettingsRowDivider()
                SettingsToggleRow(
                    title = stringResource(R.string.settings_beep_title),
                    subtitle = stringResource(R.string.settings_beep_subtitle),
                    icon = Icons.AutoMirrored.Outlined.VolumeUp,
                    checked = preferences.beepOnDetection,
                    onCheckedChange = { enabled ->
                        scope.launch { repository.setBeepOnDetection(enabled) }
                    }
                )
                SettingsRowDivider()
                SettingsToggleRow(
                    title = stringResource(R.string.settings_auto_open_urls_title),
                    subtitle = stringResource(R.string.settings_auto_open_urls_subtitle),
                    icon = Icons.Outlined.OpenInBrowser,
                    checked = preferences.autoOpenUrls,
                    onCheckedChange = { enabled ->
                        scope.launch { repository.setAutoOpenUrls(enabled) }
                    }
                )
                SettingsRowDivider()
                SettingsNavRow(
                    title = stringResource(R.string.settings_clear_history_title),
                    subtitle = stringResource(R.string.settings_clear_history_subtitle),
                    icon = Icons.Outlined.DeleteSweep,
                    titleColor = Destructive,
                    iconTint = Destructive,
                    iconBackground = Color(0xFFFEE2E2).forDarkUi(),
                    showChevron = false,
                    enabled = true,
                    onClick = { showClearDialog = true }
                )
            }

            SettingsSectionCard(title = stringResource(R.string.settings_section_about)) {
                SettingsNavRow(
                    title = stringResource(R.string.settings_about_title),
                    subtitle = "",
                    icon = Icons.Outlined.Info,
                    enabled = true,
                    onClick = onOpenAbout
                )
                SettingsRowDivider()
                SettingsNavRow(
                    title = stringResource(R.string.settings_privacy_policy_title),
                    subtitle = "",
                    icon = Icons.Outlined.PrivacyTip,
                    showChevron = false,
                    showExternalLink = true,
                    enabled = true,
                    onClick = { AppUtils.openPrivacyPolicy(context) }
                )
                SettingsRowDivider()
                SettingsNavRow(
                    title = stringResource(R.string.settings_share_title),
                    subtitle = "",
                    icon = Icons.Outlined.Share,
                    enabled = true,
                    onClick = { shareApp(context) }
                )
                SettingsRowDivider()
                SettingsNavRow(
                    title = stringResource(R.string.settings_rate_title),
                    subtitle = "",
                    icon = Icons.Outlined.Star,
                    showChevron = false,
                    showExternalLink = true,
                    enabled = true,
                    onClick = { rateApp(context) }
                )
//                SettingsRowDivider()
//                SettingsNavRow(
//                    title = "Terms of Service",
//                    subtitle = "URL not configured",
//                    icon = Icons.Outlined.Gavel,
//                    showChevron = false,
//                    showExternalLink = true,
//                    enabled = false,
//                    onClick = null
//                )
            }

//            SettingsFooter()
        }
    }

    if (showClearDialog) {
        ClearHistoryDialog(
            itemCount = historyCount,
            onConfirm = {
                showClearDialog = false
                scope.launch {
                    historyRepository.clearHistoryKeepingFavorites()
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_history_cleared),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onDismiss = { showClearDialog = false }
        )
    }

    if (showThemeDialog) {
        AppThemePickerDialog(
            selected = preferences.appTheme,
            onSelect = { theme ->
                showThemeDialog = false
                if (theme != preferences.appTheme) {
                    ThemeNavigation.markReopenSettings()
                }
                scope.launch { repository.setAppTheme(theme) }
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
private fun SettingsTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface)
            .headerBottomStroke()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            color = TextPrimary,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            letterSpacing = (-0.2).sp
        )
    }
}

@Composable
private fun SettingsPreferencesHero() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CobaltSoft, RoundedCornerShape(16.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_preferences_title),
                    color = TextPrimary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Box(
                    modifier = Modifier
                        .background(CardSurface, RoundedCornerShape(999.dp))
                        .border(
                            1.dp,
                            CobaltPrimary.copy(alpha = 0.25f),
                            RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_engine_version),
                        color = CobaltPrimary,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_preferences_subtitle),
                color = TextSecondary,
                fontFamily = PlusJakartaSans,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(CardSurface, RoundedCornerShape(12.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFF22C55E), CircleShape)
            )
            Text(
                text = stringResource(R.string.settings_pipeline_online),
                color = TextTertiary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 0.6.sp
            )
        }
        Text(
            text = stringResource(R.string.settings_pro_footer, stringResource(R.string.app_name)),
            color = TextTertiary,
            fontFamily = PlusJakartaSans,
            fontSize = 11.sp
        )
    }
}

private fun shareApp(context: android.content.Context) {
    val url = "https://play.google.com/store/apps/details?id=${context.packageName}"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, context.applicationInfo.loadLabel(context.packageManager))
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.share_chooser_title))
    )
}

private fun rateApp(context: android.content.Context) {
    val packageName = context.packageName
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val web = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(market)
    } catch (_: Exception) {
        context.startActivity(web)
    }
}

private fun currentLanguageLabel(context: android.content.Context): String {
    return when (AppUtils.getLanguage(context)) {
        "hi" -> context.getString(R.string.settings_language_hindi)
        "ru" -> context.getString(R.string.settings_language_russian)
        "it" -> context.getString(R.string.settings_language_italian)
        "fr" -> context.getString(R.string.settings_language_french)
        "es" -> context.getString(R.string.settings_language_spanish)
        "ja" -> context.getString(R.string.settings_language_japanese)
        "ko" -> context.getString(R.string.settings_language_korean)
        "de" -> context.getString(R.string.settings_language_german)
        "zh" -> context.getString(R.string.settings_language_chinese)
        "th" -> context.getString(R.string.settings_language_thai)
        "el" -> context.getString(R.string.settings_language_greek)
        "pt" -> context.getString(R.string.settings_language_portuguese_pt)
        "pt-BR" -> context.getString(R.string.settings_language_portuguese_br)
        "nl" -> context.getString(R.string.settings_language_dutch)
        "fil" -> context.getString(R.string.settings_language_filipino)
        "tr" -> context.getString(R.string.settings_language_turkish)
        "id" -> context.getString(R.string.settings_language_indonesian)
        "af" -> context.getString(R.string.settings_language_afrikaans)
        else -> context.getString(R.string.settings_language_english)
    }
}
