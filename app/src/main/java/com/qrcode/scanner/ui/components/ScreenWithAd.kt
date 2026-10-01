package com.qrcode.scanner.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.qrcode.scanner.launcher.common.ScreenLoadAd

@Composable
fun ScreenWithAd(
    screenKey: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            content()
        }
        FirebaseScreenAd(
            screenKey = screenKey,
            modifier = Modifier.navigationBarsPadding()
        )
    }
}

@Composable
fun FirebaseScreenAd(
    screenKey: String,
    modifier: Modifier = Modifier
) {
    if (!ScreenLoadAd.isEnabled(screenKey)) {
        return
    }
    val activity = LocalContext.current.findActivity() ?: return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { viewContext ->
            FrameLayout(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                ScreenLoadAd.attach(activity, this, screenKey)
            }
        },
        onRelease = { host -> ScreenLoadAd.detach(host) }
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
