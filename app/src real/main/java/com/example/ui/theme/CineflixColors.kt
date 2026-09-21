package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * CINEFLIX Design System Color Tokens
 * Combining the best aesthetic elements of:
 * - Netflix: Deep cinematic black, signature bold red (#E50914), top-match green
 * - Prime Video: Clean dark charcoal (#111118), metallic blues, subtle card borders
 * - Disney+ Hotstar: Rich deep navy-black hues, royal accents, high-contrast badges
 * - JioCinema: Vibrant purple-to-pink gradient (#7B2FF7 → #FF3CAC)
 */
@Immutable
data class CineflixColorPalette(
    // 1. Backgrounds
    val backgroundPrimary: Color = Color(0xFF0B0B0F),     // Deep charcoal black
    val backgroundSecondary: Color = Color(0xFF111118),   // Secondary surface
    val backgroundElevated: Color = Color(0xFF171722),    // Elevated container
    val backgroundNavbar: Color = Color(0xE60B0B0F),      // Sticky frosted glass nav
    val backgroundOverlay: Color = Color(0xCC000000),     // Modal backdrop

    // 2. Card & Glassmorphic Surfaces
    val cardBackground: Color = Color(0x661A1A28),        // Semi-transparent with blur
    val cardBackgroundHover: Color = Color(0x99232338),   // Hover / active elevation
    val cardBorder: Color = Color(0x1AFFFFFF),            // Subtle glass border
    val cardBorderHover: Color = Color(0x4DFFFFFF),       // Focused glass border

    // 3. Brand Accents
    val accentRed: Color = Color(0xFFE50914),             // Netflix-style crimson red
    val accentRedHover: Color = Color(0xFFFF1F2A),        // Bright red hover
    val accentPurple: Color = Color(0xFF7B2FF7),          // JioCinema / Hotstar purple
    val accentPink: Color = Color(0xFFFF3CAC),            // JioCinema vibrant pink
    val accentPrimeBlue: Color = Color(0xFF00A8E1),       // Prime Video electric blue
    val accentGold: Color = Color(0xFFF5C518),            // IMDb star rating gold

    // 4. Typography & Foreground
    val textPrimary: Color = Color(0xFFFFFFFF),           // Pure white
    val textSecondary: Color = Color(0xFFA0A0B2),         // Muted silver gray
    val textTertiary: Color = Color(0xFF6E6E82),          // Dim neutral gray
    val textDisabled: Color = Color(0xFF434354),          // Inactive element text

    // 5. Semantic Feedback
    val success: Color = Color(0xFF22C55E),               // Bright emerald green
    val error: Color = Color(0xFFEF4444),                 // Alert red
    val warning: Color = Color(0xFFF59E0B),               // Warm amber warning
    val info: Color = Color(0xFF3B82F6),                  // Information sky blue

    // 6. Interactive & Gradients
    val shimmerHighlight: Color = Color(0x33FFFFFF),
    val shimmerBase: Color = Color(0x14FFFFFF),
) {
    // Dynamic Accent Gradients
    val primaryGradientBrush: Brush
        get() = Brush.horizontalGradient(
            colors = listOf(accentPurple, accentPink)
        )

    val redGlowBrush: Brush
        get() = Brush.radialGradient(
            colors = listOf(Color(0x66E50914), Color(0x00E50914))
        )

    val purpleGlowBrush: Brush
        get() = Brush.radialGradient(
            colors = listOf(Color(0x667B2FF7), Color(0x007B2FF7))
        )

    val heroGradientOverlay: Brush
        get() = Brush.verticalGradient(
            0.0f to Color.Transparent,
            0.45f to Color(0x400B0B0F),
            0.85f to Color(0xDE0B0B0F),
            1.0f to backgroundPrimary
        )

    val cardGradientOverlay: Brush
        get() = Brush.verticalGradient(
            0.0f to Color.Transparent,
            0.5f to Color(0x20000000),
            1.0f to Color(0xF00B0B0F)
        )
}

val LocalCineflixColors = staticCompositionLocalOf { CineflixColorPalette() }
