package com.qrcode.scanner.ui.screens.create

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import kotlin.math.hypot
import kotlin.math.min

/**
 * Renders a scannable QR from [payload] using [QrStyleConfig].
 * Payload bytes are never altered — only visual styling.
 */
object QrStyledRenderer {

    data class RenderResult(
        val bitmap: Bitmap?,
        val errorMessage: String? = null
    ) {
        val isSuccess: Boolean get() = bitmap != null && errorMessage == null
    }

    fun validateContrast(foreground: Int, background: Int): String? {
        val contrast = relativeLuminanceContrast(foreground, background)
        return if (contrast < 3.0) {
            "Colors are too similar for reliable scanning. Choose higher-contrast colors."
        } else null
    }

    fun render(
        payload: String,
        style: QrStyleConfig,
        ecc: QrBitmapEncoder.EccLevel = QrBitmapEncoder.EccLevel.H,
        sizePx: Int = 1024,
        centerLogo: Bitmap? = null
    ): RenderResult {
        if (payload.isBlank()) return RenderResult(null, "Nothing to encode")
        validateContrast(style.foregroundColor, style.backgroundColor)?.let {
            return RenderResult(null, it)
        }

        // Logos need maximum recovery; bump to H when a center mark is present.
        val effectiveEcc =
            if (style.centerIcon != QrStyleConfig.CenterIcon.NONE || centerLogo != null) {
                QrBitmapEncoder.EccLevel.H
            } else {
                ecc
            }

        val matrix = encodeMatrix(payload, effectiveEcc)
        val moduleCount = matrix.width
        val quiet = estimateQuietZone(matrix)
        val contentModules = moduleCount - quiet * 2
        if (contentModules <= 0) return RenderResult(null, "Unable to encode QR")

        val qrSize = sizePx
        val (canvasW, canvasH, qrLeft, qrTop) = when (style.frame) {
            QrStyleConfig.FrameTemplate.NONE -> {
                val pad = (sizePx * 0.04f).toInt().coerceAtLeast(16)
                Quad(qrSize + pad * 2, qrSize + pad * 2, pad.toFloat(), pad.toFloat())
            }
            QrStyleConfig.FrameTemplate.PILL -> {
                val pad = (sizePx * 0.12f).toInt()
                val extraBottom = (sizePx * 0.1f).toInt()
                Quad(
                    qrSize + pad * 2,
                    qrSize + pad + extraBottom,
                    pad.toFloat(),
                    pad * 0.4f
                )
            }
            QrStyleConfig.FrameTemplate.BANNER -> {
                val pad = (sizePx * 0.14f).toInt()
                val extraTop = (sizePx * 0.1f).toInt()
                Quad(
                    qrSize + pad * 2,
                    qrSize + pad + extraTop,
                    pad.toFloat(),
                    pad + sizePx * 0.08f
                )
            }
            QrStyleConfig.FrameTemplate.CYBER -> {
                val pad = (sizePx * 0.08f).toInt()
                Quad(qrSize + pad * 2, qrSize + pad * 2, pad.toFloat(), pad.toFloat())
            }
        }

        val bitmap = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(style.backgroundColor)

        drawQrModules(
            canvas = canvas,
            matrix = matrix,
            left = qrLeft,
            top = qrTop,
            size = qrSize.toFloat(),
            style = style,
            quiet = quiet
        )

        // Center logo (~16% of QR + light plate; stays clear of finder patterns)
        val logoBmp = centerLogo
        if (style.centerIcon != QrStyleConfig.CenterIcon.NONE || logoBmp != null) {
            val logoSize = (qrSize * 0.16f)
            val cx = qrLeft + qrSize / 2f
            val cy = qrTop + qrSize / 2f
            drawCenterBadge(canvas, cx, cy, logoSize, style, logoBmp)
        }

        drawFrame(canvas, qrLeft, qrTop, qrSize.toFloat(), canvasW, canvasH, style)

        return RenderResult(bitmap)
    }

    private data class Quad(val w: Int, val h: Int, val left: Float, val top: Float)

