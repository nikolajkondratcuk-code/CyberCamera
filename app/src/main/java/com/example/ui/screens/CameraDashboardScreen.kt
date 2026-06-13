package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.*
import com.example.ui.components.CameraViewport
import com.example.ui.components.ParameterDialSlider
import com.example.ui.components.TechnicalAnalyzers
import com.example.viewmodel.CameraViewModel
import kotlinx.coroutines.delay

import com.example.ui.theme.GlassBackgroundBrush

@Composable
fun CameraDashboardScreen(
    viewModel: CameraViewModel,
    hasPermissions: Boolean = false,
    onRequestPermissions: () -> Unit = {},
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect settings reactively
    val shutterSpeed by viewModel.shutterSpeed.collectAsStateWithLifecycle()
    val iso by viewModel.iso.collectAsStateWithLifecycle()
    val whiteBalanceTemp by viewModel.whiteBalanceTemp.collectAsStateWithLifecycle()
    val tint by viewModel.tint.collectAsStateWithLifecycle()
    val focalDistance by viewModel.focalDistance.collectAsStateWithLifecycle()
    val ev by viewModel.ev.collectAsStateWithLifecycle()

    val isFocusPeaking by viewModel.isFocusPeaking.collectAsStateWithLifecycle()
    val isFalseColor by viewModel.isFalseColor.collectAsStateWithLifecycle()
    val isZebra by viewModel.isZebra.collectAsStateWithLifecycle()
    val framingGrid by viewModel.framingGrid.collectAsStateWithLifecycle()
    val selectedLUT by viewModel.selectedLUT.collectAsStateWithLifecycle()

    val activeCodec by viewModel.codec.collectAsStateWithLifecycle()
    val activeResolution by viewModel.resolution.collectAsStateWithLifecycle()
    val activeFrameRate by viewModel.frameRate.collectAsStateWithLifecycle()

    val audioSource by viewModel.audioSource.collectAsStateWithLifecycle()
    val audioGainDb by viewModel.audioGainDb.collectAsStateWithLifecycle()

    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordingSeconds by viewModel.recordingSeconds.collectAsStateWithLifecycle()
    val batteryLevel by viewModel.batteryLevel.collectAsStateWithLifecycle()
    val simulationMode by viewModel.simulationMode.collectAsStateWithLifecycle()
    val storageBytes by viewModel.availableStorageBytes.collectAsStateWithLifecycle()
    val isChatVisible by viewModel.isChatVisible.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()

    val isCapturingPhoto by viewModel.isCapturingPhoto.collectAsStateWithLifecycle()
    val photoCaptureProgress by viewModel.photoCaptureProgress.collectAsStateWithLifecycle()
    val photoCaptureStatus by viewModel.photoCaptureStatus.collectAsStateWithLifecycle()
    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val selectedLens by viewModel.selectedLens.collectAsStateWithLifecycle()
    val zoomLevel by viewModel.zoomLevel.collectAsStateWithLifecycle()

    // Control tabs: which parameter wheel dial to display
    // Options: "shutter", "iso", "wb", "tint", "focus"
    var selectedDialParam by remember { mutableStateOf("shutter") }

    // Dialog sheets
    var showFormatDialog by remember { mutableStateOf(false) }
    var showMonitoringDialog by remember { mutableStateOf(false) }

    var triggerFlash by remember { mutableStateOf(false) }
    LaunchedEffect(isCapturingPhoto) {
        if (isCapturingPhoto) {
            triggerFlash = true
            delay(120)
            triggerFlash = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GlassBackgroundBrush)
            .statusBarsPadding()
    ) {
        // 1. TOP BAR STATUS HUD
        CameraHubTopStatusBar(
            isRecording = isRecording,
            durationSeconds = recordingSeconds,
            batteryPercent = (batteryLevel * 100).toInt(),
            storageGb = storageBytes / 1_000_000_000f,
            fps = activeFrameRate.fps,
            codec = activeCodec,
            resolution = activeResolution,
            simulationMode = simulationMode,
            onToggleSimulation = {
                if (simulationMode && !hasPermissions) {
                    onRequestPermissions()
                } else {
                    viewModel.toggleSimulationMode()
                }
            },
            onFormatClick = { showFormatDialog = true }
        )

        // 1b. BLACKMAGIC INTERACTIVE PARAMETERS RIBBON
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F1216).copy(alpha = 0.9f))
                .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f)))
                .padding(vertical = 4.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ParameterBlockHeader(
                label = "FPS",
                value = "${activeFrameRate.fps}",
                isSelected = false,
                onClick = { showFormatDialog = true }
            )
            ParameterBlockHeader(
                label = "SHUTTER",
                value = shutterSpeed,
                isSelected = selectedDialParam == "shutter",
                onClick = { selectedDialParam = "shutter" }
            )
            ParameterBlockHeader(
                label = "ISO",
                value = "$iso",
                isSelected = selectedDialParam == "iso",
                onClick = { selectedDialParam = "iso" }
            )
            ParameterBlockHeader(
                label = "WB",
                value = "${whiteBalanceTemp}K",
                isSelected = selectedDialParam == "wb",
                onClick = { selectedDialParam = "wb" }
            )
            ParameterBlockHeader(
                label = "TINT",
                value = if (tint >= 0) "+$tint" else "$tint",
                isSelected = selectedDialParam == "tint",
                onClick = { selectedDialParam = "tint" }
            )
            ParameterBlockHeader(
                label = "FOCUS",
                value = if (focalDistance == 0.0f) "AUTO" else "${String.format("%.1f", focalDistance)}m",
                isSelected = selectedDialParam == "focus",
                onClick = { selectedDialParam = "focus" }
            )
        }

        // 2. PRIMARY VIEWPORT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
                .border(1.dp, Color(0xFF1F1F1F))
        ) {
            CameraViewport(
                simulationMode = simulationMode,
                shutterSpeed = shutterSpeed,
                iso = iso,
                whiteBalanceTemp = whiteBalanceTemp,
                tint = tint,
                focalDistance = focalDistance,
                isFocusPeaking = isFocusPeaking,
                isFalseColor = isFalseColor,
                isZebra = isZebra,
                framingGrid = framingGrid,
                selectedLUT = selectedLUT,
                selectedMode = selectedMode,
                selectedLens = selectedLens,
                zoomLevel = zoomLevel,
                modifier = Modifier.fillMaxSize()
            )

            // Apple-style Translucent Lens Selector (0.5x, 1x, 2x, 5x)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(24.dp))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val zoomOptions = listOf(0.5f, 1.0f, 2.0f, 5.0f)
                zoomOptions.forEach { opt ->
                    val isSelected = zoomLevel == opt
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color.Black.copy(alpha = 0.2f))
                            .clickable {
                                viewModel.setZoomLevel(opt)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (opt == 0.5f) ".5" else if (opt == 1.0f) "1x" else if (opt == 2.0f) "2" else "5",
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }

            // Side Quick Utilities Overlay
            QuickAccessToolbarVertical(
                isPeaking = isFocusPeaking,
                onTogglePeaking = { viewModel.toggleFocusPeaking() },
                isFalseColor = isFalseColor,
                onToggleFalseColor = { viewModel.toggleFalseColor() },
                isZebra = isZebra,
                onToggleZebra = { viewModel.toggleZebra() },
                selectedLUT = selectedLUT,
                onLUTSelected = { viewModel.setLUT(it) },
                isChatActive = isChatVisible,
                onToggleChat = { viewModel.toggleChat() },
                onMonitoringClick = { showMonitoringDialog = true }
            )

            // Cloud Collaborative Crew Chat Overlay
            CloudCrewChatOverlay(
                isVisible = isChatVisible,
                messages = chatMessages,
                onSendMessage = { viewModel.sendChatMessage(it) },
                onDismiss = { viewModel.toggleChat() }
            )

            // Tactile Shutter Flash Visual Overlay
            if (triggerFlash) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.92f))
                )
            }

            // Frosted Glass Multi-frame HDR Fusion Overlay
            if (isCapturingPhoto) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(enabled = false) {}, // Block user touch
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0F1216).copy(alpha = 0.9f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .widthIn(max = 330.dp)
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // High-speed 3D cascading stacked miniatures
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(55.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val cardCount = 9
                                for (i in 0 until cardCount) {
                                    val collapseFactor = (1.0f - photoCaptureProgress).coerceIn(0f, 1f)
                                    val staggerOffset = (i - 4) * 20f * collapseFactor
                                    val staggerAlpha = if (collapseFactor > 0.05f) {
                                        0.35f + (i * 0.05f)
                                    } else {
                                        if (i == 4) 1.0f else 0.0f
                                    }
                                    val scaleAmt = 1.0f - (collapseFactor * 0.04f * Math.abs(i - 4))

                                    Box(
                                        modifier = Modifier
                                            .offset(x = staggerOffset.dp, y = (collapseFactor * -4f * Math.abs(i - 4)).dp)
                                            .scale(scaleAmt)
                                            .size(width = 65.dp, height = 44.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Black.copy(alpha = staggerAlpha))
                                            .border(
                                                BorderStroke(
                                                    width = 1.dp,
                                                    color = if (i == 4) Color(0xFFA3E635) else Color.White.copy(alpha = staggerAlpha * 0.5f)
                                                ),
                                                shape = RoundedCornerShape(4.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "RAW #${i + 1}",
                                            fontSize = 6.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = staggerAlpha * 0.7f),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            // Circular stack loading outer ring
                            Box(
                                modifier = Modifier.size(56.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = photoCaptureProgress,
                                    color = Color(0xFFA3E635),
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Icon(
                                    imageVector = Icons.Default.FlipCameraAndroid,
                                    contentDescription = "Fusing exposures",
                                    tint = Color(0xFFA3E635),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "HDR+ BURST FUSION STACK",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.5.sp,
                                    color = Color(0xFFA3E635),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "9 high-speed RAW frames combined (A17+ C++)",
                                    fontSize = 8.sp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                            }
 
                            // Real-time progress status readout
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(vertical = 6.dp, horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = photoCaptureStatus,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. TECHNICAL MONITORING / HISTOGRAM & AUDIO PANEL
        TechnicalAnalyzers(
            isRecording = isRecording,
            audioGainDb = audioGainDb,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )

        // 4. MAIN CAMERA CONTROL SELECTION WHEEL (ROBUST SLIDER)
        ParameterControlWheelSection(
            selectedDialParam = selectedDialParam,
            shutterSpeed = shutterSpeed,
            iso = iso,
            whiteBalanceTemp = whiteBalanceTemp,
            tint = tint,
            focalDistance = focalDistance,
            onShutterSelected = { viewModel.setShutterSpeed(it) },
            onIsoSelected = { viewModel.setIso(it.toInt()) },
            onWbSelected = { viewModel.setWhiteBalanceTemp(it.replace("K", "").toInt()) },
            onTintSelected = { viewModel.setTint(it.toInt()) },
            onFocusSelected = {
                val num = if (it == "CAF (Auto)") 0.0f else it.replace("m", "").toFloatOrNull() ?: 0.5f
                viewModel.setFocalDistance(num)
            }
        )

        // 5. PARAMETER TAB BUTTONS (Select ISO, SHUTTER, WB, TINT, FOCUS)
        ParameterSelectorCarousel(
            selectedDialParam = selectedDialParam,
            onParamSelected = { selectedDialParam = it },
            shutterSpeed = shutterSpeed,
            iso = iso,
            wb = whiteBalanceTemp,
            lockSettingsExpanded = isRecording
        )

        // Interactive dynamic tactile Camera Mode Selector (Sliding iPhone ribbon)
        CameraModeSelector(
            selectedMode = selectedMode,
            onModeSelected = { viewModel.setCameraMode(it) },
            enabled = !isRecording && !isCapturingPhoto
        )

        // 6. BOTTOM CONSOLE BAR / REC BUTTON & NAVIGATION
        CameraBottomTriggerConsole(
            isRecording = isRecording,
            isCapturingPhoto = isCapturingPhoto,
            selectedMode = selectedMode,
            selectedLens = selectedLens,
            onFlipCamera = { viewModel.cycleCameraLens() },
            onToggleDualCam = {
                if (selectedLens == CameraLens.DUAL) {
                    viewModel.setCameraLens(CameraLens.BACK)
                } else {
                    viewModel.setCameraLens(CameraLens.DUAL)
                }
            },
            onRecClick = { viewModel.toggleRecording() },
            onPhotoClick = { viewModel.captureFusedPhoto() },
            onNavigateToLibrary = {
                // Prevent leaving screen when film recording is active
                if (!isRecording && !isCapturingPhoto) onNavigateToLibrary()
            }
        )
    }

    // format popover dialog dialog
    if (showFormatDialog) {
        CameraFormatDialog(
            currentCodec = activeCodec,
            currentRes = activeResolution,
            currentFps = activeFrameRate,
            onCodecChange = { viewModel.setCodec(it) },
            onResChange = { viewModel.setResolution(it) },
            onFpsChange = { viewModel.setFrameRate(it) },
            onDismiss = { showFormatDialog = false }
        )
    }

    // detailed accessory popover dialog dialogue
    if (showMonitoringDialog) {
        CameraMonitoringAccDialog(
            isPeaking = isFocusPeaking,
            onTogglePeaking = { viewModel.toggleFocusPeaking() },
            isFalseColor = isFalseColor,
            onToggleFalseColor = { viewModel.toggleFalseColor() },
            isZebra = isZebra,
            onToggleZebra = { viewModel.toggleZebra() },
            selectedLUT = selectedLUT,
            onLUTSelected = { viewModel.setLUT(it) },
            framingGrid = framingGrid,
            onGridSelected = { viewModel.setFramingGrid(it) },
            audioSource = audioSource,
            audioGainDb = audioGainDb,
            onAudioSourceSelected = { viewModel.setAudioSource(it) },
            onAudioGainChanged = { viewModel.setAudioGain(it) },
            onDismiss = { showMonitoringDialog = false }
        )
    }
}

@Composable
fun CameraHubTopStatusBar(
    isRecording: Boolean,
    durationSeconds: Int,
    batteryPercent: Int,
    storageGb: Float,
    fps: Int,
    codec: VideoCodec,
    resolution: VideoResolution,
    simulationMode: Boolean,
    onToggleSimulation: () -> Unit,
    onFormatClick: () -> Unit
) {
    val hrs = durationSeconds / 3600
    val mins = (durationSeconds % 3600) / 60
    val secs = durationSeconds % 60
    val frames = (System.currentTimeMillis() % 1000 / 41).toInt() % fps // simulated rolling camera frame segments
    val timecode = String.format("%02d:%02d:%02d:%02d", hrs, mins, secs, frames)

    // Beautiful glowing background for parameters status bar
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f))
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Red Recording Status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isRecording) {
                // Pulsing dot
                val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(alpha)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.4f))
                )
            }

            Text(
                text = timecode,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (isRecording) Color.Red else Color.White,
                modifier = Modifier.testTag("timecode_view")
            )

            if (isRecording) {
                Text(
                    text = "REC",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Red,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Active spec format shortcut button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(4.dp))
                .clickable(enabled = !isRecording) { onFormatClick() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val shortCode = codec.label.substringAfter("Apple ").substringBefore(" /")
            val shortRes = when (resolution) {
                VideoResolution.UHD_4K -> "4K"
                VideoResolution.FHD_1080P -> "1080p"
                VideoResolution.HD_720P -> "720p"
            }
            Text(
                text = "$shortCode | $shortRes | ${fps}p",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isRecording) Color.White.copy(alpha = 0.4f) else Color(0xFF22D3EE),
                fontSize = 9.sp
            )
            if (!isRecording) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Format dialog dropdown",
                    tint = Color(0xFF22D3EE),
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // Sensor Feed toggle button (Simulator vs Physical Android Hardware camera)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (simulationMode) Color(0x2222D3EE) else Color.White.copy(alpha = 0.08f))
                .border(BorderStroke(0.5.dp, if (simulationMode) Color(0xFF22D3EE).copy(alpha = 0.30f) else Color.White.copy(alpha = 0.15f)), RoundedCornerShape(4.dp))
                .clickable { onToggleSimulation() }
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (simulationMode) Icons.Default.Tv else Icons.Default.PhotoCamera,
                contentDescription = "sensor mode toggle button",
                tint = if (simulationMode) Color(0xFF22D3EE) else Color.White,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = if (simulationMode) "SIMULATOR" else "HARDWARE",
                color = if (simulationMode) Color(0xFF22D3EE) else Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp
            )
        }

        // Space & Battery Telemetry indicators
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Memory space left
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.SdCard,
                    contentDescription = "disk space remaining icon",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = String.format("%.1f GB", storageGb),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Battery telemetry
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = when {
                        batteryPercent > 80 -> Icons.Default.BatteryFull
                        batteryPercent > 30 -> Icons.Default.Battery4Bar
                        else -> Icons.Default.BatteryAlert
                    },
                    contentDescription = "battery meter telemetry",
                    tint = if (batteryPercent > 20) Color(0xFF00FF66) else Color.Red,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "$batteryPercent%",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun QuickAccessToolbarVertical(
    isPeaking: Boolean,
    onTogglePeaking: () -> Unit,
    isFalseColor: Boolean,
    onToggleFalseColor: () -> Unit,
    isZebra: Boolean,
    onToggleZebra: () -> Unit,
    selectedLUT: CyberLUT,
    onLUTSelected: (CyberLUT) -> Unit,
    isChatActive: Boolean,
    onToggleChat: () -> Unit,
    onMonitoringClick: () -> Unit
) {
    var expandedLutSelector by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(55.dp)
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Focus Peaking
        QuickToolIconButton(
            icon = Icons.Default.FilterCenterFocus,
            label = "PEAK",
            active = isPeaking,
            onClick = onTogglePeaking,
            tag = "quick_peak_btn"
        )

        // 2. False Color
        QuickToolIconButton(
            icon = Icons.Default.ColorLens,
            label = "COLOR",
            active = isFalseColor,
            onClick = onToggleFalseColor,
            tag = "quick_false_btn"
        )

        // 3. Zebra
        QuickToolIconButton(
            icon = Icons.Default.Pattern,
            label = "ZEBRA",
            active = isZebra,
            onClick = onToggleZebra,
            tag = "quick_zebra_btn"
        )

        // 4. LUT cinematic shortcut
        Box {
            QuickToolIconButton(
                icon = Icons.Default.MovieFilter,
                label = "LUTS",
                active = selectedLUT != CyberLUT.NEUTRAL,
                onClick = { expandedLutSelector = !expandedLutSelector },
                tag = "quick_lut_btn"
            )

            DropdownMenu(
                expanded = expandedLutSelector,
                onDismissRequest = { expandedLutSelector = false },
                modifier = Modifier.background(Color(0xFF141414))
            ) {
                CyberLUT.values().forEach { lut ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = lut.label,
                                color = if (selectedLUT == lut) Color(0xFF00FF66) else Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        onClick = {
                            onLUTSelected(lut)
                            expandedLutSelector = false
                        },
                        modifier = Modifier.testTag("lut_option_${lut.name.lowercase()}")
                    )
                }
            }
        }

        // 5. Cloud Chat shortcut
        QuickToolIconButton(
            icon = Icons.Default.Forum,
            label = "CHAT",
            active = isChatActive,
            onClick = onToggleChat,
            tag = "quick_chat_btn"
        )

        Spacer(modifier = Modifier.weight(1f))

        // 6. Gear button for opening full settings sheet
        QuickToolIconButton(
            icon = Icons.Default.Tune,
            label = "MONITOR",
            active = false,
            onClick = onMonitoringClick,
            tag = "quick_acc_btn"
        )
    }
}

