package com.qrcode.scanner.ui.screens.scan

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import com.qrcode.scanner.ui.theme.enableThemedEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.Rotate90DegreesCw
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import com.qrcode.scanner.ui.theme.BorderSubtle
import com.qrcode.scanner.ui.theme.CardSurface
import com.qrcode.scanner.ui.theme.CobaltPrimary
import com.qrcode.scanner.ui.theme.CobaltSoft
import com.qrcode.scanner.ui.theme.NestedSurface
import com.qrcode.scanner.ui.components.AppBackButton
import com.qrcode.scanner.ui.components.appHeaderBackground
import com.qrcode.scanner.ui.theme.PageBackground
import com.qrcode.scanner.ui.theme.PlusJakartaSans
import com.qrcode.scanner.ui.theme.QRCodeScannerTheme
import com.qrcode.scanner.ui.components.ScreenWithAd
import com.qrcode.scanner.ui.theme.TextPrimary
import com.qrcode.scanner.ui.theme.TextSecondary
import com.qrcode.scanner.ui.theme.TextTertiary
import com.qrcode.scanner.ui.theme.White
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Stitch: Scan from Gallery & Crop (White Theme) — 63c5f18f8b71408e9d958ffc20a35888
 * Title corrected to "Crop Photo". Crop + Rotate + Flip only (no cloud / enhance / invert).
 */
class GalleryCropActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableThemedEdgeToEdge()
        val initialUri = intent.getStringExtra(ScanIntents.EXTRA_IMAGE_URI)?.let(Uri::parse)
        val autoPick = intent.getBooleanExtra(ScanIntents.EXTRA_AUTO_PICK, initialUri == null)
        val scanMode = intent.getStringExtra(ScanIntents.EXTRA_SCAN_MODE) ?: ScanIntents.MODE_BATCH

        setContent {
            QRCodeScannerTheme {
                ScreenWithAd(screenKey = "OtherScreen", nativeSize = "small") {
                GalleryCropScreen(
                    initialUri = initialUri,
                    autoPick = autoPick,
                    scanMode = scanMode,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onDetectedSingle = { code ->
                        startActivity(
                            ScanIntents.openScanResult(
                                context = this,
                                rawValue = code.rawValue,
                                format = code.format,
                                formatName = code.formatName
                            )
                        )
                        finish()
                    },
                    onDetectionFailed = { uri, reason ->
                        startActivity(
                            ScanIntents.openDetectionError(
                                context = this,
                                imageUri = uri,
                                reason = reason,
                                scanMode = scanMode
                            )
                        )
                        finish()
                    }
                )
                }
            }
        }
    }
}

