package com.example.ui.components

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import com.example.core.CyberLUT
import com.example.core.FalseColorPalette
import com.example.core.FramingGridStyle
import com.example.core.CameraLens
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * CameraViewport renders the actual viewfinder feed.
 * It features BOTH a high-end Simulated Cyber-Matrix Scene (perfect for emulator display/interactive control feedback)
 * AND an authentic live sensor feed using CameraX if simulation mode is toggled off!
 */
@Composable
fun CameraViewport(
    simulationMode: Boolean,
    shutterSpeed: String,
    iso: Int,
    whiteBalanceTemp: Int,
    tint: Int,
    focalDistance: Float,
    isFocusPeaking: Boolean,
    isFalseColor: Boolean,
    isZebra: Boolean,
    framingGrid: FramingGridStyle,
    selectedLUT: CyberLUT,
    selectedMode: com.example.core.CameraMode = com.example.core.CameraMode.PHOTO,
    selectedLens: com.example.core.CameraLens = com.example.core.CameraLens.BACK,
    zoomLevel: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.clipToBounds()) {
        if (!simulationMode) {
            // Live Android CameraX Viewfinder
            AndroidCameraPreview(selectedLens = selectedLens, zoomLevel = zoomLevel)
        } else {
            // Cyber Interactive Simulator Canvas
            CyberSimulatorView(
                shutterSpeed = shutterSpeed,
                iso = iso,
                wbTemp = whiteBalanceTemp,
                tint = tint,
                focalDistance = focalDistance,
                isFocusPeaking = isFocusPeaking,
                isFalseColor = isFalseColor,
                isZebra = isZebra,
                selectedLUT = selectedLUT,
                selectedMode = selectedMode,
                selectedLens = selectedLens,
                zoomLevel = zoomLevel
            )
        }

        // Standard Overlay for Framing Grids
        FramingGridOverlay(style = framingGrid)
    }
}