@Composable
fun QuickToolIconButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (active) Color(0xFF00FF66).copy(alpha = 0.2f) else Color(0x3F1A1A1A))
                .border(
                    width = 1.dp,
                    color = if (active) Color(0xFF00FF66) else Color.White.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) Color(0xFF00FF66) else Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 7.sp,
            color = if (active) Color(0xFF00FF66) else Color.White.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ParameterControlWheelSection(
    selectedDialParam: String,
    shutterSpeed: String,
    iso: Int,
    whiteBalanceTemp: Int,
    tint: Int,
    focalDistance: Float,
    onShutterSelected: (String) -> Unit,
    onIsoSelected: (String) -> Unit,
    onWbSelected: (String) -> Unit,
    onTintSelected: (String) -> Unit,
    onFocusSelected: (String) -> Unit
) {
    // Return options corresponding to active selected parameter
    when (selectedDialParam) {
        "shutter" -> {
            val options = listOf("1/24", "1/30", "1/48", "1/50", "1/60", "1/96", "1/120", "1/250", "1/500", "1/1000", "1/2000", "1/4000", "1/8000")
            ParameterDialSlider(
                title = "Shutter Angle / Speed",
                options = options,
                selectedValue = shutterSpeed,
                onValueSelected = onShutterSelected
            )
        }
        "iso" -> {
            val options = listOf("50", "100", "200", "400", "800", "1600", "3200", "6400", "12800")
            ParameterDialSlider(
                title = "Sensor Gain (ISO)",
                options = options,
                selectedValue = iso.toString(),
                onValueSelected = onIsoSelected
            )
        }
        "wb" -> {
            val options = listOf("2000K", "2800K", "3200K", "4300K", "5000K", "5600K", "6500K", "7500K", "9000K", "11000K")
            ParameterDialSlider(
                title = "Color Temperature (WB)",
                options = options,
                selectedValue = "${whiteBalanceTemp}K",
                onValueSelected = onWbSelected
            )
        }
        "tint" -> {
            val options = listOf("-100", "-75", "-50", "-25", "0", "25", "50", "75", "100")
            ParameterDialSlider(
                title = "Color Tint Balance",
                options = options,
                selectedValue = tint.toString(),
                onValueSelected = onTintSelected
            )
        }
        "focus" -> {
            val options = listOf("CAF (Auto)", "0.1m", "0.2m", "0.3m", "0.5m", "1.0m", "2.0m", "5.0m", "8.0m", "999.0m")
            ParameterDialSlider(
                title = "Focal Plane (MF)",
                options = options,
                selectedValue = if (focalDistance == 0.0f) "CAF (Auto)" else "${focalDistance}m",
                onValueSelected = onFocusSelected
            )
        }
    }
}

