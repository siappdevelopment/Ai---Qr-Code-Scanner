package com.qrcode.scanner.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrcode.scanner.data.settings.SettingsPreferences
import com.qrcode.scanner.data.settings.SettingsRepositoryProvider

/**
 * Electric Cobalt theme driven by Settings → Theme (Phase 12.20).
 * - No dynamic Material color
 * - Solid colors only (no gradients)
 * - Default preference is Light; System follows [isSystemInDarkTheme]
 */
private fun lightSchemeFrom(palette: ScanPulsePalette) = lightColorScheme(
    primary = palette.cobaltPrimary,
    onPrimary = palette.white,
    primaryContainer = palette.cobaltSoft,
    onPrimaryContainer = palette.cobaltDark,
    secondary = palette.cobaltAccent,
    onSecondary = palette.white,
    secondaryContainer = palette.cobaltSoft,
    onSecondaryContainer = palette.cobaltDark,
    tertiary = palette.cobaltAccent,
    onTertiary = palette.white,
    background = palette.pageBackground,
    onBackground = palette.textPrimary,
    surface = palette.cardSurface,
    onSurface = palette.textPrimary,
    surfaceVariant = palette.nestedSurface,
    onSurfaceVariant = palette.textSecondary,
    outline = palette.borderSubtle,
    outlineVariant = palette.borderSubtle,
    error = palette.destructive,
    onError = palette.white
)

private fun darkSchemeFrom(palette: ScanPulsePalette) = darkColorScheme(
    primary = palette.cobaltPrimary,
    onPrimary = palette.white,
    primaryContainer = palette.cobaltSoft,
    onPrimaryContainer = palette.cobaltDark,
    secondary = palette.cobaltAccent,
    onSecondary = palette.white,
    secondaryContainer = palette.cobaltSoft,
    onSecondaryContainer = palette.cobaltDark,
    tertiary = palette.cobaltAccent,
    onTertiary = palette.white,
    background = palette.pageBackground,
    onBackground = palette.textPrimary,
    surface = palette.cardSurface,
    onSurface = palette.textPrimary,
    surfaceVariant = palette.nestedSurface,
    onSurfaceVariant = palette.textSecondary,
    outline = palette.borderSubtle,
    outlineVariant = palette.borderSubtle,
    error = palette.destructive,
    onError = palette.white
)

@Composable
fun QRCodeScannerTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { SettingsRepositoryProvider.get(context) }
    val preferences by repository.preferences.collectAsStateWithLifecycle(
        initialValue = SettingsPreferences()
    )
    val systemDark = isSystemInDarkTheme()
    val useDark = preferences.appTheme.resolveDark(systemDark)
    val palette = if (useDark) ScanPulsePalette.Dark else ScanPulsePalette.Light
    val colorScheme = if (useDark) darkSchemeFrom(palette) else lightSchemeFrom(palette)

    SideEffect {
        ScanPulseThemeState.palette = palette
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = !useDark
            insets.isAppearanceLightNavigationBars = !useDark
            @Suppress("DEPRECATION")
            window.statusBarColor = AndroidColor.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = AndroidColor.TRANSPARENT
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