@Composable
private fun GalleryCropScreen(
    initialUri: Uri?,
    autoPick: Boolean,
    scanMode: String,
    onBack: () -> Unit,
    onDetectedSingle: (GalleryScanHelper.DetectedCode) -> Unit,
    onDetectionFailed: (Uri?, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var imageUri by remember { mutableStateOf(initialUri) }
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rotation by remember { mutableIntStateOf(0) }
    var flipH by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var detecting by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var multiResults by remember { mutableStateOf<List<GalleryScanHelper.DetectedCode>?>(null) }

    // Normalized crop rect inside displayed image (0..1).
    // Default near-full frame so quiet zones / finder patterns stay intact.
    var cropLeft by remember { mutableFloatStateOf(0.04f) }
    var cropTop by remember { mutableFloatStateOf(0.04f) }
    var cropRight by remember { mutableFloatStateOf(0.96f) }
    var cropBottom by remember { mutableFloatStateOf(0.96f) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) {
            if (imageUri == null) onBack()
            return@rememberLauncherForActivityResult
        }
        imageUri = uri
        rotation = 0
        flipH = false
        cropLeft = 0.04f
        cropTop = 0.04f
        cropRight = 0.96f
        cropBottom = 0.96f
    }

    LaunchedEffect(Unit) {
        if (imageUri == null && autoPick) {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    LaunchedEffect(imageUri) {
        val uri = imageUri ?: return@LaunchedEffect
        loading = true
        loadError = null
        sourceBitmap = withContext(Dispatchers.IO) {
            GalleryScanHelper.decodeBitmap(context, uri)
        }
        loading = false
        if (sourceBitmap == null) {
            loadError = "Unable to open this image"
        }
    }

    fun runDetect() {
        val src = sourceBitmap
        val uri = imageUri
        if (src == null || uri == null || detecting) return
        detecting = true
        scope.launch {
            try {
                val transformed = withContext(Dispatchers.Default) {
                    GalleryScanHelper.transformBitmap(src, rotation, flipH)
                }
                val cropped = withContext(Dispatchers.Default) {
                    GalleryScanHelper.cropBitmap(
                        transformed,
                        cropLeft,
                        cropTop,
                        cropRight,
                        cropBottom
                    )
                }
                // Try cropped region first; if empty, fall back to full transformed image
                // (user may have cropped too tightly around a QR quiet zone).
                var hits = withContext(Dispatchers.Default) {
                    GalleryScanHelper.detect(cropped, scanMode)
                }
                if (hits.isEmpty() && (cropLeft > 0.01f || cropTop > 0.01f ||
                        cropRight < 0.99f || cropBottom < 0.99f)
                ) {
                    hits = withContext(Dispatchers.Default) {
                        GalleryScanHelper.detect(transformed, scanMode)
                    }
                }
                if (cropped !== src && cropped !== transformed && !cropped.isRecycled) {
                    cropped.recycle()
                }
                if (transformed !== src && !transformed.isRecycled) {
                    transformed.recycle()
                }

                when {
                    hits.isEmpty() -> onDetectionFailed(
                        cachedImageUri(context, src, uri),
                        "No scannable code detected"
                    )
                    hits.size == 1 -> onDetectedSingle(hits.first())
                    else -> multiResults = hits
                }
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Detection failed", Toast.LENGTH_SHORT).show()
                onDetectionFailed(
                    cachedImageUri(context, src, uri),
                    e.message ?: "Detection failed"
                )
            } finally {
                detecting = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .appHeaderBackground()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppBackButton(onClick = onBack)
            Text(
                text = "Crop Photo",
                color = TextPrimary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(NestedSurface)
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            when {
                loading -> CircularProgressIndicator(color = CobaltPrimary)
                loadError != null -> Text(
                    text = loadError!!,
                    color = TextSecondary,
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp
                )
                sourceBitmap != null -> {
                    val displayBmp = remember(sourceBitmap, rotation, flipH) {
                        GalleryScanHelper.transformBitmap(sourceBitmap!!, rotation, flipH)
                    }
                    CropCanvas(
                        bitmap = displayBmp,
                        cropLeft = cropLeft,
                        cropTop = cropTop,
                        cropRight = cropRight,
                        cropBottom = cropBottom,
                        onCropChange = { l, t, r, b ->
                            cropLeft = l
                            cropTop = t
                            cropRight = r
                            cropBottom = b
                        }
                    )
                }
                else -> Text(
                    text = "Select a photo to crop",
                    color = TextTertiary,
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp
                )
            }
            if (detecting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CobaltPrimary)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ToolChip(
                label = "Rotate 90°",
                icon = Icons.Outlined.Rotate90DegreesCw,
                modifier = Modifier.weight(1f),
                enabled = sourceBitmap != null && !detecting,
                onClick = { rotation = (rotation + 90) % 360 }
            )
            ToolChip(
                label = "Flip",
                icon = Icons.Outlined.Flip,
                modifier = Modifier.weight(1f),
                enabled = sourceBitmap != null && !detecting,
                onClick = { flipH = !flipH }
            )
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (sourceBitmap == null || detecting) NestedSurface else CobaltPrimary)
                .clickable(
                    enabled = sourceBitmap != null && !detecting,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { runDetect() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = if (sourceBitmap == null || detecting) TextTertiary else White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (detecting) "Detecting…" else "Scan Selection",
                    color = if (sourceBitmap == null || detecting) TextTertiary else White,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }

        TextButton(
            onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = "Choose another photo",
                color = CobaltPrimary,
                fontFamily = PlusJakartaSans,
                fontSize = 13.sp
            )
        }
    }

    multiResults?.let { results ->
        AlertDialog(
            onDismissRequest = { multiResults = null },
            title = {
                Text(
                    text = "Multiple codes found",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Select which code to open:",
                        color = TextSecondary,
                        fontFamily = PlusJakartaSans,
                        fontSize = 13.sp
                    )
                    results.forEachIndexed { index, code ->
                        Text(
                            text = "${index + 1}. ${code.formatName}: ${code.rawValue.take(48)}",
                            color = TextPrimary,
                            fontFamily = PlusJakartaSans,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    multiResults = null
                                    onDetectedSingle(code)
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { multiResults = null }) {
                    Text("Cancel", color = CobaltPrimary)
                }
            }
        )
    }
}

@Composable
private fun CropCanvas(
    bitmap: Bitmap,
    cropLeft: Float,
    cropTop: Float,
    cropRight: Float,
    cropBottom: Float,
    onCropChange: (Float, Float, Float, Float) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        val maxW = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()
        val bmpAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val boxAspect = maxW / maxH
        val drawW: Float
        val drawH: Float
        if (bmpAspect > boxAspect) {
            drawW = maxW
            drawH = maxW / bmpAspect
        } else {
            drawH = maxH
            drawW = maxH * bmpAspect
        }
        val density = LocalDensity.current
        val drawWdp = with(density) { drawW.toDp() }
        val drawHdp = with(density) { drawH.toDp() }
        val crop = rememberUpdatedState(floatArrayOf(cropLeft, cropTop, cropRight, cropBottom))
        val onCrop = rememberUpdatedState(onCropChange)

        Box(
            modifier = Modifier
                .size(drawWdp, drawHdp)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Selected photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(drawW, drawH) {
                        val slop = 28.dp.toPx()
                        var left = 0f
                        var top = 0f
                        var right = 0f
                        var bottom = 0f
                        var drag = CropDrag.None
                        detectDragGestures(
                            onDragStart = { offset ->
                                val current = crop.value
                                left = current[0]
                                top = current[1]
                                right = current[2]
                                bottom = current[3]
                                drag = cropDragTarget(
                                    offset,
                                    left,
                                    top,
                                    right,
                                    bottom,
                                    size.width.toFloat(),
                                    size.height.toFloat(),
                                    slop
                                )
                            },
                            onDrag = { change, dragAmount ->
                                if (drag == CropDrag.None) {
                                    return@detectDragGestures
                                }
                                change.consume()
                                val dx = dragAmount.x / size.width.toFloat().coerceAtLeast(1f)
                                val dy = dragAmount.y / size.height.toFloat().coerceAtLeast(1f)
                                val next = resizeCrop(left, top, right, bottom, dx, dy, drag)
                                left = next[0]
                                top = next[1]
                                right = next[2]
                                bottom = next[3]
                                onCrop.value(left, top, right, bottom)
                            }
                        )
                    }
            ) {
                val left = cropLeft * size.width
                val top = cropTop * size.height
                val right = cropRight * size.width
                val bottom = cropBottom * size.height
                // Dim outside crop
                drawRect(Color.Black.copy(alpha = 0.45f), Offset.Zero, Size(size.width, top))
                drawRect(
                    Color.Black.copy(alpha = 0.45f),
                    Offset(0f, bottom),
                    Size(size.width, size.height - bottom)
                )
                drawRect(
                    Color.Black.copy(alpha = 0.45f),
                    Offset(0f, top),
                    Size(left, bottom - top)
                )
                drawRect(
                    Color.Black.copy(alpha = 0.45f),
                    Offset(right, top),
                    Size(size.width - right, bottom - top)
                )
                drawRect(
                    color = CobaltPrimary,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(width = 3f)
                )
                val corner = 18f
                // Corner accents
                listOf(
                    Offset(left, top) to listOf(Offset(left + corner, top), Offset(left, top + corner)),
                    Offset(right, top) to listOf(Offset(right - corner, top), Offset(right, top + corner)),
                    Offset(left, bottom) to listOf(Offset(left + corner, bottom), Offset(left, bottom - corner)),
                    Offset(right, bottom) to listOf(
                        Offset(right - corner, bottom),
                        Offset(right, bottom - corner)
                    )
                ).forEach { (origin, pts) ->
                    pts.forEach { end ->
                        drawLine(CobaltPrimary, origin, end, strokeWidth = 5f)
                    }
                }
            }
        }
    }
}

private suspend fun cachedImageUri(context: Context, bitmap: Bitmap, fallback: Uri): Uri {
    return withContext(Dispatchers.IO) {
        GalleryScanHelper.cacheCopy(context, bitmap) ?: fallback
    }
}

private enum class CropDrag {
    None,
    Move,
    Left,
    Right,
    Top,
    Bottom,
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight
}

private fun cropDragTarget(
    offset: Offset,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    width: Float,
    height: Float,
    slop: Float
): CropDrag {
    val pxLeft = left * width
    val pxTop = top * height
    val pxRight = right * width
    val pxBottom = bottom * height
    val nearLeft = abs(offset.x - pxLeft) <= slop
    val nearRight = abs(offset.x - pxRight) <= slop
    val nearTop = abs(offset.y - pxTop) <= slop
    val nearBottom = abs(offset.y - pxBottom) <= slop
    val alongX = offset.x in (pxLeft - slop)..(pxRight + slop)
    val alongY = offset.y in (pxTop - slop)..(pxBottom + slop)
    return when {
        nearLeft && nearTop -> CropDrag.TopLeft
        nearRight && nearTop -> CropDrag.TopRight
        nearLeft && nearBottom -> CropDrag.BottomLeft
        nearRight && nearBottom -> CropDrag.BottomRight
        nearLeft && alongY -> CropDrag.Left
        nearRight && alongY -> CropDrag.Right
        nearTop && alongX -> CropDrag.Top
        nearBottom && alongX -> CropDrag.Bottom
        offset.x in pxLeft..pxRight && offset.y in pxTop..pxBottom -> CropDrag.Move
        else -> CropDrag.None
    }
}

private fun resizeCrop(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    dx: Float,
    dy: Float,
    drag: CropDrag
): FloatArray {
    val minSize = 0.12f
    var nextLeft = left
    var nextTop = top
    var nextRight = right
    var nextBottom = bottom
    when (drag) {
        CropDrag.Move -> {
            val width = right - left
            val height = bottom - top
            nextLeft = (left + dx).coerceIn(0f, 1f - width)
            nextTop = (top + dy).coerceIn(0f, 1f - height)
            nextRight = nextLeft + width
            nextBottom = nextTop + height
        }
        CropDrag.Left -> nextLeft = (left + dx).coerceIn(0f, right - minSize)
        CropDrag.Right -> nextRight = (right + dx).coerceIn(left + minSize, 1f)
        CropDrag.Top -> nextTop = (top + dy).coerceIn(0f, bottom - minSize)
        CropDrag.Bottom -> nextBottom = (bottom + dy).coerceIn(top + minSize, 1f)
        CropDrag.TopLeft -> {
            nextLeft = (left + dx).coerceIn(0f, right - minSize)
            nextTop = (top + dy).coerceIn(0f, bottom - minSize)
        }
        CropDrag.TopRight -> {
            nextRight = (right + dx).coerceIn(left + minSize, 1f)
            nextTop = (top + dy).coerceIn(0f, bottom - minSize)
        }
        CropDrag.BottomLeft -> {
            nextLeft = (left + dx).coerceIn(0f, right - minSize)
            nextBottom = (bottom + dy).coerceIn(top + minSize, 1f)
        }
        CropDrag.BottomRight -> {
            nextRight = (right + dx).coerceIn(left + minSize, 1f)
            nextBottom = (bottom + dy).coerceIn(top + minSize, 1f)
        }
        CropDrag.None -> Unit
    }
    return floatArrayOf(nextLeft, nextTop, nextRight, nextBottom)
}

@Composable
private fun ToolChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) CobaltPrimary else TextTertiary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = if (enabled) TextPrimary else TextTertiary,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
