package com.qrcode.scanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity

/**
 * Electric Cobalt White / Light Theme.
 * - No dynamic Material color
 * - No automatic system dark theme
 * - Solid colors only (no gradients)
 */
private val ElectricCobaltLightScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = White,
    primaryContainer = CobaltSoft,
    onPrimaryContainer = CobaltDark,
    secondary = CobaltAccent,
    onSecondary = White,
    secondaryContainer = CobaltSoft,
    onSecondaryContainer = CobaltDark,
    tertiary = CobaltAccent,
    onTertiary = White,
    background = PageBackground,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = NestedSurface,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderSubtle,
    error = Destructive,
    onError = White
)

@Composable
fun QRCodeScannerTheme(
    // Force light theme — ignore system dark mode
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    // Explicitly unused: never follow system dark or dynamic color
    @Suppress("UNUSED_VARIABLE")
    val ignoredSystemDark = isSystemInDarkTheme()

    val colorScheme = ElectricCobaltLightScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
