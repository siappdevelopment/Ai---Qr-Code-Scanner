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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.qrcode.scanner.MainActivity
import com.qrcode.scanner.launcher.activities.LauncherHomeActivity
import com.qrcode.scanner.launcher.common.ScreenLoadAd
import com.qrcode.scanner.launcher.fragments.LauncherQrSystemBars

@Composable
fun ScreenWithAd(
    screenKey: String,
    modifier: Modifier = Modifier,
    nativeSize: String = "medium",
    content: @Composable () -> Unit
) {
    HideNavigationBarOnAdScreen()
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
            nativeSize = nativeSize,
            modifier = Modifier.navigationBarsPadding()
        )
    }
}

@Composable
fun FirebaseScreenAd(
    screenKey: String,
    modifier: Modifier = Modifier,
    nativeSize: String = "medium"
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
                ScreenLoadAd.attach(activity, this, screenKey, nativeSize)
            }
        },
        onRelease = { host -> ScreenLoadAd.detach(host) }
    )
}

@Composable
private fun HideNavigationBarOnAdScreen() {
    val activity = LocalContext.current.findActivity() ?: return
    if (activity is LauncherHomeActivity || activity is MainActivity) {
        return
    }
    DisposableEffect(activity) {
        val owner = activity as? LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                LauncherQrSystemBars.hideNavigationBar(activity)
            }
        }
        owner?.lifecycle?.addObserver(observer)
        LauncherQrSystemBars.hideNavigationBar(activity)
        onDispose {
            owner?.lifecycle?.removeObserver(observer)
        }
    }
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