@Composable
fun AndroidCameraPreview(selectedLens: com.example.core.CameraLens, zoomLevel: Float = 1.0f) {
    if (selectedLens == com.example.core.CameraLens.DUAL) {
        var isSwapped by remember { mutableStateOf(false) }
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }

        // Dual view - Show Back and Front camera in Picture-in-Picture layout
        Box(modifier = Modifier.fillMaxSize()) {
            val bgSelector = if (isSwapped) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            val pipSelector = if (isSwapped) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

            // Background Camera
            AndroidCameraPreviewSingle(
                cameraSelector = bgSelector,
                zoomLevel = if (!isSwapped) zoomLevel else 1.0f,
                modifier = Modifier.fillMaxSize()
            )
            
            // Floating Card Picture-In-Picture (Draggable & Swappable)
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
                    .width(110.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
                    .clickable {
                        isSwapped = !isSwapped
                    }
            ) {
                AndroidCameraPreviewSingle(
                    cameraSelector = pipSelector,
                    zoomLevel = if (isSwapped) zoomLevel else 1.0f,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isSwapped) "MAIN" else "SELFIE",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    } else {
        // Single view finder
        val cameraSelector = if (selectedLens == com.example.core.CameraLens.FRONT) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        AndroidCameraPreviewSingle(cameraSelector = cameraSelector, zoomLevel = zoomLevel, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun AndroidCameraPreviewSingle(
    cameraSelector: CameraSelector,
    zoomLevel: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var activeCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(cameraSelector) {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            try {
                cameraProvider.unbindAll()
                activeCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(zoomLevel, activeCamera) {
        activeCamera?.cameraControl?.setZoomRatio(zoomLevel)
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

@Composable
fun CyberSimulatorView(
    shutterSpeed: String,
    iso: Int,
    wbTemp: Int,
    tint: Int,
    focalDistance: Float,
    isFocusPeaking: Boolean,
    isFalseColor: Boolean,
    isZebra: Boolean,
    selectedLUT: CyberLUT,
    selectedMode: com.example.core.CameraMode = com.example.core.CameraMode.PHOTO,
    selectedLens: com.example.core.CameraLens = com.example.core.CameraLens.BACK,
    zoomLevel: Float = 1.0f
) {
    // Continuous spinning/pulsing animation state for rendering the mock telemetry vector objects
    val transition = rememberInfiniteTransition(label = "cyber_sensor")
    val rotationAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val wavePulse by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_pulse"
    )

    // Dynamic Grain noise state (random seed shifting)
    var grainTicker by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            grainTicker++
        }
    }

    // Capture standard simulated parameters
    // Brightness factor from Shutter calculation (faster shutter = darker)
    val shutterDenominator = shutterSpeed.substringAfter("/", "1").toFloatOrNull() ?: 1f
    val shutterExposureFactor = (150f / shutterDenominator).coerceIn(0.2f, 2.5f)
    // Higher ISO = brighter but noisier
    val isoBrightnessFactor = (iso / 400f).coerceIn(0.5f, 3.0f)
    val globalExposure = (shutterExposureFactor * isoBrightnessFactor).coerceIn(0.1f, 3.5f)

    // White Balance Kelvin shifts
    val warmFactor = ((wbTemp - 5600) / 6000f).coerceIn(-0.8f, 0.8f) // cooler to warmer
    val tintFactor = (tint / 100f).coerceIn(-1.0f, 1.0f) // green to magenta

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070908))
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2

        // Determine base visual overlay filter color based on White BalanceTemp & Tint
        val wbColorFilter = Color(
            red = (0.7f + warmFactor + if (tintFactor > 0) tintFactor * 0.2f else 0f).coerceIn(0f, 1f),
            green = (0.7f - warmFactor / 2f + if (tintFactor < 0) -tintFactor * 0.2f else 0f).coerceIn(0f, 1f),
            blue = (0.8f - warmFactor).coerceIn(0f, 1f),
            alpha = 1.0f
        )

        if (selectedLens == com.example.core.CameraLens.FRONT) {
            // Draw radial tech structure grid for Selfie viewfinder
            for (i in 1..6) {
                drawCircle(
                    color = Color(0xFF10B981).copy(alpha = 0.08f),
                    radius = i * 85f,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 0.5.dp.toPx())
                )
            }
            // Draw selfie visage avatar in the center
            drawMockSelfieFace(
                centerX = centerX,
                centerY = centerY - 10f,
                radius = 180f,
                tint = Color(0xFF10B981)
            )
        } else if (selectedLens == com.example.core.CameraLens.DUAL) {
            // Draw standard rear scenery as background (scaled by zoomLevel)
            scale(zoomLevel, Offset(centerX, centerY)) {
                drawSimulatedCyberScenery(
                    width = width,
                    height = height,
                    rotationAngle = rotationAngle,
                    wavePulse = wavePulse,
                    focalDistance = focalDistance,
                    isFocusPeaking = isFocusPeaking,
                    isFalseColor = isFalseColor,
                    isZebra = isZebra,
                    selectedLUT = selectedLUT,
                    globalExposure = globalExposure,
                    wbColorFilter = wbColorFilter,
                    selectedMode = selectedMode
                )
            }

            // Picture-in-Picture selfie box (Top-right corner)
            val pipW = 210f
            val pipH = 310f
            val pipX = width - pipW - 35f
            val pipY = 35f

            // Background card & solid border for PIP
            drawRect(
                color = Color(0xFF070908).copy(alpha = 0.92f),
                topLeft = Offset(pipX, pipY),
                size = Size(pipW, pipH)
            )
            drawRect(
                color = Color(0xFFFACC15),
                topLeft = Offset(pipX, pipY),
                size = Size(pipW, pipH),
                style = Stroke(width = 1.6.dp.toPx())
            )

            // Tech circles inside PIP
            for (i in 1..2) {
                drawCircle(
                    color = Color(0xFFFACC15).copy(alpha = 0.08f),
                    radius = i * 50f,
                    center = Offset(pipX + pipW / 2, pipY + pipH / 2),
                    style = Stroke(width = 0.5.dp.toPx())
                )
            }

            // Draw micro selfie avatar visage inside PIP
            drawMockSelfieFace(
                centerX = pipX + pipW / 2,
                centerY = pipY + pipH / 2 + 10f,
                radius = 72f,
                tint = Color(0xFFFACC15)
            )
        } else {
            // BACK Camera scenery (Normal, scaled by zoomLevel)
            scale(zoomLevel, Offset(centerX, centerY)) {
                drawSimulatedCyberScenery(
                    width = width,
                    height = height,
                    rotationAngle = rotationAngle,
                    wavePulse = wavePulse,
                    focalDistance = focalDistance,
                    isFocusPeaking = isFocusPeaking,
                    isFalseColor = isFalseColor,
                    isZebra = isZebra,
                    selectedLUT = selectedLUT,
                    globalExposure = globalExposure,
                    wbColorFilter = wbColorFilter,
                    selectedMode = selectedMode
                )
            }
        }

        // Draw animated scanning laser lines & HUD items
        drawSimulatedLaserHUD(
            width = width,
            height = height,
            rotationAngle = rotationAngle,
            wavePulse = wavePulse,
            isFalseColor = isFalseColor,
            isFocusPeaking = isFocusPeaking,
            focalDistance = focalDistance
        )

        // Draw camera mode-specific aesthetic overlay guides
        drawCameraModeOverlays(
            selectedMode = selectedMode,
            width = width,
            height = height,
            wavePulse = wavePulse
        )

        // Apply ISO visual noise (analog grain overlay)
        if (!isFalseColor) {
            val grainCount = (iso / 40).coerceIn(10, 800)
            val random = java.util.Random(grainTicker.toLong())
            for (i in 0 until grainCount) {
                val gx = random.nextFloat() * width
                val gy = random.nextFloat() * height
                val gAlpha = random.nextFloat() * 0.18f * (iso / 1600f).coerceIn(0.2f, 1.0f)
                drawCircle(
                    color = Color.White.copy(alpha = gAlpha),
                    radius = (1.5f + (iso / 800f)).dp.toPx(),
                    center = Offset(gx, gy)
                )
            }
        }
    }
}

private fun DrawScope.drawCameraModeOverlays(
    selectedMode: com.example.core.CameraMode,
    width: Float,
    height: Float,
    wavePulse: Float
) {
    val centerX = width / 2f
    val centerY = height / 2f

    when (selectedMode) {
        com.example.core.CameraMode.PORTRAIT -> {
            // Draw a gorgeous golden Portrait focus ring with double lines and dashes
            val goldenYellow = Color(0xFFFACC15)
            drawCircle(
                color = goldenYellow.copy(alpha = 0.4f),
                radius = 150f,
                center = Offset(centerX, centerY),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), wavePulse * 20f)
                )
            )
            drawCircle(
                color = goldenYellow.copy(alpha = 0.2f),
                radius = 156f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 0.8.dp.toPx())
            )
            // Yellow focus corners
            val cornerSize = 25f
            val offsetDist = 180f
            // Top-left corner
            drawLine(goldenYellow.copy(alpha = 0.8f), Offset(centerX - offsetDist, centerY - offsetDist), Offset(centerX - offsetDist + cornerSize, centerY - offsetDist), 2f)
            drawLine(goldenYellow.copy(alpha = 0.8f), Offset(centerX - offsetDist, centerY - offsetDist), Offset(centerX - offsetDist, centerY - offsetDist + cornerSize), 2f)
            // Top-right corner
            drawLine(goldenYellow.copy(alpha = 0.8f), Offset(centerX + offsetDist, centerY - offsetDist), Offset(centerX + offsetDist - cornerSize, centerY - offsetDist), 2f)
            drawLine(goldenYellow.copy(alpha = 0.8f), Offset(centerX + offsetDist, centerY - offsetDist), Offset(centerX + offsetDist, centerY - offsetDist + cornerSize), 2f)
            // Bottom-left corner
            drawLine(goldenYellow.copy(alpha = 0.8f), Offset(centerX - offsetDist, centerY + offsetDist), Offset(centerX - offsetDist + cornerSize, centerY + offsetDist), 2f)
            drawLine(goldenYellow.copy(alpha = 0.8f), Offset(centerX - offsetDist, centerY + offsetDist), Offset(centerX - offsetDist, centerY + offsetDist - cornerSize), 2f)
            // Bottom-right corner
            drawLine(goldenYellow.copy(alpha = 0.8f), Offset(centerX + offsetDist, centerY + offsetDist), Offset(centerX + offsetDist - cornerSize, centerY + offsetDist), 2f)
            drawLine(goldenYellow.copy(alpha = 0.8f), Offset(centerX + offsetDist, centerY + offsetDist), Offset(centerX + offsetDist, centerY + offsetDist - cornerSize), 2f)
        }
        com.example.core.CameraMode.NIGHT -> {
            // Draw a cyan Night indicator with high-tech moon rings
            val neonCyan = Color(0xFF22D3EE)
            drawCircle(
                color = neonCyan.copy(alpha = 0.25f),
                radius = 180f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )
            // Draw crescent Moon
            val moonCenter = Offset(centerX - 10f, centerY - 240f)
            drawCircle(
                color = neonCyan.copy(alpha = 0.8f),
                radius = 16f,
                center = moonCenter
            )
            drawCircle(
                color = Color(0xFF070908), // Back scenery color matches dark core
                radius = 14f,
                center = Offset(moonCenter.x + 6f, moonCenter.y - 4f)
            )
        }
        com.example.core.CameraMode.PANORAMA -> {
            // Horizontal sliding guide line with tick marks
            val neonGreen = Color(0xFF00FF66)
            drawLine(
                color = neonGreen.copy(alpha = 0.6f),
                start = Offset(60f, centerY),
                end = Offset(width - 60f, centerY),
                strokeWidth = 2.dp.toPx()
            )
            // Ticks across the slider
            val stepX = (width - 120f) / 10f
            for (i in 0..10) {
                val tx = 60f + i * stepX
                val tyOffset = if (i == 5) 20f else 10f
                drawLine(
                    color = neonGreen.copy(alpha = if (i == 5) 0.9f else 0.4f),
                    start = Offset(tx, centerY - tyOffset),
                    end = Offset(tx, centerY + tyOffset),
                    strokeWidth = if (i == 5) 2.dp.toPx() else 1.dp.toPx()
                )
            }
            // Draw a central tracking arrow
            val arrowX = centerX + wavePulse * 140f
            val path = Path().apply {
                moveTo(arrowX, centerY - 35f)
                lineTo(arrowX + 12f, centerY - 25f)
                lineTo(arrowX - 12f, centerY - 25f)
                close()
            }
            drawPath(path = path, color = neonGreen)
        }
        com.example.core.CameraMode.CINEMATIC -> {
            // Red tracking autofocus indicators
            val neonRed = Color(0xFFEF4444)
            val boxW = 140f
            val boxH = 100f
            val shiftX = wavePulse * 90f
            val shiftY = sin(wavePulse * Math.PI.toFloat()) * 40f
            
            drawRect(
                color = neonRed.copy(alpha = 0.7f),
                topLeft = Offset(centerX - boxW/2 + shiftX, centerY - boxH/2 + shiftY),
                size = Size(boxW, boxH),
                style = Stroke(width = 1.2.dp.toPx())
            )
            
            // Tiny crosshair in autofocus tracking box
            val trackingX = centerX + shiftX
            val trackingY = centerY + shiftY
            drawLine(neonRed.copy(alpha = 0.7f), Offset(trackingX - 10f, trackingY), Offset(trackingX + 10f, trackingY), 1.dp.toPx())
            drawLine(neonRed.copy(alpha = 0.7f), Offset(trackingX, trackingY - 10f), Offset(trackingX, trackingY + 10f), 1.dp.toPx())
        }
        com.example.core.CameraMode.TIME_LAPSE -> {
            // Yellow tiny warning rings or standard dots
            val warningRed = Color(0xFFFACC15)
            drawCircle(
                color = warningRed.copy(alpha = 0.15f * (wavePulse + 1.1f) / 2f),
                radius = 220f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        com.example.core.CameraMode.SLO_MO -> {
            // Blue fluid capture bounds
            val softBlue = Color(0xFF3B82F6)
            drawCircle(
                color = softBlue.copy(alpha = 0.3f),
                radius = 200f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        else -> {} // Normal photo/video gets natural default grids
    }
}

private fun DrawScope.drawSimulatedCyberScenery(
    width: Float,
    height: Float,
    rotationAngle: Float,
    wavePulse: Float,
    focalDistance: Float, // 0 is auto, 0.1 - 1.0 is manual.
    isFocusPeaking: Boolean,
    isFalseColor: Boolean,
    isZebra: Boolean,
    selectedLUT: CyberLUT,
    globalExposure: Float,
    wbColorFilter: Color,
    selectedMode: com.example.core.CameraMode = com.example.core.CameraMode.PHOTO
) {
    val centerX = width / 2
    val centerY = height / 2

    val isBlurMode = selectedMode == com.example.core.CameraMode.PORTRAIT || selectedMode == com.example.core.CameraMode.CINEMATIC
    val bonusBlur = if (isBlurMode) 16.0f else 0.0f

    // Simulated Depth of Field blur factor
    // Object A (Scifi City Grid) is sharpest at focalDistance = 0.3f
    // Object B (Spherical Reactor Core) is sharpest at focalDistance = 0.7f
    val fD = if (focalDistance == 0.0f) 0.5f else focalDistance
    val blurCity = ((Math.abs(fD - 0.3f) * 15f) + bonusBlur).coerceIn(0f, 30f)
    val blurCore = ((Math.abs(fD - 0.7f) * 15f) + bonusBlur).coerceIn(0f, 30f)

    // Apply LUT properties to baseline hues
    val baseReactorColor = when (selectedLUT) {
        CyberLUT.NEUTRAL -> Color(0xFF00FFCC)
        CyberLUT.TEAL_ORANGE -> Color(0xFFFF9400) // Amber orange
        CyberLUT.MONOCHROME -> Color(0xFFDCDCDC) // High silver grey
        CyberLUT.CYBERPUNK -> Color(0xFFE200FF) // Purple magenta
    }

    val baseGridColor = when (selectedLUT) {
        CyberLUT.NEUTRAL -> Color(0xFF0077FF)
        CyberLUT.TEAL_ORANGE -> Color(0xFF00E5FF) // Sky teal
        CyberLUT.MONOCHROME -> Color(0xFF7C7C7C) // Silver flat grey
        CyberLUT.CYBERPUNK -> Color(0xFF00FF66) // Neon toxic green
    }

    // 0. Draw Ambient Bokeh Balls behind everything when Portrait / Cinematic shallow depth of field is selected
    if (isBlurMode) {
        val bokehColors = listOf(
            baseGridColor.copy(alpha = 0.12f),
            baseReactorColor.copy(alpha = 0.12f),
            Color(0xFFFB7185).copy(alpha = 0.10f), // Pale rose / coral
            Color(0xFF38BDF8).copy(alpha = 0.08f), // Soft cyan
            Color(0xFFFBBF24).copy(alpha = 0.08f)  // Soft gold
        )
        val random = java.util.Random(42L)
        for (i in 0 until 18) {
            val cx = width * random.nextFloat() + (kotlin.math.sin(wavePulse * 0.4f + i) * 35f)
            val cy = height * random.nextFloat() + (kotlin.math.cos(wavePulse * 0.25f - i) * 25f)
            val radius = 50f + random.nextFloat() * 120f
            val bColor = bokehColors[i % bokehColors.size]
            
            // Outer blurred halo circle
            drawCircle(
                color = bColor,
                radius = radius,
                center = Offset(cx, cy)
            )
            // Inner specular light highlight ring
            drawCircle(
                color = Color.White.copy(alpha = 0.03f),
                radius = radius * 0.82f,
                center = Offset(cx - radius * 0.12f, cy - radius * 0.12f)
            )
        }
    }

    // 1. Draw Simulated Skyline / Cyber grid lines
    val gridOffset = wavePulse * 50f
    val gridLinesCount = 14
    for (i in 0 until gridLinesCount) {
        val y = centerY + (i * 30) - 100
        val cellExposure = (y / height) * globalExposure * 0.6f
        var strokeColor = baseGridColor.copy(alpha = (0.28f / (blurCity + 1f)).coerceIn(0.01f, 0.7f) * cellExposure)
        var strokeWidth = (2.5f - blurCity * 0.1f).coerceAtLeast(0.5f)

        // False Color exposure analyzer
        if (isFalseColor) {
            val visualLuminance = (0.2f * cellExposure).coerceIn(0.0f, 1.0f)
            strokeColor = FalseColorPalette.getColorForBrightness(visualLuminance)
        }

        // Draw perspective horizontal grids
        val path = Path().apply {
            moveTo(0f, y + gridOffset)
            quadraticTo(centerX, y + gridOffset - 60, width, y + gridOffset)
        }
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = strokeWidth)
        )

        // Focus Peaking highlights focused edges (City grid focused at fD ~0.3)
        if (isFocusPeaking && blurCity < 2.0f && !isFalseColor) {
            val peakPath = Path().apply {
                moveTo(0f, y + gridOffset)
                quadraticTo(centerX, y + gridOffset - 60, width, y + gridOffset)
            }
            drawPath(
                path = peakPath,
                color = Color(0xFF00FF66), // Sharp Green focus edges
                style = Stroke(width = 1.5f * (2.0f - blurCity))
            )
        }
    }

    // 2. Draw Sphere Cyber-Reactor Core Scene (Focused near 0.7f)
    val circleRadius = (110f + wavePulse * 8f)
    val coreExposure = globalExposure * 1.3f

    // Zebra overexposure warning pattern for high spot lights
    val isCoreOverexposed = isZebra && coreExposure > 1.8f

    var coreColor = baseReactorColor.copy(alpha = (0.85f / (blurCore + 1f)).coerceIn(0.1f, 1.0f))
    // Blend with white balance filter
    if (!isFalseColor) {
        val rCombined = (coreColor.red * wbColorFilter.red * coreExposure).coerceIn(0f, 1f)
        val gCombined = (coreColor.green * wbColorFilter.green * coreExposure).coerceIn(0f, 1f)
        val bCombined = (coreColor.blue * wbColorFilter.blue * coreExposure).coerceIn(0f, 1f)
        coreColor = Color(rCombined, gCombined, bCombined, coreColor.alpha)
    } else {
        // False Color mapping for reactor core highlights
        val coreLuminance = (0.6f * coreExposure).coerceIn(0.0f, 1.0f)
        coreColor = FalseColorPalette.getColorForBrightness(coreLuminance)
    }

    // Draw main spherical HUD center core
    drawCircle(
        color = coreColor,
        radius = circleRadius,
        center = Offset(centerX, centerY),
        style = Stroke(width = (4f - blurCore * 0.15f).coerceAtLeast(1.0f))
    )

    // Focused Peaking highlight on reactor rings if blurCore is low
    if (isFocusPeaking && blurCore < 2.0f && !isFalseColor) {
        drawCircle(
            color = Color(0xFF00FF66), // Focus peak color
            radius = circleRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2.0f * (2.0f - blurCore))
        )
    }

    // Zebra striping draw inside core if overexposed and Zebra is ticked on
    if (isCoreOverexposed) {
        // Overlay zebra stripes
        drawZebraStripes(
            center = Offset(centerX, centerY),
            radius = circleRadius,
            wavePulse = wavePulse
        )
    }

    // Draw rotating holographic spikes inside core
    rotate(rotationAngle, pivot = Offset(centerX, centerY)) {
        val lineCount = 3
        for (i in 0 until lineCount) {
            val angleRad = Math.toRadians((i * (360 / lineCount)).toDouble())
            val startX = centerX + (circleRadius - 35f) * Math.cos(angleRad).toFloat()
            val startY = centerY + (circleRadius - 35f) * Math.sin(angleRad).toFloat()
            val endX = centerX + (circleRadius + 15f) * Math.cos(angleRad).toFloat()
            val endY = centerY + (circleRadius + 15f) * Math.sin(angleRad).toFloat()

            var spikeColor = baseReactorColor.copy(alpha = if (isFalseColor) 1.0f else 0.7f)
            if (isFalseColor) {
                spikeColor = FalseColorPalette.getColorForBrightness(0.4f * coreExposure)
            }

            drawLine(
                color = spikeColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = (3.5f - blurCore * 0.1f).coerceAtLeast(1.0f)
            )

            if (isFocusPeaking && blurCore < 1.5f && !isFalseColor) {
                drawLine(
                    color = Color(0xFF00FF66),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 1.2f
                )
            }
        }
    }
}

