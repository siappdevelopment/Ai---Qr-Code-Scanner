package com.qrcode.scanner.ui.screens.create

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Keeps a picked center logo inside app storage.
 * Gallery content URIs lose read permission after the picker activity finishes.
 */
object QrLogoStore {
    private const val DIR = "qr_logos"
    private const val FILE = "center_logo.png"

    fun persist(context: Context, bitmap: Bitmap): String {
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        val file = File(dir, FILE)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    }

    fun load(context: Context, stored: String?): Bitmap? {
        if (stored.isNullOrBlank()) return null
        val decoded = runCatching {
            when {
                stored.startsWith("/") -> BitmapFactory.decodeFile(stored)
                else -> context.contentResolver.openInputStream(Uri.parse(stored))?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        }.getOrNull() ?: return null
        return QrStyledRenderer.prepareLogo(decoded, 256)
    }
}
