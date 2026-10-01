package com.qrcode.scanner.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.qrcode.scanner.launcher.common.ScreenNativeAds

@Composable
fun BigNativeAd(
    slot: ScreenNativeAds.Slot,
    modifier: Modifier = Modifier
) {
    if (!ScreenNativeAds.isEnabled(slot)) {
        return
    }
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() } ?: return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { viewContext ->
            FrameLayout(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                ScreenNativeAds.attach(activity, this, slot)
            }
        },
        onRelease = { host -> ScreenNativeAds.detach(host, slot) }
    )
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }
        current = current.baseContext
    }
    return null
}