private fun DrawScope.drawZebraStripes(
    center: Offset,
    radius: Float,
    wavePulse: Float
) {
    // Zebra draws diagonal yellow-and-black flashing warning lines strictly clipped inside circle bounds.
    // Standard canvas clipPath is a powerful way to restrict graphic bounds!
    val stripePath = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
    }
    drawContext.canvas.save()
    drawContext.canvas.clipPath(stripePath)

    val stripeSpacing = 20f
    val offset = wavePulse * 15f
    var sx = center.x - radius * 1.5f
    while (sx < center.x + radius * 1.5f) {
        drawLine(
            color = Color(0xFFFFCC00), // Safety yellow zebra lines
            start = Offset(sx + offset, center.y - radius),
            end = Offset(sx - 20f + offset, center.y + radius),
            strokeWidth = 6f
        )
        sx += stripeSpacing
    }
    drawContext.canvas.restore()
}

private fun DrawScope.drawSimulatedLaserHUD(
    width: Float,
    height: Float,
    rotationAngle: Float,
    wavePulse: Float,
    isFalseColor: Boolean,
    isFocusPeaking: Boolean,
    focalDistance: Float
) {
    val centerX = width / 2
    val centerY = height / 2

    // Simple cosmetic scanning bar passing from left to right
    val scanX = (centerX + wavePulse * (width / 2.2f))
    val barColor = if (isFalseColor) {
        FalseColorPalette.getColorForBrightness(0.9f)
    } else {
        if (isFocusPeaking) Color(0xFF00FF66).copy(alpha = 0.22f) else Color(0x3300FF99)
    }

    // Vertical sweeping scanner bar
    drawLine(
        color = barColor,
        start = Offset(scanX, 0f),
        end = Offset(scanX, height),
        strokeWidth = 1.5.dp.toPx()
    )

    // Center focal cursor overlay element
    val curSize = 25f
    val centerHUDColor = if (isFocusPeaking) Color(0xFF00FF66) else Color.White.copy(alpha = 0.7f)
    drawLine(centerHUDColor, Offset(centerX - curSize, centerY), Offset(centerX - 8f, centerY), 2f)
    drawLine(centerHUDColor, Offset(centerX + 8f, centerY), Offset(centerX + curSize, centerY), 2f)
    drawLine(centerHUDColor, Offset(centerX, centerY - curSize), Offset(centerX, centerY - 8f), 2f)
    drawLine(centerHUDColor, Offset(centerX, centerY + 8f), Offset(centerX, centerY + curSize), 2f)

    // Focal range status tag printed at the bottom of target cursor
    if (focalDistance > 0.0f) {
        // Display numeric focal metrics visually on the grid
        val fMText = String.format("%.2fm [M]", focalDistance * 5.0f)
    }
}

