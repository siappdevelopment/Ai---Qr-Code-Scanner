package com.qrcode.scanner.data.settings

/**
 * Persisted app theme mode (Phase 12.20).
 * Storage values are stable tokens — not UI copy.
 */
enum class AppThemeMode(val storageValue: String) {
    LIGHT("light"),
    DARK("dark");

    fun settingsSubtitle(): String = when (this) {
        LIGHT -> "Light"
        DARK -> "Dark"
    }

    fun selectionTitle(): String = when (this) {
        LIGHT -> "Light"
        DARK -> "Dark"
    }

    /** Resolves whether dark colors should be used. Light stays light even if the device is dark. */
    fun resolveDark(systemDark: Boolean): Boolean = when (this) {
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromStored(value: String?): AppThemeMode =
            entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) }
                ?: LIGHT
    }
}
