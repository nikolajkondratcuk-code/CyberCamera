package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

private val FrostedGlassColorScheme = darkColorScheme(
    primary = Color(0xFF22D3EE), // Cyan 400
    secondary = Color(0xFF38BDF8), // Sky 400
    tertiary = Color(0xFFFB7185), // Crimson/Rose red
    background = Color(0xFF0F172A), // Slate 900 background
    surface = Color(0x1BFFFFFF), // Frosted glass transparent white (11% Alpha)
    onPrimary = Color(0xFF0F172A),
    onSecondary = Color(0xFF0F172A),
    onBackground = Color(0xFFF1F5F9), // Slate 100 text
    onSurface = Color(0xFFF1F5F9)
)

// Main luxurious glass background gradient
val GlassBackgroundBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF1A1A2E),
        Color(0xFF16213E),
        Color(0xFF0F3460)
    )
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
      colorScheme = FrostedGlassColorScheme,
      typography = Typography,
      content = content
  )
}
