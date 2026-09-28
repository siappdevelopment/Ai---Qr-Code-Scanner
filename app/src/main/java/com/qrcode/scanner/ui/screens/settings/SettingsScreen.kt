package com.qrcode.scanner.ui.screens.settings

import android.content.pm.PackageManager
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
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrcode.scanner.data.history.HistoryRepositoryProvider
import com.qrcode.scanner.data.settings.SettingsPreferences
import com.qrcode.scanner.data.settings.SettingsRepositoryProvider
import com.qrcode.scanner.ui.screens.history.ClearHistoryDialog
import com.qrcode.scanner.ui.theme.BorderSubtle
import com.qrcode.scanner.ui.theme.CardSurface
import com.qrcode.scanner.ui.theme.CobaltPrimary
import com.qrcode.scanner.ui.theme.CobaltSoft
import com.qrcode.scanner.ui.theme.Destructive
import com.qrcode.scanner.ui.theme.PageBackground
import com.qrcode.scanner.ui.theme.PlusJakartaSans
import com.qrcode.scanner.ui.theme.TextPrimary
import com.qrcode.scanner.ui.theme.TextSecondary
import com.qrcode.scanner.ui.theme.TextTertiary
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
    var showEccDialog by remember { mutableStateOf(false) }
    var showFormatDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    val appVersion = remember {
        try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (_: PackageManager.NameNotFoundException) {
            "1.0"
        }
    }

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
//            SettingsPreferencesHero()

            SettingsSectionCard(title = "Appearance") {
                SettingsNavRow(
                    title = "Theme",
                    subtitle = preferences.appTheme.settingsSubtitle(),
                    icon = Icons.Outlined.DarkMode,
                    enabled = true,
                    onClick = { showThemeDialog = true }
                )
                SettingsRowDivider()
                SettingsNavRow(
                    title = "Language",
                    // Only English UI is shipped; preference (system vs en-US) is set on Language screen.
                    subtitle = "English (US)",
                    icon = Icons.Outlined.Language,
                    enabled = true,
                    onClick = onOpenLanguage
                )
            }

            SettingsSectionCard(title = "Scanner & Hardware") {
                SettingsToggleRow(
                    title = "Vibrate on Detection",
                    subtitle = "Haptic pulse when matrix resolves",
                    icon = Icons.Outlined.Vibration,
                    checked = preferences.vibrateOnDetection,
                    onCheckedChange = { enabled ->
                        scope.launch { repository.setVibrateOnDetection(enabled) }
                    }
                )
                SettingsRowDivider()
                SettingsToggleRow(
                    title = "Beep Sound",
                    subtitle = "Audio tone confirmation",
                    icon = Icons.AutoMirrored.Outlined.VolumeUp,
                    checked = preferences.beepOnDetection,
                    onCheckedChange = { enabled ->
                        scope.launch { repository.setBeepOnDetection(enabled) }
                    }
                )
                SettingsRowDivider()
                SettingsToggleRow(
                    title = "Auto-Open URLs",
                    subtitle = "Opens website links in the browser after a scan",
                    icon = Icons.Outlined.OpenInBrowser,
                    checked = preferences.autoOpenUrls,
                    onCheckedChange = { enabled ->
                        scope.launch { repository.setAutoOpenUrls(enabled) }
                    }
                )
            }

            SettingsSectionCard(title = "QR Creation") {
                SettingsNavRow(
                    title = "Default Correction Level",
                    subtitle = preferences.defaultQrEcc.settingsSubtitle(),
                    icon = Icons.Outlined.Security,
                    showChevron = true,
                    enabled = true,
                    onClick = { showEccDialog = true }
                )
                SettingsRowDivider()
                SettingsNavRow(
                    title = "Default QR Format",
                    subtitle = preferences.defaultQrOutputFormat.settingsSubtitle(),
                    icon = Icons.Outlined.QrCode2,
                    showChevron = true,
                    enabled = true,
                    onClick = { showFormatDialog = true }
                )
            }

            SettingsSectionCard(title = "Storage & History") {
//                SettingsNavRow(
//                    title = "Cloud Auto-Backup",
//                    subtitle = "Google Drive encrypted store",
//                    icon = Icons.Outlined.CloudSync,
//                    trailingLabel = "Unavailable",
//                    showChevron = false,
//                    enabled = false,
//                    onClick = null
//                )
                SettingsRowDivider()
                SettingsNavRow(
                    title = "Clear Scan & Create History",
                    subtitle = "Remove non-favorite items · favorites kept",
                    icon = Icons.Outlined.DeleteSweep,
                    titleColor = Destructive,
                    iconTint = Destructive,
                    iconBackground = Color(0xFFFEE2E2),
                    showChevron = false,
                    enabled = true,
                    onClick = { showClearDialog = true }
                )
            }

            SettingsSectionCard(title = "About & Legal") {
                SettingsNavRow(
                    title = "About ScanPulse",
                    subtitle = "Version $appVersion",
                    icon = Icons.Outlined.Info,
                    enabled = true,
                    onClick = onOpenAbout
                )
                SettingsRowDivider()
                SettingsNavRow(
                    title = "Privacy Policy",
                    subtitle = "URL not configured",
                    icon = Icons.Outlined.PrivacyTip,
                    showChevron = false,
                    showExternalLink = true,
                    enabled = false,
                    onClick = null
                )
                SettingsRowDivider()
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
                    Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showClearDialog = false }
        )
    }

    if (showEccDialog) {
        QrDefaultEccPickerDialog(
            selected = preferences.defaultQrEcc,
            onSelect = { ecc ->
                showEccDialog = false
                scope.launch { repository.setDefaultQrEcc(ecc) }
            },
            onDismiss = { showEccDialog = false }
        )
    }

    if (showFormatDialog) {
        QrDefaultFormatPickerDialog(
            selected = preferences.defaultQrOutputFormat,
            onSelect = { format ->
                showFormatDialog = false
                scope.launch { repository.setDefaultQrOutputFormat(format) }
            },
            onDismiss = { showFormatDialog = false }
        )
    }

    if (showThemeDialog) {
        AppThemePickerDialog(
            selected = preferences.appTheme,
            onSelect = { theme ->
                showThemeDialog = false
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Settings",
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
                    text = "Preferences",
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
                        text = "Engine v2.4.0",
                        color = CobaltPrimary,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "High-speed optical matrix & synthesis parameters",
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
                text = "OPTICAL CV PIPELINE ONLINE",
                color = TextTertiary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 0.6.sp
            )
        }
        Text(
            text = "ScanPulse Pro · Device Acceleration Verified",
            color = TextTertiary,
            fontFamily = PlusJakartaSans,
            fontSize = 11.sp
        )
    }
}
