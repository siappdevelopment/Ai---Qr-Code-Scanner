package com.qrcode.scanner.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * ScanPulse color tokens for Light / Dark.
 * Screens read the top-level vals below; [ScanPulseThemeState] swaps the active palette.
 */
data class ScanPulsePalette(
    val cobaltPrimary: Color,
    val cobaltAccent: Color,
    val cobaltDark: Color,
    val cobaltSoft: Color,
    val white: Color,
    val pageBackground: Color,
    val cardSurface: Color,
    val nestedSurface: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val inactiveNav: Color,
    val destructive: Color
) {
    companion object {
        /** Existing White + Electric Cobalt product palette (unchanged). */
        val Light = ScanPulsePalette(
            cobaltPrimary = Color(0xFF0033CC),
            cobaltAccent = Color(0xFF00B4FF),
            cobaltDark = Color(0xFF002080),
            cobaltSoft = Color(0xFFEEF4FF),
            white = Color(0xFFFFFFFF),
            pageBackground = Color(0xFFF8FAFC),
            cardSurface = Color(0xFFFFFFFF),
            nestedSurface = Color(0xFFF1F5F9),
            borderSubtle = Color(0xFFE2E8F0),
            textPrimary = Color(0xFF0F172A),
            textSecondary = Color(0xFF64748B),
            textTertiary = Color(0xFF94A3B8),
            inactiveNav = Color(0xFF64748B),
            destructive = Color(0xFFDC2626)
        )

        /**
         * Dark surfaces stay neutral charcoal. The accent is a muted steel blue,
         * not the light theme's electric cobalt or cyan.
         */
        val Dark = ScanPulsePalette(
            cobaltPrimary = Color(0xFF4A78C8),
            cobaltAccent = Color(0xFF9BB6E3),
            cobaltDark = Color(0xFFD7E4F8),
            cobaltSoft = Color(0xFF1A2433),
            white = Color(0xFFFFFFFF),
            pageBackground = Color(0xFF101114),
            cardSurface = Color(0xFF1A1D24),
            nestedSurface = Color(0xFF242830),
            borderSubtle = Color(0xFF343944),
            textPrimary = Color(0xFFF3F5F8),
            textSecondary = Color(0xFFC2C7D0),
            textTertiary = Color(0xFF8E949F),
            inactiveNav = Color(0xFFA8AEB8),
            destructive = Color(0xFFF07178)
        )
    }
}

/**
 * Snapshot-backed active palette so existing `PageBackground` / `TextPrimary` reads
 * recompose when the user changes Theme (without per-screen theme branches).
 */
object ScanPulseThemeState {
    var palette: ScanPulsePalette by mutableStateOf(ScanPulsePalette.Light)
        internal set
}

/** Electric Cobalt Utility — theme-aware tokens (solid colors only). */

val CobaltPrimary: Color get() = ScanPulseThemeState.palette.cobaltPrimary
val CobaltAccent: Color get() = ScanPulseThemeState.palette.cobaltAccent
val CobaltDark: Color get() = ScanPulseThemeState.palette.cobaltDark
val CobaltSoft: Color get() = ScanPulseThemeState.palette.cobaltSoft

val White: Color get() = ScanPulseThemeState.palette.white
val PageBackground: Color get() = ScanPulseThemeState.palette.pageBackground
val CardSurface: Color get() = ScanPulseThemeState.palette.cardSurface
val NestedSurface: Color get() = ScanPulseThemeState.palette.nestedSurface

val BorderSubtle: Color get() = ScanPulseThemeState.palette.borderSubtle
val TextPrimary: Color get() = ScanPulseThemeState.palette.textPrimary
val TextSecondary: Color get() = ScanPulseThemeState.palette.textSecondary
val TextTertiary: Color get() = ScanPulseThemeState.palette.textTertiary
val InactiveNav: Color get() = ScanPulseThemeState.palette.inactiveNav
val Destructive: Color get() = ScanPulseThemeState.palette.destructive

/**
 * Light colors pass through unchanged. In dark mode, pale fills become muted wells
 * and darker brand colors are lifted so they stay readable on charcoal.
 */
fun Color.forDarkUi(): Color {
    if (ScanPulseThemeState.palette !== ScanPulsePalette.Dark) return this
    val luminance = (0.2126f * red) + (0.7152f * green) + (0.0722f * blue)
    return if (luminance >= 0.65f) {
        Color(
            red = (red * 0.16f + 0.12f).coerceIn(0f, 1f),
            green = (green * 0.16f + 0.13f).coerceIn(0f, 1f),
            blue = (blue * 0.16f + 0.16f).coerceIn(0f, 1f),
            alpha = alpha
        )
    } else {
        Color(
            red = (red * 0.42f + 0.58f).coerceIn(0f, 1f),
            green = (green * 0.42f + 0.58f).coerceIn(0f, 1f),
            blue = (blue * 0.42f + 0.58f).coerceIn(0f, 1f),
            alpha = alpha
        )
    }
}