@Composable
fun ParameterSelectorCarousel(
    selectedDialParam: String,
    onParamSelected: (String) -> Unit,
    shutterSpeed: String,
    iso: Int,
    wb: Int,
    lockSettingsExpanded: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.04f))
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(vertical = 6.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ParameterButton(
            label = "SHUTTER",
            valueLabel = shutterSpeed,
            active = selectedDialParam == "shutter",
            onClick = { onParamSelected("shutter") },
            locked = lockSettingsExpanded
        )
        ParameterButton(
            label = "ISO",
            valueLabel = iso.toString(),
            active = selectedDialParam == "iso",
            onClick = { onParamSelected("iso") },
            locked = lockSettingsExpanded
        )
        ParameterButton(
            label = "WB",
            valueLabel = "${wb}K",
            active = selectedDialParam == "wb",
            onClick = { onParamSelected("wb") },
            locked = lockSettingsExpanded
        )
        ParameterButton(
            label = "TINT",
            valueLabel = "TIN",
            active = selectedDialParam == "tint",
            onClick = { onParamSelected("tint") },
            locked = lockSettingsExpanded
        )
        ParameterButton(
            label = "FOCUS",
            valueLabel = "FOC",
            active = selectedDialParam == "focus",
            onClick = { onParamSelected("focus") },
            locked = lockSettingsExpanded
        )
    }
}

