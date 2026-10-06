package com.qrcode.scanner.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.graphics.toArgb
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
import com.qrcode.scanner.data.settings.AppThemeMode
import com.qrcode.scanner.data.settings.SettingsPreferences
import com.qrcode.scanner.data.settings.SettingsRepositoryProvider
import com.qrcode.scanner.data.settings.readAppNightMode
import com.qrcode.scanner.MainActivity
import com.qrcode.scanner.launcher.activities.LauncherHomeActivity

/**
 * Electric Cobalt theme driven by Settings → Theme (Phase 12.20).
 * - No dynamic Material color
 * - Solid colors only (no gradients)
 * - Default preference is Light. Dark stays dark. The phone theme is ignored.
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
    val initialPreferences = remember(context) {
        SettingsPreferences(
            appTheme = if (readAppNightMode(context) == AppCompatDelegate.MODE_NIGHT_YES) {
                AppThemeMode.DARK
            } else {
                AppThemeMode.LIGHT
            }
        )
    }
    val preferences by repository.preferences.collectAsStateWithLifecycle(
        initialValue = initialPreferences
    )
    val useDark = preferences.appTheme == AppThemeMode.DARK
    val palette = if (useDark) ScanPulsePalette.Dark else ScanPulsePalette.Light
    val colorScheme = if (useDark) darkSchemeFrom(palette) else lightSchemeFrom(palette)

    SideEffect {
        ScanPulseThemeState.palette = palette
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findHostActivity() ?: return@SideEffect
            // Launcher owns its bars. MainActivity Scan bars are applied in ScanPulseNavHost.
            if (activity is LauncherHomeActivity || activity is MainActivity) {
                return@SideEffect
            }
            val window = activity.window
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = !useDark
            insets.isAppearanceLightNavigationBars = !useDark
            @Suppress("DEPRECATION")
            window.statusBarColor = AndroidColor.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = if (useDark) {
                palette.pageBackground.toArgb()
            } else {
                AndroidColor.TRANSPARENT
            }
            if (useDark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/** Edge-to-edge; status + nav bars stay transparent (swipe-in system nav is see-through). */
fun ComponentActivity.enableThemedEdgeToEdge() {
    val dark = readAppNightMode(this) == AppCompatDelegate.MODE_NIGHT_YES
    enableEdgeToEdge(
        statusBarStyle = if (dark) {
            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        } else {
            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        },
        navigationBarStyle = if (dark) {
            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        } else {
            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        }
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
    @Suppress("DEPRECATION")
    window.navigationBarColor = AndroidColor.TRANSPARENT
    @Suppress("DEPRECATION")
    window.statusBarColor = AndroidColor.TRANSPARENT
}

private fun Context.findHostActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }
        current = current.baseContext
    }
    return null
}
