package com.example.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CineflixTheme

/**
 * CINEFLIX Genre Filter Chip
 * Pill-shaped, clickable interactive filter chip.
 * Selected state uses vibrant JioCinema/Hotstar gradient or red accent.
 */
@Composable
fun CineflixGenreChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val bgModifier = if (isSelected) {
        Modifier.background(CineflixTheme.colors.primaryGradientBrush)
    } else {
        Modifier.background(CineflixTheme.colors.cardBackground)
    }

    val borderColor = if (isSelected) {
        CineflixTheme.colors.accentPink
    } else {
        CineflixTheme.colors.cardBorder
    }

    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else CineflixTheme.colors.textSecondary,
        label = "chip_text_color"
    )

    Box(
        modifier = modifier
            .testTag("genre_chip_$label")
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .then(bgModifier)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = if (isSelected) CineflixTheme.typography.buttonSmall else CineflixTheme.typography.bodySmall,
            color = textColor
        )
    }
}
