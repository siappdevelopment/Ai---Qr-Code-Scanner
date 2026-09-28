package com.qrcode.scanner.data.settings

/**
 * Persisted app theme mode (Phase 12.20).
 * Storage values are stable tokens — not UI copy.
 */
enum class AppThemeMode(val storageValue: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system");

    fun settingsSubtitle(): String = when (this) {
        LIGHT -> "Light (Electric Cobalt)"
        DARK -> "Dark (Electric Cobalt)"
        SYSTEM -> "System Default"
    }

    fun selectionTitle(): String = when (this) {
        LIGHT -> "Light"
        DARK -> "Dark"
        SYSTEM -> "System Default"
    }

    /** Resolves whether dark colors should be used for the current system appearance. */
    fun resolveDark(systemDark: Boolean): Boolean = when (this) {
        LIGHT -> false
        DARK -> true
        SYSTEM -> systemDark
    }

    companion object {
        fun fromStored(value: String?): AppThemeMode =
            entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) }
                ?: LIGHT
    }
}