@Composable
fun FramingGridOverlay(style: FramingGridStyle) {
    if (style == FramingGridStyle.NONE) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        when (style) {
            FramingGridStyle.THIRD_RULE -> {
                val gridColor = Color.White.copy(alpha = 0.38f)
                val stroke = 1.dp.toPx()

                // Vertical lines
                drawLine(gridColor, Offset(width / 3f, 0f), Offset(width / 3f, height), stroke)
                drawLine(gridColor, Offset(2 * width / 3f, 0f), Offset(2 * width / 3f, height), stroke)

                // Horizontal lines
                drawLine(gridColor, Offset(0f, height / 3f), Offset(width, height / 3f), stroke)
                drawLine(gridColor, Offset(0f, 2 * height / 3f), Offset(width, 2 * height / 3f), stroke)
            }
            FramingGridStyle.CROSSHAIR -> {
                val gridColor = Color(0xFFFFB800).copy(alpha = 0.5f) // Safety yellow crosshair
                val stroke = 1.dp.toPx()
                val center = Offset(width / 2, height / 2)

                drawLine(gridColor, Offset(center.x - 40f, center.y), Offset(center.x + 40f, center.y), stroke)
                drawLine(gridColor, Offset(center.x, center.y - 40f), Offset(center.x, center.y + 40f), stroke)
                drawCircle(gridColor, radius = 10f, center = center, style = Stroke(stroke))
            }
            FramingGridStyle.CINEMATIC_2_39 -> {
                // Draws dark translucent letterboxes top and bottom to frame 2.39:1 cinemascope active viewport!
                val activeHeight = width / 2.39f
                val marginHeight = (height - activeHeight) / 2f
                if (marginHeight > 0f) {
                    // Top letterbox
                    drawRect(
                        color = Color.Black.copy(alpha = 0.78f),
                        topLeft = Offset(0f, 0f),
                        size = Size(width, marginHeight)
                    )
                    // Bottom letterbox
                    drawRect(
                        color = Color.Black.copy(alpha = 0.78f),
                        topLeft = Offset(0f, height - marginHeight),
                        size = Size(width, marginHeight)
                    )

                    // Cinematic framing guidelines (cropped red guides)
                    val lineStroke = 1.dp.toPx()
                    drawLine(Color.Red.copy(alpha = 0.5f), Offset(0f, marginHeight), Offset(width, marginHeight), lineStroke)
                    drawLine(Color.Red.copy(alpha = 0.5f), Offset(0f, height - marginHeight), Offset(width, height - marginHeight), lineStroke)
                }
            }
            else -> {}
        }
    }
}

