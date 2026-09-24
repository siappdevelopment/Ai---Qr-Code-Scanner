package com.qrcode.scanner.ui.screens.scan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.FlipCameraAndroid
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.qrcode.scanner.ui.theme.BorderSubtle
import com.qrcode.scanner.ui.theme.CardSurface
import com.qrcode.scanner.ui.theme.CobaltPrimary
import com.qrcode.scanner.ui.theme.CobaltSoft
import com.qrcode.scanner.ui.theme.NestedSurface
import com.qrcode.scanner.ui.theme.PageBackground
import com.qrcode.scanner.ui.theme.PlusJakartaSans
import com.qrcode.scanner.ui.theme.TextPrimary
import com.qrcode.scanner.ui.theme.TextSecondary
import com.qrcode.scanner.ui.theme.White
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "ScannerViewfinder"

/**
 * Stitch: Camera Scanner Viewfinder — b12400fc841b48708d9fabb9c528a652
 * White + Electric Cobalt adaptation: solid colors only, no Profile / Ask AI / BottomNav.
 */
@Composable
fun ScannerViewfinderScreen(
    onBack: () -> Unit,
    onBarcodeDetected: (rawValue: String, format: Int, formatName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionDenied = !granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var torchEnabled by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var scanMode by remember { mutableStateOf(ScanMode.Qr) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val detectionHandled = remember { AtomicBoolean(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // After returning from ScanResult / Settings, reset detection gate and re-check permission.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                detectionHandled.set(false)
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                hasCameraPermission = granted
                if (granted) permissionDenied = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(torchEnabled, camera) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }
    LaunchedEffect(zoomRatio, camera) {
        val cam = camera ?: return@LaunchedEffect
        val zoomState = cam.cameraInfo.zoomState.value ?: return@LaunchedEffect
        val clamped = zoomRatio.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
        cam.cameraControl.setZoomRatio(clamped)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        when {
            hasCameraPermission -> {
                CameraPreviewHost(
                    lensFacing = lensFacing,
                    scanMode = scanMode,
                    detectionHandled = detectionHandled,
                    onCameraReady = { camera = it },
                    onBarcodeDetected = { raw, format, name ->
                        if (detectionHandled.compareAndSet(false, true)) {
                            onBarcodeDetected(raw, format, name)
                        }
                    }
                )
            }
            permissionDenied -> {
                PermissionDeniedPanel(
                    onRequestAgain = {
                        val activity = context as? Activity
                        val canShowDialog = activity == null ||
                            ActivityCompat.shouldShowRequestPermissionRationale(
                                activity,
                                Manifest.permission.CAMERA
                            )
                        if (canShowDialog) {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            // Permanent deny — open app settings so the user can retry.
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                            )
                        }
                    },
                    onBack = onBack
                )
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Requesting camera permission…",
                        color = TextSecondary,
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (hasCameraPermission) {
            ScannerHudOverlay(
                torchEnabled = torchEnabled,
                scanMode = scanMode,
                zoomRatio = zoomRatio,
                onBack = onBack,
                onToggleTorch = {
                    if (camera?.cameraInfo?.hasFlashUnit() == true) {
                        torchEnabled = !torchEnabled
                    }
                },
                onFlipCamera = {
                    torchEnabled = false
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                    detectionHandled.set(false)
                },
                onModeSelected = { mode ->
                    scanMode = mode
                    detectionHandled.set(false)
                },
                onZoomSelected = { zoomRatio = it }
            )
        }
    }
}

private enum class ScanMode { Qr, Barcode, Batch }

@Composable
private fun CameraPreviewHost(
    lensFacing: Int,
    scanMode: ScanMode,
    detectionHandled: AtomicBoolean,
    onCameraReady: (Camera?) -> Unit,
    onBarcodeDetected: (String, Int, String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            onCameraReady(null)
        }
    }

    LaunchedEffect(lensFacing, scanMode, lifecycleOwner) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        onCameraReady(null)

        val preview = Preview.Builder()
            .build()
            .also { it.surfaceProvider = previewView.surfaceProvider }

        val scanner = BarcodeScanning.getClient(barcodeOptionsFor(scanMode))
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analysis.setAnalyzer(analysisExecutor) { imageProxy ->
            processImageProxy(
                imageProxy = imageProxy,
                scanner = scanner,
                detectionHandled = detectionHandled,
                onBarcodeDetected = onBarcodeDetected
            )
        }

        try {
            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            val bound = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                analysis
            )
            onCameraReady(bound)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to bind camera", t)
            onCameraReady(null)
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

private suspend fun android.content.Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                cont.resume(future.get())
            } catch (t: Throwable) {
                cont.resumeWithException(t)
            }
        }, ContextCompat.getMainExecutor(this))
    }

