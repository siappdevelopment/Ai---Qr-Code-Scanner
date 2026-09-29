package com.qrcode.scanner.ui.screens.splash

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.qrcode.scanner.R
import com.qrcode.scanner.ui.theme.CobaltPrimary
import com.qrcode.scanner.ui.theme.NestedSurface
import com.qrcode.scanner.ui.theme.PageBackground
import com.qrcode.scanner.ui.theme.PlusJakartaSans
import com.qrcode.scanner.ui.theme.TextPrimary
import com.qrcode.scanner.ui.theme.TextSecondary

/**
 * Existing Compose splash. Startup timing is owned by StartupFlow, not by a second splash screen.
 */
@Composable
fun SplashScreen(
    onAdRootReady: (View) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
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
                color = TextPrimary,
                fontFamily = PlusJakartaSans,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Instant QR & Barcode Intelligence",
                color = TextSecondary,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = CobaltPrimary,
                trackColor = NestedSurface
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
