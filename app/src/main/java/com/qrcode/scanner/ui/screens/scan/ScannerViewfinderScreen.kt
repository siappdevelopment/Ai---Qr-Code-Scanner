package com.qrcode.scanner.ui.screens.scan

import android.Manifest
import android.app.Activity
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.qrcode.scanner.data.settings.SettingsPreferences
import com.qrcode.scanner.data.settings.SettingsRepositoryProvider
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
import kotlinx.coroutines.isActive
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
    onFinishContinuousBatch: (List<ContinuousBatchItem>) -> Unit = {},
    modifier: Modifier = Modifier,
    safeContentPadding: PaddingValues? = null
) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepositoryProvider.get(context) }
    // Null until first DataStore emission — avoids treating Continuous as OFF while prefs load.
    var settingsPreferences by remember { mutableStateOf<SettingsPreferences?>(null) }
    LaunchedEffect(settingsRepository) {
        settingsRepository.preferences.collect { settingsPreferences = it }
    }
    val prefs = settingsPreferences
    val preferencesReady = prefs != null
    // Setting removed. A saved ON value must not swallow scans with no result screen.
    val continuousBatchScan = false

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
    val scanMode = ScanMode.Batch
    var camera by remember { mutableStateOf<Camera?>(null) }
    val detectionHandled = remember { AtomicBoolean(false) }
    val continuousSession = remember { ContinuousBatchSession() }
    val continuousFinishStarted = remember { AtomicBoolean(false) }
    val continuousItems by continuousSession.items.collectAsStateWithLifecycle()
    val latestContinuousBatchScan by rememberUpdatedState(continuousBatchScan)
    val latestOnFinishContinuousBatch by rememberUpdatedState(onFinishContinuousBatch)
    val lifecycleOwner = (context as? ComponentActivity) ?: LocalLifecycleOwner.current

    fun finishContinuousBatchSession() {
        // Idempotent: double Done/Back must not open an empty batch or finish twice.
        if (!continuousFinishStarted.compareAndSet(false, true)) return
        val snapshot = continuousSession.snapshot()
        continuousSession.clear()
        latestOnFinishContinuousBatch(snapshot)
    }

    // Continuous session lives for this Scanner visit only (not DataStore / Room).
    DisposableEffect(Unit) {
        onDispose { continuousSession.clear() }
    }

    // Deterministic Continuous preference transitions (after prefs are ready).
    var previousContinuous by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(preferencesReady, continuousBatchScan) {
        if (!preferencesReady) return@LaunchedEffect
        val prev = previousContinuous
        previousContinuous = continuousBatchScan
        when {
            prev == null && continuousBatchScan -> {
                // First resolved value is ON — start with an empty session.
                continuousSession.clear()
                detectionHandled.set(false)
            }
            prev == false && continuousBatchScan -> {
                // OFF → ON: fresh continuous session.
                continuousSession.clear()
                detectionHandled.set(false)
            }
            prev == true && !continuousBatchScan -> {
                // ON → OFF: review accepted items; do not silently discard.
                if (continuousSession.size() > 0) {
                    finishContinuousBatchSession()
                } else {
                    continuousSession.clear()
                    detectionHandled.set(false)
                }
            }
        }
    }

    // Continuous ON + accepted codes: Back finishes/reviews the batch instead of dropping it.
    BackHandler(enabled = continuousBatchScan && continuousItems.isNotEmpty()) {
        finishContinuousBatchSession()
    }

    val cameraState = rememberUpdatedState(camera)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                cameraState.value?.cameraControl?.enableTorch(false)
                torchEnabled = false
            }
            if (event == Lifecycle.Event.ON_RESUME) {
                // Single-shot gate only; Continuous session is retained across resume.
                if (!latestContinuousBatchScan) {
                    detectionHandled.set(false)
                }
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                hasCameraPermission = granted
                if (granted) permissionDenied = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            cameraState.value?.cameraControl?.enableTorch(false)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
            .background(if (hasCameraPermission) Color.Black else PageBackground)
    ) {
        when {
            hasCameraPermission -> {
                CameraPreviewHost(
                    lensFacing = lensFacing,
                    scanMode = scanMode,
                    continuousBatchScan = continuousBatchScan,
                    continuousSession = continuousSession,
                    detectionHandled = detectionHandled,
                    detectionEnabled = true,
                    onCameraReady = { camera = it },
                    onBarcodeDetected = { raw, format, name ->
                        // Single-shot OFF path — unchanged once prefs are ready.
                        if (detectionHandled.compareAndSet(false, true)) {
                            DetectionFeedback.onAcceptedDetection(
                                context = context,
                                vibrateEnabled = prefs?.vibrateOnDetection != false,
                                beepEnabled = prefs?.beepOnDetection == true
                            )
                            onBarcodeDetected(raw, format, name)
                        }
                    },
                    onContinuousAccepted = { raw, format, name ->
                        // Continuous ON — stay on scanner; no Result navigation.
                        DetectionFeedback.onAcceptedDetection(
                            context = context,
                            vibrateEnabled = prefs?.vibrateOnDetection == true,
                            beepEnabled = prefs?.beepOnDetection == true
                        )
                        Log.i(
                            TAG,
                            "continuous accept format=$name rawLen=${raw.length} session=${continuousSession.size()}"
                        )
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
                safeContentPadding = safeContentPadding,
                torchEnabled = torchEnabled,
                zoomRatio = zoomRatio,
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
                onZoomSelected = { zoomRatio = it },
                onOpenGallery = {
                    context.startActivity(
                        ScanIntents.openGalleryCrop(
                            context = context,
                            imageUri = null,
                            scanMode = ScanIntents.MODE_BATCH
                        )
                    )
                }
            )
        }
    }
}

private enum class ScanMode { Qr, Barcode, Batch }

@Composable
private fun CameraPreviewHost(
    lensFacing: Int,
    scanMode: ScanMode,
    continuousBatchScan: Boolean,
    continuousSession: ContinuousBatchSession,
    detectionHandled: AtomicBoolean,
    detectionEnabled: Boolean,
    onCameraReady: (Camera?) -> Unit,
    onBarcodeDetected: (String, Int, String) -> Unit,
    onContinuousAccepted: (String, Int, String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = (context as? ComponentActivity) ?: LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderHolder = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val boundCamera = remember { mutableStateOf<Camera?>(null) }
    val latestOnBarcodeDetected by rememberUpdatedState(onBarcodeDetected)
    val latestOnContinuousAccepted by rememberUpdatedState(onContinuousAccepted)
    val latestContinuousBatchScan by rememberUpdatedState(continuousBatchScan)
    val latestDetectionEnabled by rememberUpdatedState(detectionEnabled)
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            setBackgroundColor(android.graphics.Color.BLACK)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val cam = boundCamera.value
            val provider = cameraProviderHolder.value
            boundCamera.value = null
            val executor = ContextCompat.getMainExecutor(context)
            try {
                if (cam != null) {
                    cam.cameraControl.enableTorch(false).addListener({
                        provider?.unbindAll()
                    }, executor)
                } else {
                    provider?.unbindAll()
                }
            } catch (_: Throwable) {
                provider?.unbindAll()
            }
            analysisExecutor.shutdown()
            onCameraReady(null)
        }
    }

    LaunchedEffect(lensFacing, scanMode, lifecycleOwner) {
        val cameraProvider = context.getCameraProvider()
        if (!isActive) return@LaunchedEffect
        cameraProviderHolder.value = cameraProvider
        cameraProvider.unbindAll()
        onCameraReady(null)

        val preview = Preview.Builder()
            .build()
            .also { it.surfaceProvider = previewView.surfaceProvider }

        val scanner = BarcodeScanning.getClient(barcodeOptionsFor(scanMode))
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()

        analysis.setAnalyzer(analysisExecutor) { imageProxy ->
            processImageProxy(
                imageProxy = imageProxy,
                scanner = scanner,
                continuousBatchScan = latestContinuousBatchScan,
                continuousSession = continuousSession,
                detectionHandled = detectionHandled,
                detectionEnabled = latestDetectionEnabled,
                onBarcodeDetected = { raw, format, name ->
                    latestOnBarcodeDetected(raw, format, name)
                },
                onContinuousAccepted = { raw, format, name ->
                    latestOnContinuousAccepted(raw, format, name)
                }
            )
        }

        if (!isActive) return@LaunchedEffect
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
            if (!isActive) {
                boundCamera.value = null
                cameraProvider.unbindAll()
                return@LaunchedEffect
            }
            boundCamera.value = bound
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
    continuousBatchScan: Boolean,
    continuousSession: ContinuousBatchSession,
    detectionHandled: AtomicBoolean,
    detectionEnabled: Boolean,
    onBarcodeDetected: (String, Int, String) -> Unit,
    onContinuousAccepted: (String, Int, String) -> Unit
) {
    // Wait for Settings prefs before accepting any detection (Continuous path must be correct).
    if (!detectionEnabled) {
        imageProxy.close()
        return
    }
    // Single-shot only: skip further ML work after the first accepted detection.
    if (!continuousBatchScan && detectionHandled.get()) {
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
            if (continuousBatchScan) {
                for (barcode in barcodes) {
                    val raw = barcode.rawValue ?: continue
                    if (raw.isBlank()) continue
                    val name = formatName(barcode.format)
                    if (continuousSession.tryAccept(raw, barcode.format, name)) {
                        onContinuousAccepted(raw, barcode.format, name)
                    }
                }
            } else {
                if (detectionHandled.get()) return@addOnSuccessListener
                val hit = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                    ?: return@addOnSuccessListener
                val raw = hit.rawValue ?: return@addOnSuccessListener
                onBarcodeDetected(raw, hit.format, formatName(hit.format))
            }
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
    safeContentPadding: PaddingValues?,
    torchEnabled: Boolean,
    zoomRatio: Float,
    onToggleTorch: () -> Unit,
    onFlipCamera: () -> Unit,
    onZoomSelected: (Float) -> Unit,
    onOpenGallery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (safeContentPadding != null) {
                    Modifier.padding(safeContentPadding)
                } else {
                    Modifier
                        .statusBarsPadding()
                        .navigationBarsPadding()
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HudIconButton(
                icon = if (torchEnabled) {
                    Icons.Outlined.FlashlightOn
                } else {
                    Icons.Outlined.FlashlightOff
                },
                label = "FLASH",
                contentDescription = "Toggle flashlight",
                selected = torchEnabled,
                onClick = onToggleTorch
            )
            HudIconButton(
                icon = Icons.Outlined.PhotoLibrary,
                label = "GALLERY",
                contentDescription = "Scan image from gallery",
                onClick = onOpenGallery
            )
            HudIconButton(
                icon = Icons.Outlined.FlipCameraAndroid,
                label = "FLIP",
                contentDescription = "Flip camera lens",
                onClick = onFlipCamera
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(268.dp),
            contentAlignment = Alignment.Center
        ) {
            ReticleCorners()
            ScanLine()
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 20.dp)
                .background(CardSurface, RoundedCornerShape(999.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(1f, 2f, 5f).forEach { z ->
                val selected = zoomRatio == z
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 40.dp)
                        .background(
                            if (selected) CobaltPrimary else Color.Transparent,
                            RoundedCornerShape(999.dp)
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { onZoomSelected(z) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${z.toInt()}x",
                        color = if (selected) White else TextSecondary,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
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
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    selected: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick
        )
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (selected) Color.White.copy(alpha = 0.32f) else Color.Black.copy(alpha = 0.45f)
                )
                .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = White,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            color = White,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.4.sp,
            modifier = Modifier.padding(top = 6.dp)
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
