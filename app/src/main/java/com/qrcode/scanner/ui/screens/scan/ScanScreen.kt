package com.qrcode.scanner.ui.screens.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qrcode.scanner.ui.theme.BorderSubtle
import com.qrcode.scanner.ui.theme.CardSurface
import com.qrcode.scanner.ui.theme.CobaltPrimary
import com.qrcode.scanner.ui.theme.PageBackground
import com.qrcode.scanner.ui.theme.PlusJakartaSans
import com.qrcode.scanner.ui.theme.TextPrimary
import com.qrcode.scanner.ui.theme.TextSecondary
import com.qrcode.scanner.ui.theme.White

/**
 * Root Scan tab (keeps ScanPulseBottomBar via MainActivity NavHost).
 * Launches full-screen [ScannerActivity] via Intent.
 */
@Composable
fun ScanScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        context.startActivity(ScanIntents.openScanner(context))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface, RoundedCornerShape(16.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Scan",
                color = TextPrimary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                text = "Full-screen scanner opens in a separate Activity.",
                color = TextSecondary,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(CobaltPrimary, RoundedCornerShape(12.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        context.startActivity(ScanIntents.openScanner(context))
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Open Scanner",
                    color = White,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
