package com.qrcode.scanner.launcher.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.qrcode.scanner.app.R
import com.qrcode.scanner.ui.navigation.ScanPulseNavHost
import com.qrcode.scanner.ui.theme.QRCodeScannerTheme

/**
 * Right-swipe pager page. Hosts the existing Compose QR UI in the ViewPager.
 * Camera follows this page lifecycle so it does not stay open on the launcher home page.
 */
class LauncherQrPageFragment : Fragment() {
    private val pageLifecycleOwner = PageLifecycleOwner()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        pageLifecycleOwner.registry.currentState = Lifecycle.State.CREATED
        val themedContext = ContextThemeWrapper(requireContext(), R.style.Theme_QRCodeScanner)
        return ComposeView(themedContext).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setViewTreeLifecycleOwner(pageLifecycleOwner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                QRCodeScannerTheme {
                    ScanPulseNavHost(showStartupSplash = false)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (pageLifecycleOwner.registry.currentState != Lifecycle.State.DESTROYED) {
            pageLifecycleOwner.registry.currentState = Lifecycle.State.RESUMED
        }
    }

    override fun onPause() {
        if (pageLifecycleOwner.registry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            pageLifecycleOwner.registry.currentState = Lifecycle.State.CREATED
        }
        super.onPause()
    }

    override fun onDestroyView() {
        if (pageLifecycleOwner.registry.currentState.isAtLeast(Lifecycle.State.CREATED)
            && pageLifecycleOwner.registry.currentState != Lifecycle.State.DESTROYED
        ) {
            pageLifecycleOwner.registry.currentState = Lifecycle.State.CREATED
        }
        super.onDestroyView()
    }

    override fun onDestroy() {
        if (pageLifecycleOwner.registry.currentState != Lifecycle.State.DESTROYED) {
            pageLifecycleOwner.registry.currentState = Lifecycle.State.DESTROYED
        }
        super.onDestroy()
    }

    private class PageLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle
            get() = registry
    }
}
