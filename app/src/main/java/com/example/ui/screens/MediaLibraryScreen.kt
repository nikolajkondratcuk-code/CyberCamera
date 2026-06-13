package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.RecordedClip
import com.example.ui.components.CyberSimulatorView
import com.example.ui.components.FramingGridOverlay
import com.example.core.CyberLUT
import com.example.core.FramingGridStyle
import com.example.core.CameraMode
import com.example.viewmodel.CameraViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

import com.example.ui.theme.GlassBackgroundBrush

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaLibraryScreen(
    viewModel: CameraViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clips by viewModel.clipHistory.collectAsStateWithLifecycle()

    var activePlaybackClip by remember { mutableStateOf<RecordedClip?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<RecordedClip?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GlassBackgroundBrush)
            .statusBarsPadding()
    ) {
        // 1. TOP HEADER NAVIGATION BAR
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
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("lib_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate back to shooter HUD",
                        tint = Color.White
                    )
                }

                Column {
                    Text(
                        text = "CYBER VAULT",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${clips.size} clips saved",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Database clearance wipe trigger
            if (clips.isNotEmpty()) {
                IconButton(
                    onClick = { showClearConfirmDialog = true },
                    modifier = Modifier.testTag("clear_all_vault")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Wipe media storage history",
                        tint = Color.Red.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // 2. MAIN SCROLL CONTAINER / LIST OR EMPTY VIEW
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (clips.isEmpty()) {
                // High-End Empty visual guidance placeholder (Adhering to frontend-designGuidelines)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .testTag("empty_history_view"),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.OndemandVideo,
                        contentDescription = "Empty cinematic camera roll placeholder icon",
                        tint = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "THE VAULT IS EMPTY",
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No recorded cinematic clips were found. Return to the CyberCamera dashboard and hit record to create high-end mobile films.",
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 9.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f), contentColor = Color(0xFF22D3EE)),
                        border = BorderStroke(1.dp, Color(0xFF22D3EE).copy(alpha = 0.25f))
                    ) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = "cam button", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LAUNCH DASHBOARD", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            } else {
                // Scrolling library of clips
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("library_scroll_column"),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(clips, key = { it.id }) { clip ->
                        ClipAssetCardRow(
                            clip = clip,
                            onPlay = { activePlaybackClip = clip },
                            onDelete = { showDeleteConfirmDialog = clip }
                        )
                    }
                }
            }
        }
    }

    // Interactive Media Playback Overlay monitor
    activePlaybackClip?.let { clip ->
        InteractivePlaybackOverlay(
            clip = clip,
            onDismiss = { activePlaybackClip = null }
        )
    }

    // Delete confirmation dialog
    showDeleteConfirmDialog?.let { clip ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            containerColor = Color(0xEE1E293B),
            title = {
                Text(
                    "DELETE CINEMATIC CLIP?",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to permanently erase the master video file '${clip.filename}' from physical storage?",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteClip(clip)
                        showDeleteConfirmDialog = null
                    },
                    modifier = Modifier.testTag("delete_confirm_ok")
                ) {
                    Text("DELETE FILE", color = Color(0xFFFB7185), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("CANCEL", color = Color.White, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }

    // Wipe all history confirmation dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = Color(0xEE1E293B),
            title = {
                Text(
                    "PURGE ALL VAULT RECORDINGS?",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Warning: This will permanently wipe all ${clips.size} clips and related production metadata reports from the secure database. This action is irreversible.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllClips()
                        showClearConfirmDialog = false
                    },
                    modifier = Modifier.testTag("purge_vault_ok")
                ) {
                    Text("PURGE ALL", color = Color(0xFFFB7185), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("CANCEL", color = Color.White, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

@Composable
fun ClipAssetCardRow(
    clip: RecordedClip,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val dateString = remember(clip.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
        sdf.format(Date(clip.timestamp))
    }

    val mbSize = remember(clip.sizeBytes) {
        String.format("%.1f MB", clip.sizeBytes / (1024f * 1024f))
    }

    val formattedDuration = remember(clip.durationSeconds) {
        val m = clip.durationSeconds / 60
        val s = clip.durationSeconds % 60
        String.format("%02d:%02d", m, s)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable { onPlay() }
            .padding(10.dp)
            .testTag("clip_item_${clip.id}"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail placeholder drawing resembling camera viewfinder
        Box(
            modifier = Modifier
                .size(76.dp, 56.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.25f))
                .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (clip.isPhoto) {
                Icon(
                    imageVector = Icons.Default.CameraRoll,
                    contentDescription = "photo icon thumbnail",
                    tint = Color(0xFF22D3EE).copy(alpha = 0.9f),
                    modifier = Modifier.size(24.dp)
                )

                // Photo float badge label
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "FUSION",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 7.sp,
                        color = Color(0xFFA5F3FC),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.PlayCircleFilled,
                    contentDescription = "play icon thumbnail guide",
                    tint = Color(0xFF22D3EE).copy(alpha = 0.9f),
                    modifier = Modifier.size(24.dp)
                )

                // Duration floating stamp badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formattedDuration,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 7.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Clip Info details metadata
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = clip.filename,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$dateString · $mbSize",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Metadata row capsule parameters
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (clip.isPhoto) {
                    MetaBadge(label = "48MP Pro")
                    MetaBadge(label = "9-Stacked")
                    MetaBadge(label = "ISO ${clip.isoUsed}")
                    MetaBadge(label = "A17 Fused")
                } else {
                    MetaBadge(label = clip.resolution)
                    MetaBadge(label = "${clip.frameRate} FPS")
                    MetaBadge(label = "ISO ${clip.isoUsed}")
                    MetaBadge(label = clip.codec.substringAfter("Apple ").take(7))
                }
            }
        }

        // Delete File Trigger Button
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .border(0.5.dp, Color(0xFFFB7185).copy(alpha = 0.25f), CircleShape)
                .testTag("delete_clip_btn_${clip.id}")
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Erase master clip",
                tint = Color(0xFFFB7185).copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun MetaBadge(label: String) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.5.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = Color(0xFF22D3EE).copy(alpha = 0.9f),
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

// FULL INTERACTIVE SCREEN PLAYBACK OVERLAY
@Composable
fun InteractivePlaybackOverlay(
    clip: RecordedClip,
    onDismiss: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(!clip.isPhoto) }
    var mockPlayDurationSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(isPlaying) {
        if (!isPlaying || clip.isPhoto) return@LaunchedEffect
        while (mockPlayDurationSeconds < clip.durationSeconds) {
            delay(1000)
            mockPlayDurationSeconds++
        }
        isPlaying = false // file playback done!
    }

    // Determine simulated parameter settings corresponding to the selection
    val speedDenominator = clip.shutterUsed.substringAfter("/", "1").toFloatOrNull() ?: 1f
    val shutterExposureFactor = (150f / speedDenominator).coerceIn(0.2f, 2.5f)
    val isoBrightnessFactor = (clip.isoUsed / 400f).coerceIn(0.5f, 3.0f)
    val combinedExposure = (shutterExposureFactor * isoBrightnessFactor).coerceIn(0.1f, 3.5f)

    // Match LUT
    val matchedLut = when {
        clip.lutUsed.contains("Neon") -> CyberLUT.CYBERPUNK
        clip.lutUsed.contains("Teal") -> CyberLUT.TEAL_ORANGE
        clip.lutUsed.contains("Mono") -> CyberLUT.MONOCHROME
        else -> CyberLUT.NEUTRAL
    }

    val progressValue = if (clip.isPhoto) 1.0f else (mockPlayDurationSeconds.toFloat() / clip.durationSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)
    val progressLabel = if (clip.isPhoto) "HDR PHOTO FUSED" else String.format("%02d:%02d / %02d:%02d",
        mockPlayDurationSeconds / 60, mockPlayDurationSeconds % 60,
        clip.durationSeconds / 60, clip.durationSeconds % 60
    )

    val playMode = remember(clip.cameraMode) {
        try {
            CameraMode.valueOf(clip.cameraMode)
        } catch (_: Exception) {
            CameraMode.PHOTO
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("playback_overlay")
    ) {
        // Render visual cyber simulated replay
        CyberSimulatorView(
            shutterSpeed = clip.shutterUsed,
            iso = clip.isoUsed,
            wbTemp = clip.whiteBalanceUsed,
            tint = clip.tintUsed,
            focalDistance = clip.focalDistUsed,
            isFocusPeaking = false,
            isFalseColor = false,
            isZebra = false,
            selectedLUT = matchedLut,
            selectedMode = playMode
        )

        // Rule of guidelines overlays during play for technical preview screen
        FramingGridOverlay(style = FramingGridStyle.CINEMATIC_2_39)

        // Visual HUD Overlays
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top overlay layout row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (clip.isPhoto) Color(0xFF22D3EE) else (if (isPlaying) Color.Green else Color.DarkGray))
                    )
                    Text(
                        text = if (clip.isPhoto) "APPLE DEEP FUSION IMAGE ACTIVE" else if (isPlaying) "METADATA REPLAY ACTIVE" else "REPLAY COMPLETED",
                        color = if (clip.isPhoto) Color(0xFF22D3EE) else if (isPlaying) Color(0xFF00FF66) else Color.White.copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = clip.filename,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Cinematic production metadata reports box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(6.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (clip.isPhoto) "MASTER PHOTO REPORT Logs" else "MASTER CAMERA REPORT Logs".uppercase(),
                    color = Color(0xFF00FF66),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        MetaReportItem(label = "Codec Profile", value = clip.codec)
                        MetaReportItem(label = "Resolution", value = clip.resolution)
                        MetaReportItem(label = "Frame Rate", value = if (clip.isPhoto) "Static Photo" else "${clip.frameRate} fps")
                    }
                    Column {
                        MetaReportItem(label = "ISO Settings", value = clip.isoUsed.toString())
                        MetaReportItem(label = "Exposure speed", value = clip.shutterUsed)
                        MetaReportItem(label = "Color Temp", value = "${clip.whiteBalanceUsed}K")
                    }
                    Column {
                        MetaReportItem(label = "Focal Distance", value = if (clip.focalDistUsed == 0.0f) "Continuous AF" else "${clip.focalDistUsed}m")
                        MetaReportItem(label = "LUT Profile", value = clip.lutUsed.substringBefore(" style"))
                        MetaReportItem(label = "Format extension", value = clip.filename.substringAfterLast("."))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Replay scrub slider bar Or static indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = progressValue,
                        color = if (clip.isPhoto) Color(0xFF22D3EE) else Color(0xFF00FF66),
                        trackColor = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                    )

                    Text(
                        text = progressLabel,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Monitor control layer row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!clip.isPhoto) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { isPlaying = !isPlaying },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "playback play pause button",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = { mockPlayDurationSeconds = 0 },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = "restart playback video",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "COMPOSITED DEEP HDR GRAPH (DNG ProRAW Matrix)",
                            color = Color(0xFF22D3EE),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("CLOSE MONITOR", fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun MetaReportItem(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            fontSize = 7.sp,
            color = Color.White.copy(alpha = 0.4f),
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            fontSize = 7.sp,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