private fun DrawScope.drawMockSelfieFace(centerX: Float, centerY: Float, radius: Float, tint: Color) {
    // Draw outer avatar dash circular boundary
    drawCircle(
        color = tint.copy(alpha = 0.35f),
        radius = radius,
        center = Offset(centerX, centerY),
        style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
    )
    
    // Draw face/head oval
    drawOval(
        color = tint.copy(alpha = 0.12f),
        topLeft = Offset(centerX - radius * 0.5f, centerY - radius * 0.7f),
        size = Size(radius * 1.0f, radius * 1.3f)
    )
    drawOval(
        color = tint.copy(alpha = 0.6f),
        topLeft = Offset(centerX - radius * 0.5f, centerY - radius * 0.7f),
        size = Size(radius * 1.0f, radius * 1.3f),
        style = Stroke(width = 1.5.dp.toPx())
    )
    
    // Draw friendly eyes
    drawCircle(
        color = tint.copy(alpha = 0.8f),
        radius = radius * 0.08f,
        center = Offset(centerX - radius * 0.2f, centerY - radius * 0.15f)
    )
    drawCircle(
        color = tint.copy(alpha = 0.8f),
        radius = radius * 0.08f,
        center = Offset(centerX + radius * 0.2f, centerY - radius * 0.15f)
    )
    
    // Draw smiling mouth arc
    val smilePath = Path().apply {
        arcTo(
            rect = Rect(
                left = centerX - radius * 0.25f,
                top = centerY + radius * 0.02f,
                right = centerX + radius * 0.25f,
                bottom = centerY + radius * 0.35f
            ),
            startAngleDegrees = 0f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
    }
    drawPath(
        path = smilePath,
        color = tint.copy(alpha = 0.85f),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )
    
    // Draw elegant shoulder curves out to the edge
    val shoulderPath = Path().apply {
        moveTo(centerX - radius * 0.8f, centerY + radius * 1.1f)
        quadraticTo(
            centerX - radius * 0.4f, centerY + radius * 0.65f,
            centerX - radius * 0.3f, centerY + radius * 0.65f
        )
        lineTo(centerX + radius * 0.3f, centerY + radius * 0.65f)
        quadraticTo(
            centerX + radius * 0.4f, centerY + radius * 0.65f,
            centerX + radius * 0.8f, centerY + radius * 1.1f
        )
        close()
    }
    drawPath(
        path = shoulderPath,
        color = tint.copy(alpha = 0.08f)
    )
    drawPath(
        path = shoulderPath,
        color = tint.copy(alpha = 0.5f),
        style = Stroke(width = 1.2.dp.toPx())
    )
}
