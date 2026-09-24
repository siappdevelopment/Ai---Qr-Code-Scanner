package com.qrcode.scanner.ui.screens.create

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrBitmapEncoder {

    enum class EccLevel(val label: String, val zxing: ErrorCorrectionLevel) {
        L("L", ErrorCorrectionLevel.L),
        M("M", ErrorCorrectionLevel.M),
        Q("Q", ErrorCorrectionLevel.Q),
        H("H", ErrorCorrectionLevel.H)
    }

    fun encode(
        content: String,
        sizePx: Int = 1024,
        ecc: EccLevel = EccLevel.H
    ): Bitmap {
        require(content.isNotBlank()) { "QR content is empty" }
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ecc.zxing,
            EncodeHintType.MARGIN to 1
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
            it.setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }
}
