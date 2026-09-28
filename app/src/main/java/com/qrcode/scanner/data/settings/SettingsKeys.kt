package com.qrcode.scanner.data.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object SettingsKeys {
    val VIBRATE_ON_DETECTION = booleanPreferencesKey("vibrate_on_detection")
    val BEEP_ON_DETECTION = booleanPreferencesKey("beep_on_detection")
    val AUTO_OPEN_URLS = booleanPreferencesKey("auto_open_urls")
    val CONTINUOUS_BATCH_SCAN = booleanPreferencesKey("continuous_batch_scan")
    /** Stored as L / M / Q / H */
    val DEFAULT_QR_ECC = stringPreferencesKey("default_qr_ecc")
    /** Stored as SVG / PNG */
    val DEFAULT_QR_OUTPUT_FORMAT = stringPreferencesKey("default_qr_output_format")
    /** Stored as light / dark / system */
    val APP_THEME = stringPreferencesKey("app_theme")
}