@Composable
fun ParameterButton(
    label: String,
    valueLabel: String,
    active: Boolean,
    onClick: () -> Unit,
    locked: Boolean
) {
    // Elegant translucent glass capsule
    Column(
        modifier = Modifier
            .width(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                border = BorderStroke(
                    width = 0.8.dp,
                    color = if (active) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.4f),
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = valueLabel,
            fontSize = 9.sp,
            color = if (active) Color.White else Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun CameraBottomTriggerConsole(
    isRecording: Boolean,
    isCapturingPhoto: Boolean,
    selectedMode: CameraMode,
    selectedLens: CameraLens,
    onFlipCamera: () -> Unit,
    onToggleDualCam: () -> Unit,
    onRecClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    val contextIsBusy = isRecording || isCapturingPhoto

    // Smooth tactile rotation animation for the lens switcher icon
    var rotationAngleTarget by remember { mutableStateOf(0f) }
    LaunchedEffect(selectedLens) {
        rotationAngleTarget += 180f
    }
    val animatedRotation by animateFloatAsState(
        targetValue = rotationAngleTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "lens_rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.20f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.2.dp.toPx()
                )
            }
            .navigationBarsPadding()
            .padding(vertical = 14.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Library page navigation launcher (Glass capsule style)
        IconButton(
            onClick = onNavigateToLibrary,
            enabled = !contextIsBusy,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (contextIsBusy) Color.Transparent else Color.White.copy(alpha = 0.06f))
                .border(
                    BorderStroke(
                        width = 0.8.dp,
                        color = if (contextIsBusy) Color.Transparent else Color.White.copy(alpha = 0.15f)
                    ),
                    shape = CircleShape
                )
                .testTag("vault_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Navigate to media library",
                tint = if (contextIsBusy) Color.White.copy(alpha = 0.2f) else Color.White
            )
        }

        // SINGLE PREMIUM DYNAMIC SHUTTER (Adapts to Active Mode: Photo vs Video)
        val isVideoMode = selectedMode.isVideo
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .clickable(enabled = if (isVideoMode) !isCapturingPhoto else !isRecording) {
                    if (isVideoMode) onRecClick() else onPhotoClick()
                }
                .border(
                    2.dp,
                    if (isVideoMode) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.45f),
                    CircleShape
                )
                .padding(6.dp)
                .testTag(if (isVideoMode) "record_button" else "photo_button"),
            contentAlignment = Alignment.Center
        ) {
            if (isVideoMode) {
                // RED video recording trigger styling
                val sizePercent = if (isRecording) 0.5f else 0.85f
                val animSize by animateFloatAsState(
                    targetValue = sizePercent,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "trigger_size"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize(animSize)
                        .clip(if (isRecording) RoundedCornerShape(12.dp) else CircleShape) // squares out when recording
                        .background(if (!isCapturingPhoto) Color(0xFFF87171) else Color(0xFFF87171).copy(alpha = 0.2f)) // Soft Red / Coral Red
                        .border(
                            2.dp,
                            Color.Black.copy(alpha = 0.3f),
                            if (isRecording) RoundedCornerShape(12.dp) else CircleShape
                        )
                )
            } else {
                // WHITE photo shutter trigger styling
                val sizePercent = if (isCapturingPhoto) 0.65f else 0.85f
                val animSize by animateFloatAsState(
                    targetValue = sizePercent,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "trigger_size"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize(animSize)
                        .clip(CircleShape)
                        .background(if (!isRecording) Color.White else Color.White.copy(alpha = 0.2f))
                        .border(1.5.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                ) {
                    val centerIcon = when (selectedMode) {
                        CameraMode.PORTRAIT -> Icons.Default.Portrait
                        CameraMode.NIGHT -> Icons.Default.Brightness3
                        CameraMode.PANORAMA -> Icons.Default.PanoramaHorizontalSelect
                        else -> Icons.Default.CameraAlt
                    }
                    Icon(
                        imageVector = centerIcon,
                        contentDescription = "Capture trigger icon",
                        tint = if (isCapturingPhoto) Color.LightGray else Color(0xFF0F172A),
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }

        // DOUBLE INTERACTIVE TRIGGER CAPSULE FOR CAMERAS (Flip & Dual Mode)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Dual Cam Stream Activator
            val isDualActive = selectedLens == CameraLens.DUAL
            IconButton(
                onClick = onToggleDualCam,
                enabled = !contextIsBusy,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDualActive) Color(0xFFFACC15).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f)
                    )
                    .border(
                        BorderStroke(
                            width = 1.dp,
                            color = if (isDualActive) Color(0xFFFACC15) else Color.White.copy(alpha = 0.15f)
                        ),
                        shape = CircleShape
                    )
                    .testTag("dual_cam_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Toggle Front+Back Dual Cam capture",
                        tint = if (isDualActive) Color(0xFFFACC15) else Color.White.copy(alpha = 0.65f)
                    )
                    if (isDualActive) {
                        // Tiny glowing red active badge
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }

            // 2. Selfie / Main Camera Flip Switch
            val isSelfieActive = selectedLens == CameraLens.FRONT
            IconButton(
                onClick = onFlipCamera,
                enabled = !contextIsBusy,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelfieActive) Color(0xFF10B981).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f)
                    )
                    .border(
                        BorderStroke(
                            width = 1.dp,
                            color = if (isSelfieActive) Color(0xFF10B981) else Color.White.copy(alpha = 0.15f)
                        ),
                        shape = CircleShape
                    )
                    .testTag("flip_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Cached,
                    contentDescription = "Toggle selfie camera",
                    tint = if (isSelfieActive) Color(0xFF10B981) else Color.White,
                    modifier = Modifier.scale(1.15f).rotate(animatedRotation)
                )
            }
        }
    }
}

