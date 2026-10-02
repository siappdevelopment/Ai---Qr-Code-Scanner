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

    fun consumeReopenSettings(): Boolean {
        if (!reopenSettings) {
            return false
        }
        reopenSettings = false
        return true
    }
}