private fun barcodeOptionsFor(mode: ScanMode): BarcodeScannerOptions {
    val formats = when (mode) {
        ScanMode.Qr -> intArrayOf(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_DATA_MATRIX
        )
        ScanMode.Barcode -> intArrayOf(
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_CODABAR,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_ITF,
            Barcode.FORMAT_PDF417
        )
        ScanMode.Batch -> intArrayOf(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_DATA_MATRIX,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_CODABAR,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_ITF,
            Barcode.FORMAT_PDF417
        )
    }
    return BarcodeScannerOptions.Builder()
        .setBarcodeFormats(formats[0], *formats.copyOfRange(1, formats.size))
        .build()
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    scanner: BarcodeScanner,
    detectionHandled: AtomicBoolean,
    onBarcodeDetected: (String, Int, String) -> Unit
) {
    if (detectionHandled.get()) {
        imageProxy.close()
        return
    }
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            if (detectionHandled.get()) return@addOnSuccessListener
            val hit = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                ?: return@addOnSuccessListener
            val raw = hit.rawValue ?: return@addOnSuccessListener
            onBarcodeDetected(raw, hit.format, formatName(hit.format))
        }
        .addOnFailureListener { e ->
            Log.w(TAG, "Barcode analyze failed", e)
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

private fun formatName(format: Int): String = when (format) {
    Barcode.FORMAT_QR_CODE -> "QR_CODE"
    Barcode.FORMAT_AZTEC -> "AZTEC"
    Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
    Barcode.FORMAT_PDF417 -> "PDF417"
    Barcode.FORMAT_CODE_128 -> "CODE_128"
    Barcode.FORMAT_CODE_39 -> "CODE_39"
    Barcode.FORMAT_CODE_93 -> "CODE_93"
    Barcode.FORMAT_CODABAR -> "CODABAR"
    Barcode.FORMAT_EAN_13 -> "EAN_13"
    Barcode.FORMAT_EAN_8 -> "EAN_8"
    Barcode.FORMAT_UPC_A -> "UPC_A"
    Barcode.FORMAT_UPC_E -> "UPC_E"
    Barcode.FORMAT_ITF -> "ITF"
    else -> "UNKNOWN"
}

@Composable
private fun ScannerHudOverlay(
    torchEnabled: Boolean,
    scanMode: ScanMode,
    zoomRatio: Float,
    onBack: () -> Unit,
    onToggleTorch: () -> Unit,
    onFlipCamera: () -> Unit,
    onModeSelected: (ScanMode) -> Unit,
    onZoomSelected: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HudIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Go back",
                size = 44.dp,
                onClick = onBack
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HudIconButton(
                    icon = if (torchEnabled) {
                        Icons.Outlined.FlashlightOn
                    } else {
                        Icons.Outlined.FlashlightOff
                    },
                    contentDescription = "Toggle flashlight",
                    selected = torchEnabled,
                    onClick = onToggleTorch
                )
                HudIconButton(
                    icon = Icons.Outlined.PhotoLibrary,
                    contentDescription = "Scan image from gallery",
                    onClick = { /* Gallery scan deferred to a later phase */ }
                )
                HudIconButton(
                    icon = Icons.Outlined.FlipCameraAndroid,
                    contentDescription = "Flip camera lens",
                    onClick = onFlipCamera
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(260.dp),
            contentAlignment = Alignment.Center
        ) {
            ReticleCorners()
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(1.dp)
                    .background(CobaltPrimary.copy(alpha = 0.35f))
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(CobaltPrimary.copy(alpha = 0.35f))
            )
            ScanLine()
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModePill(
                label = "QR Code",
                icon = Icons.Outlined.QrCode,
                selected = scanMode == ScanMode.Qr,
                onClick = { onModeSelected(ScanMode.Qr) }
            )
            ModePill(
                label = "Barcode",
                icon = Icons.Outlined.ViewWeek,
                selected = scanMode == ScanMode.Barcode,
                onClick = { onModeSelected(ScanMode.Barcode) }
            )
            ModePill(
                label = "Batch Scan",
                icon = Icons.Outlined.Layers,
                selected = scanMode == ScanMode.Batch,
                onClick = { onModeSelected(ScanMode.Batch) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .background(CardSurface, RoundedCornerShape(16.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CenterFocusStrong,
                    contentDescription = null,
                    tint = CobaltPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Align QR code or barcode inside the frame to scan automatically",
                    color = TextSecondary,
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1f, 2f, 5f).forEach { z ->
                    val selected = zoomRatio == z
                    Box(
                        modifier = Modifier
                            .background(
                                if (selected) CobaltSoft else NestedSurface,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (selected) CobaltPrimary else BorderSubtle,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onZoomSelected(z) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${z.toInt()}x",
                            color = if (selected) CobaltPrimary else TextSecondary,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanLine() {
    val transition = rememberInfiniteTransition(label = "scanLine")
    val offsetY by transition.animateFloat(
        initialValue = 16f,
        targetValue = 244f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLineY"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (offsetY - 130f).dp)
            .height(3.dp)
            .background(CobaltPrimary)
    )
}

@Composable
private fun ReticleCorners() {
    val thickness = 4.dp
    val arm = 32.dp
    val color = CobaltPrimary
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .width(arm)
                .height(thickness)
                .background(color, RoundedCornerShape(topStart = 4.dp))
        )
        Box(
            Modifier
                .align(Alignment.TopStart)
                .width(thickness)
                .height(arm)
                .background(color, RoundedCornerShape(topStart = 4.dp))
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .width(arm)
                .height(thickness)
                .background(color, RoundedCornerShape(topEnd = 4.dp))
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .width(thickness)
                .height(arm)
                .background(color, RoundedCornerShape(topEnd = 4.dp))
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .width(arm)
                .height(thickness)
                .background(color, RoundedCornerShape(bottomStart = 4.dp))
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .width(thickness)
                .height(arm)
                .background(color, RoundedCornerShape(bottomStart = 4.dp))
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .width(arm)
                .height(thickness)
                .background(color, RoundedCornerShape(bottomEnd = 4.dp))
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .width(thickness)
                .height(arm)
                .background(color, RoundedCornerShape(bottomEnd = 4.dp))
        )
    }
}

@Composable
private fun HudIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) CobaltPrimary else White)
            .border(
                1.dp,
                if (selected) CobaltPrimary else BorderSubtle,
                RoundedCornerShape(12.dp)
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) White else TextPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ModePill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) CobaltSoft else White)
            .border(
                1.dp,
                if (selected) CobaltPrimary else BorderSubtle,
                RoundedCornerShape(999.dp)
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) CobaltPrimary else TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = if (selected) CobaltPrimary else TextSecondary,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun PermissionDeniedPanel(
    onRequestAgain: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Camera permission required",
            color = TextPrimary,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Allow camera access to scan QR codes and barcodes.",
            color = TextSecondary,
            fontFamily = PlusJakartaSans,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(CobaltPrimary, RoundedCornerShape(12.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onRequestAgain
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Grant permission",
                color = White,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onBack
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Go back",
                color = TextPrimary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}
