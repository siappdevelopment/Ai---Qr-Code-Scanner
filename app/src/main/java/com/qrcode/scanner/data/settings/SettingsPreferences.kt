package com.qrcode.scanner.data.settings

/**
 * App preferences persisted via DataStore.
 * Phase 12.1: vibrate/beep. Phase 12.5: auto-open URLs.
 * Phase 12.7: continuous batch scan preference.
 * Phase 12.17: QR creation ECC + output format defaults.
 * Phase 12.20: app theme mode.
 */
data class SettingsPreferences(
    val vibrateOnDetection: Boolean = DEFAULT_VIBRATE_ON_DETECTION,
    val beepOnDetection: Boolean = DEFAULT_BEEP_ON_DETECTION,
    val autoOpenUrls: Boolean = DEFAULT_AUTO_OPEN_URLS,
    val continuousBatchScan: Boolean = DEFAULT_CONTINUOUS_BATCH_SCAN,
    val defaultQrEcc: QrDefaultEcc = DEFAULT_QR_ECC,
    val defaultQrOutputFormat: QrDefaultOutputFormat = DEFAULT_QR_OUTPUT_FORMAT,
    val appTheme: AppThemeMode = DEFAULT_APP_THEME
) {
    companion object {
        /** Matches Stitch Settings Hub default (Vibrate ON). */
        const val DEFAULT_VIBRATE_ON_DETECTION = true

        /** Matches Stitch Settings Hub default (Beep OFF) and current silent scanner. */
        const val DEFAULT_BEEP_ON_DETECTION = false

        /**
         * Product Phase 12.5 default: OFF (safer than Stitch visual ON until Safe Browsing exists).
         */
        const val DEFAULT_AUTO_OPEN_URLS = false

        /**
         * Phase 12.7 default: OFF. Matches Stitch and keeps single-shot scanner until wired.
         */
        const val DEFAULT_CONTINUOUS_BATCH_SCAN = false

        /** Matches existing hardcoded create-form / encoder default (High / 30%). */
        val DEFAULT_QR_ECC: QrDefaultEcc = QrDefaultEcc.H

        /** Matches Settings Hub copy: Vector SVG / Sharp 1024px. */
        val DEFAULT_QR_OUTPUT_FORMAT: QrDefaultOutputFormat = QrDefaultOutputFormat.SVG

        /** Product default remains Light (White + Electric Cobalt). */
        val DEFAULT_APP_THEME: AppThemeMode = AppThemeMode.LIGHT
    }
}
