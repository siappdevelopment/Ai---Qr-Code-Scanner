package com.qrcode.scanner.ui.navigation

/**
 * Night-mode changes recreate the host. The QR page then opens on Scan,
 * so a theme change from Settings asks the new screen to open Settings again.
 */
object ThemeNavigation {
    @Volatile
    private var reopenSettings = false

    fun markReopenSettings() {
        reopenSettings = true
    }

    /** Peek only: the launcher uses it to paint the header status bar before the first frame. */
    fun isReopenSettingsPending(): Boolean = reopenSettings

    fun consumeReopenSettings(): Boolean {
        if (!reopenSettings) {
            return false
        }
        reopenSettings = false
        return true
    }
}
