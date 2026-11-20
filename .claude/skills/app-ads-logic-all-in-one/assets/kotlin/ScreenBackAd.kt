package com.qrcode.scanner.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.qrcode.scanner.launcher.common.ScreenInterAds

fun ComponentActivity.bindScreenBackAd(screenKey: String) {
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            ScreenInterAds.onBack(this@bindScreenBackAd, screenKey, Runnable { finish() })
        }
    })
}

fun Context.runWithClickAd(screenKey: String, action: () -> Unit) {
    val activity = findClickHostActivity()
    if (activity == null) {
        action()
        return
    }
    ScreenInterAds.onClick(activity, screenKey, Runnable { action() })
}

private fun Context.findClickHostActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }
        current = current.baseContext
    }
    return null
}
