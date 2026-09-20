package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val CineflixM3DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE50914),          // Netflix red
    onPrimary = Color.White,
    primaryContainer = Color(0xFF7B2FF7), // Purple accent
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFFF3CAC),        // Pink accent
    onSecondary = Color.White,
    background = Color(0xFF0B0B0F),       // Deep charcoal black
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF111118),          // Charcoal secondary
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1B1B26),
    onSurfaceVariant = Color(0xFFA0A0B2),
    outline = Color(0x33FFFFFF),
    outlineVariant = Color(0x1AFFFFFF),
    error = Color(0xFFEF4444),
    onError = Color.White
)

@Composable
fun CineflixTheme(
    content: @Composable () -> Unit
) {
    val colors = CineflixColorPalette()
    val typography = CineflixTypography()

    CompositionLocalProvider(
        LocalCineflixColors provides colors,
        LocalCineflixTypography provides typography
    ) {
        MaterialTheme(
            colorScheme = CineflixM3DarkColorScheme,
            content = content
        )
    }
}

/**
 * Direct accessor object for Cineflix design tokens:
 * e.g., CineflixTheme.colors.accentRed, CineflixTheme.typography.h2
 */
object CineflixTheme {
    val colors: CineflixColorPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalCineflixColors.current

    val typography: CineflixTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalCineflixTypography.current
}
