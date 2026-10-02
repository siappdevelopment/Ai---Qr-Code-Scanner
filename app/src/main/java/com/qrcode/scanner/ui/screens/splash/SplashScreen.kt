package com.qrcode.scanner.ui.screens.splash

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrcode.scanner.app.R
import com.qrcode.scanner.data.settings.AppThemeMode
import com.qrcode.scanner.data.settings.SettingsPreferences
import com.qrcode.scanner.data.settings.SettingsRepositoryProvider
import com.qrcode.scanner.data.settings.readAppNightMode
import com.qrcode.scanner.ui.theme.PlusJakartaSans
import com.qrcode.scanner.ui.theme.ScanPulsePalette

/**
 * Existing Compose splash. Startup timing is owned by StartupFlow, not by a second splash screen.
 */
@Composable
fun SplashScreen(
    onAdRootReady: (View) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val repository = remember { SettingsRepositoryProvider.get(context) }
    val initialPreferences = remember(context) {
        SettingsPreferences(
            appTheme = if (readAppNightMode(context) == AppCompatDelegate.MODE_NIGHT_YES) {
                AppThemeMode.DARK
            } else {
                AppThemeMode.LIGHT
            }
        )
    }
    val preferences by repository.preferences.collectAsStateWithLifecycle(
        initialValue = initialPreferences
    )
    val dark = preferences.appTheme == AppThemeMode.DARK
    val palette = if (dark) ScanPulsePalette.Dark else ScanPulsePalette.Light

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val insets = WindowCompat.getInsetsController(window, view)
        insets.isAppearanceLightStatusBars = !dark
        insets.isAppearanceLightNavigationBars = !dark
        val background = palette.pageBackground.toArgb()
        @Suppress("DEPRECATION")
        window.statusBarColor = background
        @Suppress("DEPRECATION")
        window.navigationBarColor = background
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.pageBackground)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        setImageResource(R.mipmap.ic_launcher)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        contentDescription = context.getString(R.string.app_name)
                    }
                },
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(22.dp))
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "ScanPulse",
                color = palette.textPrimary,
                fontFamily = PlusJakartaSans,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Instant QR & Barcode Intelligence",
                color = palette.textSecondary,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = palette.cobaltPrimary,
                trackColor = palette.nestedSurface
            )
        }

        AndroidView(
            factory = { context ->
                val themed = ContextThemeWrapper(context, R.style.Theme_LauncherSettings)
                LayoutInflater.from(themed).inflate(R.layout.layout_splash_startup_ads, null, false)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            update = onAdRootReady
        )
    }
}
