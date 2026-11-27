package com.qrcode.scanner.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.qrcode.scanner.launcher.common.ExitDialog

/**
 * Put first inside a root tab: it catches the Back press that would leave the app and opens the exit dialog.
 * A Back handler the screen declares after it (for example the batch scan list) still runs first.
 */
@Composable
fun ExitDialogBackHandler(enabled: Boolean) {
    val activity = LocalContext.current.findExitActivity()
    BackHandler(enabled = enabled && activity?.isTaskRoot == true) {
        activity?.let { ExitDialog.onExitRequested(it) }
    }
}

private fun Context.findExitActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }
        current = current.baseContext
    }
    return null
}
