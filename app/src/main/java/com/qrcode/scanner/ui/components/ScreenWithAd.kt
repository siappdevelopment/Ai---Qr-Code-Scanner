package com.qrcode.scanner.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.qrcode.scanner.MainActivity
import com.qrcode.scanner.launcher.activities.LauncherHomeActivity
import com.qrcode.scanner.launcher.common.ScreenInterAds
import com.qrcode.scanner.launcher.common.ScreenLoadAd
import com.qrcode.scanner.launcher.fragments.LauncherQrSystemBars

@Composable
fun ScreenWithAd(
    screenKey: String,
    modifier: Modifier = Modifier,
    nativeSize: String = "medium",
    /** When true, system/UI back runs ScreenInterAds for this screenKey first. */
    bindBackAd: Boolean = false,
    onBackLeave: (() -> Unit)? = null,
    /**
     * Forms with text fields: the keyboard covers the ad (the ad stays at the screen bottom) and only the
     * part of the keyboard that overlaps the content shrinks the content area, so the form can scroll
     * up to its last button.
     */
    keyboardAware: Boolean = false,
    content: @Composable () -> Unit
) {
    HideNavigationBarOnAdScreen()
    if (bindBackAd) {
        BindScreenBackInterAd(screenKey = screenKey, onLeave = onBackLeave)
    }
    val density = LocalDensity.current
    var adHeightPx by remember { mutableIntStateOf(0) }
    var columnBottomPx by remember { mutableIntStateOf(0) }
    // Exact overlap: where the keyboard starts in this window, against where this screen ends. The ime inset
    // alone also counts the navigation bar area, which would leave a gap above the keyboard.
    val rootHeightPx = LocalView.current.rootView.height
    val keyboardOverlapPx = if (keyboardAware) {
        val keyboardTop = rootHeightPx - WindowInsets.ime.getBottom(density)
        (columnBottomPx - keyboardTop - adHeightPx).coerceAtLeast(0)
    } else {
        0
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (keyboardAware) {
                    Modifier.onGloballyPositioned { columnBottomPx = (it.positionInWindow().y + it.size.height).toInt() }
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = with(density) { keyboardOverlapPx.toDp() })
        ) {
            content()
        }
        Box(modifier = Modifier.onSizeChanged { adHeightPx = it.height }) {
            FirebaseScreenAd(
                screenKey = screenKey,
                nativeSize = nativeSize,
                modifier = Modifier.navigationBarsPadding()
            )
        }
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
private fun BindScreenBackInterAd(
    screenKey: String,
    onLeave: (() -> Unit)?
) {
    val context = LocalContext.current
    val activity = context.findActivity() as? ComponentActivity ?: return
    DisposableEffect(screenKey, activity) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                ScreenInterAds.onBack(activity, screenKey) {
                    if (onLeave != null) {
                        onLeave()
                    } else {
                        activity.finish()
                    }
                }
            }
        }
        activity.onBackPressedDispatcher.addCallback(activity, callback)
        onDispose { callback.remove() }
    }
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

/**
 * navigationBarsPadding() that steps aside while the keyboard is open: the keyboard already covers the
 * navigation bar area, so keeping that padding would leave a gap between the content and the keyboard.
 */
fun Modifier.navigationBarsPaddingUnlessKeyboard(): Modifier = composed {
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    if (keyboardOpen) Modifier else Modifier.navigationBarsPadding()
}
