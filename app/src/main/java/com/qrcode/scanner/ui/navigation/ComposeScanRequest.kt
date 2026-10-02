package com.qrcode.scanner.ui.navigation

/**
 * History asks the existing Scan page to open. The scanner activity is not used.
 */
object ComposeScanRequest {
    @Volatile
    private var pending: Boolean = false

    fun request() {
        pending = true
    }

    fun isPending(): Boolean = pending

    fun consume(): Boolean {
        if (!pending) {
            return false
        }
        pending = false
        return true
    }
}