@Composable
fun CameraModeSelector(
    selectedMode: CameraMode,
    onModeSelected: (CameraMode) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val modes = listOf(
        CameraMode.TIME_LAPSE,
        CameraMode.SLO_MO,
        CameraMode.CINEMATIC,
        CameraMode.VIDEO,
        CameraMode.PHOTO,
        CameraMode.PORTRAIT,
        CameraMode.NIGHT,
        CameraMode.PANORAMA
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(160.dp))
            
            modes.forEach { mode ->
                val isSelected = mode == selectedMode
                Box(
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .clickable(enabled = enabled) { onModeSelected(mode) }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = mode.label,
                            color = if (isSelected) Color(0xFFFACC15) else Color.White.copy(alpha = 0.45f), // Yellow accent for dynamic iPhone style
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFACC15))
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(160.dp))
        }
    }
}

// FORMAT POPUP DIALOG
@Composable
fun CameraFormatDialog(
    currentCodec: VideoCodec,
    currentRes: VideoResolution,
    currentFps: FrameRate,
    onCodecChange: (VideoCodec) -> Unit,
    onResChange: (VideoResolution) -> Unit,
    onFpsChange: (FrameRate) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151515),
        title = {
            Text(
                "CAMERA ENCODING PROFILE",
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Resolution Pick
                Column {
                    Text("RESOLUTION SPEC SENSOR", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    VideoResolution.values().forEach { res ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onResChange(res) }
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentRes == res,
                                onClick = { onResChange(res) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00FF66))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(res.label, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Divider(color = Color(0xFF2E2E2E))

                // Codec Pick
                Column {
                    Text("RECORDING CODEC DEPTH", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    VideoCodec.values().forEach { codec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCodecChange(codec) }
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentCodec == codec,
                                onClick = { onCodecChange(codec) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00FF66))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(codec.label, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                Text(codec.description, color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
                            }
                        }
                    }
                }

                Divider(color = Color(0xFF2E2E2E))

                // FPS Pick
                Column {
                    Text("FRAME VELOCITY RATE", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    FrameRate.values().forEach { fps ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFpsChange(fps) }
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentFps == fps,
                                onClick = { onFpsChange(fps) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00FF66))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(fps.label, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("APPLY SPEC", color = Color(0xFF00FF66), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

// HIGH MONITOR SPEC SHEET DIALOG
@Composable
fun CameraMonitoringAccDialog(
    isPeaking: Boolean,
    onTogglePeaking: () -> Unit,
    isFalseColor: Boolean,
    onToggleFalseColor: () -> Unit,
    isZebra: Boolean,
    onToggleZebra: () -> Unit,
    selectedLUT: CyberLUT,
    onLUTSelected: (CyberLUT) -> Unit,
    framingGrid: FramingGridStyle,
    onGridSelected: (FramingGridStyle) -> Unit,
    audioSource: AudioSource,
    audioGainDb: Float,
    onAudioSourceSelected: (AudioSource) -> Unit,
    onAudioGainChanged: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151515),
        title = {
            Text(
                "CAMERA MONITORING CONTROL",
                color = Color.White,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Exposure utilities toggles row
                Text("DIAGNOSTIC VISUAL AIDS", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTogglePeaking,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isPeaking) Color(0xFF00FF66) else Color(0xFF242424)),
                        modifier = Modifier.weight(1f).height(34.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("PEAKING", fontSize = 10.sp, color = if (isPeaking) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = onToggleFalseColor,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isFalseColor) Color(0xFF00FF66) else Color(0xFF242424)),
                        modifier = Modifier.weight(1f).height(34.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("FALSE COL", fontSize = 10.sp, color = if (isFalseColor) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = onToggleZebra,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isZebra) Color(0xFF00FF66) else Color(0xFF242424)),
                        modifier = Modifier.weight(1f).height(34.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("ZEBRA", fontSize = 10.sp, color = if (isZebra) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Divider(color = Color(0xFF2E2E2E))

                // Framing overlay rules grid select
                Column {
                    Text("COMPOSITION OVERLAY GRIDS", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    FramingGridStyle.values().forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGridSelected(style) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = framingGrid == style,
                                onClick = { onGridSelected(style) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00FF66))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(style.label, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Divider(color = Color(0xFF2E2E2E))

                // Cinematic camera LOOKS (LUT profiles)
                Column {
                    Text("CINEMATIC COLORS (LUT)", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    CyberLUT.values().forEach { lut ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLUTSelected(lut) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLUT == lut,
                                onClick = { onLUTSelected(lut) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00FF66))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(lut.label, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                Text(lut.description, color = Color.White.copy(alpha = 0.4f), fontSize = 8.sp)
                            }
                        }
                    }
                }

                Divider(color = Color(0xFF2E2E2E))

                // Audio configuration setups
                Column {
                    Text("AUDIO SOURCE HARDWARE", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    AudioSource.values().forEach { source ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAudioSourceSelected(source) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = audioSource == source,
                                onClick = { onAudioSourceSelected(source) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00FF66))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(source.label, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                Text(source.samplingRate, color = Color.White.copy(alpha = 0.4f), fontSize = 8.sp)
                            }
                        }
                    }
                }

                // Audio Gain Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AUDIO GAIN SENSITIVITY", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace)
                        Text(String.format("%.1f dB", audioGainDb), fontSize = 10.sp, color = Color(0xFF00FF66), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = audioGainDb,
                        onValueChange = onAudioGainChanged,
                        valueRange = -12f..12f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF00FF66),
                            thumbColor = Color(0xFF00FF66)
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE PANEL", color = Color(0xFF00FF66), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
fun ParameterBlockHeader(
    label: String,
    value: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val highlightColor = Color(0xFFA3E635) // Neon Chartreuse
    Column(
        modifier = Modifier
            .width(58.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
            .border(
                BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) highlightColor else Color.White.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = value,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isSelected) highlightColor else Color.White,
            fontFamily = FontFamily.Monospace,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun BoxScope.CloudCrewChatOverlay(
    isVisible: Boolean,
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glass")
    
    // Liquid glass shifting spots parameters
    val waveOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset1"
    )
    val waveOffset2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset2"
    )

    // Pulse animation for the green CONNECTED indicator
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = Modifier
            .fillMaxHeight()
            .width(290.dp)
            .align(Alignment.CenterEnd)
    ) {
        var textState by remember { mutableStateOf("") }
        val listState = rememberLazyListState()

        // Auto Scroll to bottom on size changes
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // 1. Dynamic Liquid Flow Background Spots
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x3B22D3EE), Color.Transparent),
                        ),
                        radius = 280f,
                        center = Offset(
                            x = size.width / 2 + (Math.sin(Math.toRadians(waveOffset1.toDouble())) * 100f).toFloat(),
                            y = size.height / 3 + (Math.cos(Math.toRadians(waveOffset1.toDouble())) * 100f).toFloat()
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x2BFB7185), Color.Transparent),
                        ),
                        radius = 320f,
                        center = Offset(
                            x = size.width / 2 + (Math.cos(Math.toRadians(waveOffset2.toDouble())) * 120f).toFloat(),
                            y = size.height * 2 / 3 + (Math.sin(Math.toRadians(waveOffset2.toDouble())) * 120f).toFloat()
                        )
                    )
                }
                .background(
                    // 2. Liquid Frosted Glass Composite Mask (translucent dark slate blend)
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF00A0E17),
                            Color(0xEA080C14)
                        )
                    )
                )
                // 3. Ultra-refined white frost border
                .border(
                    BorderStroke(
                        1.dp, 
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                )
                .clickable { /* Prevents click pass-through to camera preview underneath */ }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // Header Panel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00FF66).copy(alpha = pulseAlpha))
                            )
                            Text(
                                text = "CLOUD WORKSPACE",
                                color = Color(0xFF22D3EE),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }
                        Text(
                            text = "Scene 4 | 5 Active Crew",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close chat panel",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                // Message Stream block
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (msg.isMe) Arrangement.End else Arrangement.Start
                        ) {
                            if (!msg.isMe) {
                                // Left Avatar Custom Initials
                                AvatarCircle(
                                    initials = msg.sender.take(1).uppercase(),
                                    backgroundColor = Color(msg.avatarColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            // Structured glass bubble message
                            Column(
                                horizontalAlignment = if (msg.isMe) Alignment.End else Alignment.Start,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msg.sender.uppercase(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (msg.isMe) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "| ${msg.role} |",
                                        fontSize = 7.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White.copy(alpha = 0.3f)
                                    )
                                    Text(
                                        text = msg.timestamp,
                                        fontSize = 7.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White.copy(alpha = 0.3f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Liquid Glass Bubble background
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (msg.isMe) Color(0x3B22D3EE) else Color(0x11FFFFFF),
                                            shape = RoundedCornerShape(
                                                topStart = if (msg.isMe) 10.dp else 2.dp,
                                                topEnd = if (msg.isMe) 2.dp else 10.dp,
                                                bottomEnd = 10.dp,
                                                bottomStart = 10.dp
                                            )
                                        )
                                        .border(
                                            BorderStroke(
                                                0.5.dp, 
                                                if (msg.isMe) Color(0x8022D3EE) else Color.White.copy(alpha = 0.08f)
                                            ),
                                            shape = RoundedCornerShape(
                                                topStart = if (msg.isMe) 10.dp else 2.dp,
                                                topEnd = if (msg.isMe) 2.dp else 10.dp,
                                                bottomEnd = 10.dp,
                                                bottomStart = 10.dp
                                            )
                                        )
                                        .padding(horizontal = 9.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = msg.text,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            if (msg.isMe) {
                                Spacer(modifier = Modifier.width(8.dp))
                                // Right User Avatar Initials
                                AvatarCircle(
                                    initials = "ME",
                                    backgroundColor = Color(msg.avatarColor)
                                )
                            }
                        }
                    }
                }

                // Quick presets reply ribbon helper
                Column(
                    modifier = Modifier.padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "QUICK PRODUCTION PRESETS",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.35f),
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        val replies = listOf(
                            "🎬 Speed! Rolling Scene 4!",
                            "💡 Request light check",
                            "🎤 Audio rolling, quiet",
                            "🎥 Zebra metrics check",
                            "👍 Scene wrap!"
                        )
                        replies.forEach { quickMsg ->
                            Box(
                                modifier = Modifier
                                    .background(Color(0x1BFFFFFF), RoundedCornerShape(12.dp))
                                    .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(12.dp))
                                    .clickable { onSendMessage(quickMsg) }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = quickMsg,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                    }
                }

                // Interactive typing box block
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextField(
                        value = textState,
                        onValueChange = { textState = it },
                        placeholder = {
                            Text(
                                "Type message to crew...",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0x15FFFFFF),
                            unfocusedContainerColor = Color(0x05FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color(0xFF22D3EE),
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("chat_input_field"),
                        singleLine = true
                    )

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x3B22D3EE))
                            .border(BorderStroke(1.dp, Color(0xFF22D3EE).copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                            .clickable {
                                if (textState.isNotBlank()) {
                                    onSendMessage(textState)
                                    textState = ""
                                }
                            }
                            .testTag("chat_send_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send message to crew",
                            tint = Color(0xFF22D3EE),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AvatarCircle(
    initials: String,
    backgroundColor: Color
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(backgroundColor.copy(alpha = 0.25f))
            .border(BorderStroke(1.dp, backgroundColor), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = backgroundColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