    fun toSvg(
        payload: String,
        style: QrStyleConfig,
        ecc: QrBitmapEncoder.EccLevel = QrBitmapEncoder.EccLevel.H,
        sizePx: Int = 1024
    ): String {
        validateContrast(style.foregroundColor, style.backgroundColor)?.let { err ->
            throw IllegalStateException(err)
        }
        val matrix = encodeMatrix(payload, ecc)
        val n = matrix.width
        val cell = sizePx.toFloat() / n
        val fg = colorToHex(style.foregroundColor)
        val bg = colorToHex(style.backgroundColor)
        val quiet = estimateQuietZone(matrix)
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.append("""<svg xmlns="http://www.w3.org/2000/svg" width="$sizePx" height="$sizePx" viewBox="0 0 $sizePx $sizePx">""")
        sb.append("""<rect width="100%" height="100%" fill="$bg"/>""")
        for (y in 0 until n) {
            for (x in 0 until n) {
                if (!matrix[x, y]) continue
                if (isInFinder(x, y, n, quiet)) continue
                val px = x * cell
                val py = y * cell
                when (style.bodyPattern) {
                    QrStyleConfig.BodyPattern.CLASSIC ->
                        sb.append("""<rect x="$px" y="$py" width="$cell" height="$cell" fill="$fg"/>""")
                    QrStyleConfig.BodyPattern.ROUNDED -> {
                        val r = cell * 0.28f
                        sb.append("""<rect x="$px" y="$py" width="$cell" height="$cell" rx="$r" fill="$fg"/>""")
                    }
                    QrStyleConfig.BodyPattern.DOTS -> {
                        val cx = px + cell / 2f
                        val cy = py + cell / 2f
                        val r = cell * 0.38f
                        sb.append("""<circle cx="$cx" cy="$cy" r="$r" fill="$fg"/>""")
                    }
                    QrStyleConfig.BodyPattern.DIAMOND -> {
                        val cx = px + cell / 2f
                        val cy = py + cell / 2f
                        val d = cell * 0.58f
                        sb.append("""<polygon points="${cx},${cy - d} ${cx + d},$cy $cx,${cy + d} ${cx - d},$cy" fill="$fg"/>""")
                    }
                }
            }
        }
        // Finder eyes as rounded/square groups
        listOf(
            quiet to quiet,
            n - quiet - 7 to quiet,
            quiet to n - quiet - 7
        ).forEach { (fx, fy) ->
            appendEyeSvg(sb, fx, fy, cell, style, fg, bg)
        }
        sb.append("</svg>")
        return sb.toString()
    }

