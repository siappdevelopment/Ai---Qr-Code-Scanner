package com.qrcode.scanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.qrcode.scanner.ui.theme.BorderSubtle
import com.qrcode.scanner.ui.theme.CardSurface

/** 1dp line under the header, drawn inside the header so the page below cannot cover it. */
@Composable
fun Modifier.headerBottomStroke(): Modifier {
    val color = BorderSubtle
    return this.drawWithContent {
        drawContent()
        val strokeWidth = 1.dp.toPx().coerceAtLeast(1f)
        drawRect(
            color = color,
            topLeft = Offset(0f, size.height - strokeWidth),
            size = Size(size.width, strokeWidth)
        )
    }
}

/** Header fill used by Home, Create, and Settings, including the status-bar area. */
@Composable
fun Modifier.appHeaderBackground(): Modifier = this
    .fillMaxWidth()
    .background(CardSurface)
    .headerBottomStroke()
    .statusBarsPadding()
