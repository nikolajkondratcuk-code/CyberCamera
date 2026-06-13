package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun TechnicalAnalyzers(
    isRecording: Boolean,
    audioGainDb: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Left Panel: Real-Time RGB Waveform Histogram
        Box(
            modifier = Modifier
                .weight(1.2f)
                .height(60.dp)
                .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
        ) {
            LiveRGBHistogram(isRecording = isRecording)
            Text(
                text = "HISTOGRAM (LUMA)",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
            )
        }

        // Right Panel: Stereo DB Audio meters (Left + Right channels)
        Column(
            modifier = Modifier
                .weight(1f)
                .height(60.dp),
            verticalArrangement = Arrangement.SpaceAround
        ) {
            StereoAudioMeters(isRecording = isRecording, audioGainDb = audioGainDb)
        }
    }
}

@Composable
fun LiveRGBHistogram(isRecording: Boolean) {
    val transition = rememberInfiniteTransition(label = "histogram_osc")
    val ticker by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "histogram_tick"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Simulating three color curves (Red, Green, Blue) offset
        // To draw an organic waveform histogram, we compile bezier curvatures.
        val rPath = Path()
        val gPath = Path()
        val bPath = Path()

        rPath.moveTo(0f, height)
        gPath.moveTo(0f, height)
        bPath.moveTo(0f, height)

        val resolution = 16
        val step = width / (resolution - 1)

        for (i in 0 until resolution) {
            val ratio = i.toFloat() / (resolution - 1)
            val x = ratio * width

            // Compose wavy heights which resemble typical video live histograms
            val rHeight = (height - 8f - (kotlin.math.sin((ratio * 5.0 + ticker).toDouble()).toFloat() * 15f + kotlin.math.sin((ratio * 12.0).toDouble()).toFloat() * 10f + height * 0.4f)).coerceIn(5f, height)
            val gHeight = (height - 8f - (kotlin.math.sin((ratio * 4.5 + ticker * 1.1).toDouble()).toFloat() * 18f + kotlin.math.sin((ratio * 10.0 + 1.2).toDouble()).toFloat() * 8f + height * 0.45f)).coerceIn(5f, height)
            val bHeight = (height - 8f - (kotlin.math.sin((ratio * 6.0 + ticker * 0.8).toDouble()).toFloat() * 12f + kotlin.math.sin((ratio * 15.0 - 0.5).toDouble()).toFloat() * 12f + height * 0.38f)).coerceIn(5f, height)

            rPath.lineTo(x, rHeight)
            gPath.lineTo(x, gHeight)
            bPath.lineTo(x, bHeight)
        }

        rPath.lineTo(width, height)
        gPath.lineTo(width, height)
        bPath.lineTo(width, height)

        rPath.close()
        gPath.close()
        bPath.close()

        // Draw overlapping channels
        drawPath(
            path = rPath,
            color = Color(0x3FFF3333)
        )
        drawPath(
            path = gPath,
            color = Color(0x3F33FF33)
        )
        drawPath(
            path = bPath,
            color = Color(0x3F3333FF)
        )

        // Draw outline path for baseline luma
        val lumaPath = Path().apply {
            moveTo(0f, height - 10)
            for (i in 0 until resolution) {
                val ratio = i.toFloat() / (resolution - 1)
                val x = ratio * width
                val lumaHeight = height - 12f - (kotlin.math.sin((ratio * 4.8 + ticker * 0.95).toDouble()).toFloat() * 14f).coerceIn(4f, height)
                lineTo(x, lumaHeight)
            }
            lineTo(width, height)
        }
        drawPath(
            path = lumaPath,
            color = Color(0xCC22D3EE),
            style = Stroke(width = 1.8f)
        )
    }
}

@Composable
fun StereoAudioMeters(isRecording: Boolean, audioGainDb: Float) {
    var bounceSeed by remember { mutableStateOf(0f) }

    LaunchedEffect(isRecording) {
        while (true) {
            delay(120)
            // Bounces between -60dB to 0dB. If recording, it is hotter!
            val base = if (isRecording) -12f else -45f
            val fluctuation = (Math.random() * 25f).toFloat() - 12f
            // Combine with DB gain settings
            bounceSeed = (base + fluctuation + audioGainDb).coerceIn(-60f, 0f)
        }
    }

    val animDbLeft by animateFloatAsState(
        targetValue = bounceSeed,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "anim_audio_l"
    )
    val animDbRight by animateFloatAsState(
        targetValue = bounceSeed + (Math.random() * 10f - 5f).toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "anim_audio_r"
    )

    AudioMeterRow(label = "CH1 (L)", dbValue = animDbLeft)
    AudioMeterRow(label = "CH2 (R)", dbValue = animDbRight)
}

@Composable
fun AudioMeterRow(label: String, dbValue: Float) {
    // dbValue ranges from -60dB to 0dB.
    // Convert -60dB -> 0.0f, and 0dB -> 1.0f progress multiplier
    val progress = ((dbValue + 60f) / 60f).coerceIn(0.01f, 1.0f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(32.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
        ) {
            // Draw segmental levels grid (Cyan/Teal below -12, Yellow/Orange from -12 to -4, Red/Rose above -4)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Draw filled progress
                val fillWidth = width * progress
                val greenLimit = width * (-12f + 60f) / 60f
                val yellowLimit = width * (-4f + 60f) / 60f

                // Segment drawing:
                // Green (Cyan-based Glass style) part
                if (fillWidth > 0) {
                    val gWidth = fillWidth.coerceAtMost(greenLimit)
                    drawRect(
                        brush = Brush.horizontalGradient(listOf(Color(0xFF22D3EE), Color(0xFF06B6D4))),
                        topLeft = Offset(0f, 0f),
                        size = Size(gWidth, height)
                    )
                }

                // Yellow (Amber glass) part
                if (fillWidth > greenLimit) {
                    val yWidth = (fillWidth - greenLimit).coerceAtMost(yellowLimit - greenLimit)
                    drawRect(
                        brush = Brush.horizontalGradient(listOf(Color(0xFFFBBF24), Color(0xFFD97706))),
                        topLeft = Offset(greenLimit, 0f),
                        size = Size(yWidth, height)
                    )
                }

                // Red (Rose glass) part (Clipping Alert area)
                if (fillWidth > yellowLimit) {
                    val rWidth = fillWidth - yellowLimit
                    drawRect(
                        brush = Brush.horizontalGradient(listOf(Color(0xFFFB7185), Color(0xFFE11D48))),
                        topLeft = Offset(yellowLimit, 0f),
                        size = Size(rWidth, height)
                    )
                }

                // Draw DB scale division markers
                val divCount = 10
                for (j in 1 until divCount) {
                    val divX = (j.toFloat() / divCount) * width
                    drawLine(
                        color = Color.Black,
                        start = Offset(divX, 0f),
                        end = Offset(divX, height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }

        // Display current dB numerical output
        val printedDb = if (dbValue < -58f) "-∞" else String.format("%.0f dB", dbValue)
        Text(
            text = printedDb,
            color = if (dbValue > -10f) Color(0xFFFFB800) else Color.White.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(32.dp)
        )
    }
}