    private fun encodeMatrix(payload: String, ecc: QrBitmapEncoder.EccLevel): BitMatrix {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ecc.zxing,
            EncodeHintType.MARGIN to 2
        )
        return QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 0, 0, hints)
    }

    private fun estimateQuietZone(matrix: BitMatrix): Int {
        var quiet = 0
        val n = matrix.width
        while (quiet < n / 2) {
            var empty = true
            for (i in 0 until n) {
                if (matrix[quiet, i] || matrix[i, quiet]) {
                    empty = false
                    break
                }
            }
            if (!empty) break
            quiet++
        }
        return quiet.coerceAtLeast(0)
    }

    private fun isInFinder(x: Int, y: Int, n: Int, quiet: Int): Boolean {
        fun inBox(ox: Int, oy: Int) = x in ox until (ox + 7) && y in oy until (oy + 7)
        return inBox(quiet, quiet) ||
            inBox(n - quiet - 7, quiet) ||
            inBox(quiet, n - quiet - 7)
    }

    private fun drawQrModules(
        canvas: Canvas,
        matrix: BitMatrix,
        left: Float,
        top: Float,
        size: Float,
        style: QrStyleConfig,
        quiet: Int
    ) {
        val n = matrix.width
        val cell = size / n
        val fg = Paint().apply {
            color = style.foregroundColor
            this.style = Paint.Style.FILL
            isAntiAlias = false
            isFilterBitmap = false
        }
        val bg = Paint().apply {
            color = style.backgroundColor
            this.style = Paint.Style.FILL
            isAntiAlias = false
        }

        // Background plate for QR area
        canvas.drawRect(left, top, left + size, top + size, bg)

        for (y in 0 until n) {
            for (x in 0 until n) {
                if (!matrix[x, y]) continue
                if (isInFinder(x, y, n, quiet)) continue
                val px = left + x * cell
                val py = top + y * cell
                drawModule(canvas, px, py, cell, style.bodyPattern, fg)
            }
        }

        listOf(
            quiet to quiet,
            n - quiet - 7 to quiet,
            quiet to n - quiet - 7
        ).forEach { (fx, fy) ->
            drawEye(
                canvas = canvas,
                left = left + fx * cell,
                top = top + fy * cell,
                size = cell * 7f,
                style = style,
                fg = fg,
                bg = bg
            )
        }
    }

    private fun drawModule(
        canvas: Canvas,
        x: Float,
        y: Float,
        cell: Float,
        pattern: QrStyleConfig.BodyPattern,
        paint: Paint
    ) {
        when (pattern) {
            QrStyleConfig.BodyPattern.CLASSIC ->
                canvas.drawRect(x, y, x + cell, y + cell, paint)
            QrStyleConfig.BodyPattern.ROUNDED -> {
                val r = cell * 0.18f
                canvas.drawRoundRect(RectF(x, y, x + cell, y + cell), r, r, paint)
            }
            QrStyleConfig.BodyPattern.DOTS -> {
                val cx = x + cell / 2f
                val cy = y + cell / 2f
                // Keep fill high enough for scanners (~96% of cell)
                canvas.drawCircle(cx, cy, cell * 0.48f, paint)
            }
            QrStyleConfig.BodyPattern.DIAMOND -> {
                val cx = x + cell / 2f
                val cy = y + cell / 2f
                // ~67% cell fill (was 50% at 0.5); still reads as a diamond mesh
                val d = cell * 0.58f
                val path = Path().apply {
                    moveTo(cx, cy - d)
                    lineTo(cx + d, cy)
                    lineTo(cx, cy + d)
                    lineTo(cx - d, cy)
                    close()
                }
                canvas.drawPath(path, paint)
            }
        }
    }

    private fun drawEye(
        canvas: Canvas,
        left: Float,
        top: Float,
        size: Float,
        style: QrStyleConfig,
        fg: Paint,
        bg: Paint
    ) {
        // Finder rings must stay recognizably rectangular for scanners.
        // Soft: mild outer rounding only. Circle: square outer/mid rings + circular core.
        val outerRadius = when (style.eyeStyle) {
            QrStyleConfig.EyeStyle.SQUARE -> 0f
            QrStyleConfig.EyeStyle.SOFT -> size * 0.10f
            QrStyleConfig.EyeStyle.CIRCLE -> 0f
        }
        val midRadius = when (style.eyeStyle) {
            QrStyleConfig.EyeStyle.SQUARE -> 0f
            QrStyleConfig.EyeStyle.SOFT -> size * 0.07f
            QrStyleConfig.EyeStyle.CIRCLE -> 0f
        }
        val outer = RectF(left, top, left + size, top + size)
        canvas.drawRoundRect(outer, outerRadius, outerRadius, fg)
        // Standard finder: 7×7 dark, 5×5 light (1-module inset), 3×3 dark (2-module inset)
        val inset = size * (1f / 7f)
        val mid = RectF(left + inset, top + inset, left + size - inset, top + size - inset)
        canvas.drawRoundRect(mid, midRadius, midRadius, bg)
        val coreInset = size * (2f / 7f)
        val core = RectF(
            left + coreInset,
            top + coreInset,
            left + size - coreInset,
            top + size - coreInset
        )
        when (style.eyeStyle) {
            QrStyleConfig.EyeStyle.CIRCLE ->
                canvas.drawOval(core, fg)
            else ->
                canvas.drawRoundRect(core, midRadius * 0.6f, midRadius * 0.6f, fg)
        }
    }

    private fun drawCenterBadge(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        logoSize: Float,
        style: QrStyleConfig,
        logoBitmap: Bitmap?
    ) {
        val pad = logoSize * 0.12f
        val plate = RectF(
            cx - logoSize / 2f - pad,
            cy - logoSize / 2f - pad,
            cx + logoSize / 2f + pad,
            cy + logoSize / 2f + pad
        )
        val platePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.backgroundColor
            this.style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.foregroundColor
            this.style = Paint.Style.STROKE
            strokeWidth = logoSize * 0.04f
        }
        canvas.drawRoundRect(plate, logoSize * 0.22f, logoSize * 0.22f, platePaint)
        canvas.drawRoundRect(plate, logoSize * 0.22f, logoSize * 0.22f, borderPaint)

        when {
            logoBitmap != null && style.centerIcon == QrStyleConfig.CenterIcon.CUSTOM -> {
                val dst = RectF(
                    cx - logoSize / 2f,
                    cy - logoSize / 2f,
                    cx + logoSize / 2f,
                    cy + logoSize / 2f
                )
                canvas.drawBitmap(logoBitmap, null, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            }
            style.centerIcon == QrStyleConfig.CenterIcon.WIFI ->
                drawWifiIcon(canvas, cx, cy, logoSize * 0.7f, style.foregroundColor)
            style.centerIcon == QrStyleConfig.CenterIcon.GLOBE ->
                drawGlobeIcon(canvas, cx, cy, logoSize * 0.7f, style.foregroundColor)
            else -> Unit
        }
    }

    private fun drawWifiIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = size * 0.1f
            strokeCap = Paint.Cap.ROUND
        }
        val r1 = size * 0.18f
        val r2 = size * 0.32f
        val r3 = size * 0.46f
        canvas.drawCircle(cx, cy + size * 0.28f, size * 0.06f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        })
        canvas.drawArc(cx - r1, cy + size * 0.05f, cx + r1, cy + size * 0.05f + r1 * 2, -140f, 100f, false, p)
        canvas.drawArc(cx - r2, cy - size * 0.05f, cx + r2, cy - size * 0.05f + r2 * 2, -140f, 100f, false, p)
        canvas.drawArc(cx - r3, cy - size * 0.18f, cx + r3, cy - size * 0.18f + r3 * 2, -140f, 100f, false, p)
    }

    private fun drawGlobeIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = size * 0.08f
        }
        val r = size * 0.42f
        canvas.drawCircle(cx, cy, r, p)
        canvas.drawOval(cx - r * 0.45f, cy - r, cx + r * 0.45f, cy + r, p)
        canvas.drawLine(cx - r, cy, cx + r, cy, p)
    }

    private fun drawFrame(
        canvas: Canvas,
        qrLeft: Float,
        qrTop: Float,
        qrSize: Float,
        canvasW: Int,
        canvasH: Int,
        style: QrStyleConfig
    ) {
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.foregroundColor
            this.style = Paint.Style.STROKE
            strokeWidth = qrSize * 0.02f
            strokeCap = Paint.Cap.SQUARE
        }
        when (style.frame) {
            QrStyleConfig.FrameTemplate.NONE -> Unit
            QrStyleConfig.FrameTemplate.CYBER -> {
                val len = qrSize * 0.12f
                val inset = qrSize * 0.02f
                val l = qrLeft - inset
                val t = qrTop - inset
                val r = qrLeft + qrSize + inset
                val b = qrTop + qrSize + inset
                // corners
                canvas.drawLine(l, t, l + len, t, accent)
                canvas.drawLine(l, t, l, t + len, accent)
                canvas.drawLine(r, t, r - len, t, accent)
                canvas.drawLine(r, t, r, t + len, accent)
                canvas.drawLine(l, b, l + len, b, accent)
                canvas.drawLine(l, b, l, b - len, accent)
                canvas.drawLine(r, b, r - len, b, accent)
                canvas.drawLine(r, b, r, b - len, accent)
            }
            QrStyleConfig.FrameTemplate.BANNER -> {
                val bandH = qrSize * 0.1f
                val band = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = style.foregroundColor
                    this.style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, canvasW.toFloat(), bandH, band)
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = style.backgroundColor
                    textAlign = Paint.Align.CENTER
                    textSize = bandH * 0.45f
                    isFakeBoldText = true
                }
                canvas.drawText(
                    "SCAN ME",
                    canvasW / 2f,
                    bandH * 0.68f,
                    textPaint
                )
            }
            QrStyleConfig.FrameTemplate.PILL -> {
                val pillH = qrSize * 0.11f
                val pillTop = qrTop + qrSize + qrSize * 0.04f
                val pillW = qrSize * 0.55f
                val pillLeft = (canvasW - pillW) / 2f
                val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = style.foregroundColor
                    this.style = Paint.Style.FILL
                }
                canvas.drawRoundRect(
                    RectF(pillLeft, pillTop, pillLeft + pillW, pillTop + pillH),
                    pillH / 2f,
                    pillH / 2f,
                    pillPaint
                )
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = style.backgroundColor
                    textAlign = Paint.Align.CENTER
                    textSize = pillH * 0.42f
                    isFakeBoldText = true
                }
                canvas.drawText(
                    "SCAN ME",
                    canvasW / 2f,
                    pillTop + pillH * 0.68f,
                    textPaint
                )
            }
        }
    }

    private fun appendEyeSvg(
        sb: StringBuilder,
        fx: Int,
        fy: Int,
        cell: Float,
        style: QrStyleConfig,
        fg: String,
        bg: String
    ) {
        val left = fx * cell
        val top = fy * cell
        val size = cell * 7f
        val outerRx = when (style.eyeStyle) {
            QrStyleConfig.EyeStyle.SQUARE -> 0f
            QrStyleConfig.EyeStyle.SOFT -> size * 0.10f
            QrStyleConfig.EyeStyle.CIRCLE -> 0f
        }
        val midRx = when (style.eyeStyle) {
            QrStyleConfig.EyeStyle.SQUARE -> 0f
            QrStyleConfig.EyeStyle.SOFT -> size * 0.07f
            QrStyleConfig.EyeStyle.CIRCLE -> 0f
        }
        val inset = size * (1f / 7f)
        val coreInset = size * (2f / 7f)
        sb.append("""<rect x="$left" y="$top" width="$size" height="$size" rx="$outerRx" fill="$fg"/>""")
        sb.append(
            """<rect x="${left + inset}" y="${top + inset}" width="${size - inset * 2}" height="${size - inset * 2}" rx="$midRx" fill="$bg"/>"""
        )
        val coreX = left + coreInset
        val coreY = top + coreInset
        val coreS = size - coreInset * 2
        if (style.eyeStyle == QrStyleConfig.EyeStyle.CIRCLE) {
            val cx = coreX + coreS / 2f
            val cy = coreY + coreS / 2f
            val r = coreS / 2f
            sb.append("""<circle cx="$cx" cy="$cy" r="$r" fill="$fg"/>""")
        } else {
            sb.append(
                """<rect x="$coreX" y="$coreY" width="$coreS" height="$coreS" rx="${midRx * 0.6f}" fill="$fg"/>"""
            )
        }
    }

    private fun relativeLuminanceContrast(c1: Int, c2: Int): Double {
        val l1 = relativeLuminance(c1)
        val l2 = relativeLuminance(c2)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Int): Double {
        fun channel(c: Int): Double {
            val s = c / 255.0
            return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        val r = channel(Color.red(color))
        val g = channel(Color.green(color))
        val b = channel(Color.blue(color))
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun colorToHex(color: Int): String =
        String.format("#%06X", 0xFFFFFF and color)

    /** Scale logo to max dimension while keeping aspect. */
    fun prepareLogo(source: Bitmap, maxPx: Int): Bitmap {
        val scale = min(maxPx.toFloat() / source.width, maxPx.toFloat() / source.height)
        if (scale >= 1f) return source
        val w = (source.width * scale).toInt().coerceAtLeast(1)
        val h = (source.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, w, h, true)
    }
}
