package com.example.core

import androidx.compose.ui.graphics.Color

enum class VideoCodec(val label: String, val extension: String, val description: String) {
    H264("H.264 / AVC", "mp4", "High compatibility standard compressions"),
    HEVC("H.265 / HEVC", "mp4", "High Efficiency Video Coding smaller size"),
    PRORES("Apple ProRes 422", "mov", "Professional post-production raw grade")
}

enum class VideoResolution(val label: String, val width: Int, val height: Int, val bitrateMultiplier: Float) {
    UHD_4K("4K UHD (3840x2160)", 3840, 2160, 4.0f),
    FHD_1080P("1080p FHD (1920x1080)", 1920, 1080, 1.5f),
    HD_720P("720p HD (1280x720)", 1280, 720, 0.8f)
}

enum class FrameRate(val fps: Int, val label: String) {
    FPS_24(24, "24 FPS (Cinematic)"),
    FPS_30(30, "30 FPS (Standard)"),
    FPS_60(60, "60 FPS (Fluid / High)"),
    FPS_120(120, "120 FPS (Slo-Mo / Dual Scan)")
}

enum class CyberLUT(val label: String, val description: String) {
    NEUTRAL("Log-C Rec.709 (Flat/Neutral)", "Standard dynamic range conversion"),
    TEAL_ORANGE("Teal & Orange cinematic style", "High-contrast Hollywood warm skin and cool shadows"),
    MONOCHROME("Slate Mono 800", "High contrast silver look with crushed dark regions"),
    CYBERPUNK("Neon Cyberpunk 2099", "Exaggerated violet highlights and neon green shadow tints")
}

enum class FramingGridStyle(val label: String) {
    NONE("No Grid"),
    THIRD_RULE("Rule of Thirds (3x3)"),
    CROSSHAIR("Center Crosshair"),
    CINEMATIC_2_39("Cinematic Scope (2.39:1)")
}

enum class CameraMode(val label: String, val isVideo: Boolean, val description: String) {
    TIME_LAPSE("TIME-LAPSE", true, "Time Plus interval capture"),
    SLO_MO("SLO-MO", true, "Slow motion 120/240 FPS"),
    CINEMATIC("CINEMATIC", true, "Cinematic auto-focus and depth bokeh"),
    VIDEO("VIDEO", true, "Standard Dolby HDR video"),
    PHOTO("PHOTO", false, "Standard 24MP/48MP Deep Fusion"),
    PORTRAIT("PORTRAIT", false, "Portrait blur depth-of-field"),
    NIGHT("NIGHT", false, "Long Exposure multi-frame night mode"),
    PANORAMA("PANO", false, "Panoramic horizontal sweep")
}

enum class CameraLens(val label: String, val description: String) {
    BACK("REAR", "Standard Pro Main Back Lens"),
    FRONT("SELFIE", "Front-Facing Ultra-wide Lens"),
    DUAL("DUAL-CAM", "Simultaneous Front + Rear Dual Capture")
}

enum class AudioSource(val label: String, val samplingRate: String) {
    INTERNAL_MIC("Smartphone Mono Mic", "48 kHz"),
    STEREO_MIC("Matrix Dual-Mic (Cyber)", "96 kHz HD"),
    EXTERNAL_JACK("Pro External Source (XLR/Aux)", "48 kHz Studio")
}

/**
 * Mapping helper for False Color exposition guide
 */
object FalseColorPalette {
    // False color maps visual brightness (0 to 255) to analytical diagnostic hues.
    // Violet: near-black underexposure (0-20)
    // Deep Blue: shadows highlights (20-60)
    // Dark Green: target zone / skin tones low (60-100)
    // Light Green: target zone / skin tones high (100-140)
    // Amber/Yellow: highlight headroom (140-200)
    // Orange: near overexposure (200-235)
    // Bright Red: clipped white highlights (235-255)
    fun getColorForBrightness(brightness: Float): Color {
        val bValue = (brightness * 255f).coerceIn(0f, 255f).toInt()
        return when (bValue) {
            in 0..20 -> Color(0xFF6B00B6) // Violet (Deep underexposed shadow)
            in 21..60 -> Color(0xFF0014B4) // Royal Blue (Shallow shadow)
            in 61..100 -> Color(0xFF007530) // Dark Green (Lower Midtones)
            in 101..140 -> Color(0xFF4C9800) // Light Green (True Skin tones)
            in 141..190 -> Color(0xFFD49700) // Amber / Dark Yellow (High Midtones)
            in 191..234 -> Color(0xFFE04500) // Intense Orange (Highlight alert)
            else -> Color(0xFFFF0000) // Clipped Red (100% white clipping)
        }
    }
}
