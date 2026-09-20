package com.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CineflixTheme

/**
 * CINEFLIX Design System Badges
 * High-contrast, WCAG AA compliant indicator badges combining
 * Netflix, Prime Video, Hotstar & JioCinema metadata presentation.
 */

@Composable
fun RatingBadge(
    rating: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .testTag("rating_badge")
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xE614141E))
            .border(1.dp, Color(0x33F5C518), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Rating star",
            tint = CineflixTheme.colors.accentGold,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = String.format("%.1f", rating),
            style = CineflixTheme.typography.monoRating,
            color = Color.White
        )
    }
}

@Composable
fun MatchBadge(
    matchPercent: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("match_badge")
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x3322C55E))
            .border(1.dp, CineflixTheme.colors.success.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$matchPercent% Match",
            style = CineflixTheme.typography.monoBadge,
            color = CineflixTheme.colors.success
        )
    }
}

@Composable
fun QualityBadge(
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("quality_badge_$label")
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x4D1F1F2F))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = CineflixTheme.typography.monoBadge,
            color = CineflixTheme.colors.textSecondary
        )
    }
}

@Composable
fun AgeRatingBadge(
    ageRating: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("age_badge_$ageRating")
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x33FFFFFF))
            .border(1.dp, Color(0x4DFFFFFF), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = ageRating,
            style = CineflixTheme.typography.monoBadge,
            color = Color.White
        )
    }
}

@Composable
fun PlatformBadge(
    badgeText: String,
    modifier: Modifier = Modifier
) {
    val bgModifier = when {
        badgeText.contains("ORIGINAL", ignoreCase = true) ->
            Modifier.background(CineflixTheme.colors.accentRed)
        badgeText.contains("PRIME", ignoreCase = true) ->
            Modifier.background(CineflixTheme.colors.accentPrimeBlue)
        badgeText.contains("HOTSTAR", ignoreCase = true) ->
            Modifier.background(CineflixTheme.colors.accentPurple)
        badgeText.contains("JIO", ignoreCase = true) ->
            Modifier.background(CineflixTheme.colors.primaryGradientBrush)
        else ->
            Modifier.background(CineflixTheme.colors.accentRed)
    }

    Box(
        modifier = modifier
            .testTag("platform_badge")
            .clip(RoundedCornerShape(4.dp))
            .then(bgModifier)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = badgeText.uppercase(),
            style = CineflixTheme.typography.monoBadge.copy(letterSpacing = 1.sp),
            color = Color.White
        )
    }
}

@Composable
fun Top10Badge(
    rank: Int? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .testTag("top_10_badge")
            .clip(RoundedCornerShape(4.dp))
            .background(CineflixTheme.colors.accentRed)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (rank != null) "TOP $rank TODAY" else "TOP 10",
            style = CineflixTheme.typography.monoBadge.copy(letterSpacing = 1.sp),
            color = Color.White
        )
    }
}
