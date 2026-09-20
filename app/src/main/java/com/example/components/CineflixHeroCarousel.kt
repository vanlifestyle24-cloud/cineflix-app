package com.example.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.MediaItem
import com.example.ui.theme.CineflixTheme
import kotlinx.coroutines.delay

/**
 * CINEFLIX Hero Carousel
 * - Ken-Burns subtle zoom effect
 * - Auto-playing index transitions
 * - Responsive adaptive height (50vh mobile, 70-80vh tablet/desktop)
 * - Rich metadata: Match %, Rating, Platform badge, 4K UHD pills
 * - Action buttons: Play Now, Watchlist toggle, Details Modal trigger
 */

@Composable
fun CineflixHeroCarousel(
    items: List<MediaItem>,
    bannerHeight: Dp = 440.dp,
    onPlayClick: (MediaItem) -> Unit,
    onDetailsClick: (MediaItem) -> Unit,
    onWatchlistToggle: (MediaItem) -> Unit,
    isBookmarked: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }
    val currentItem = items[currentIndex % items.size]

    // Auto-advance carousel every 7 seconds
    LaunchedEffect(currentIndex) {
        delay(7000)
        currentIndex = (currentIndex + 1) % items.size
    }

    // Ken-Burns gentle ambient zoom animation
    val infiniteTransition = rememberInfiniteTransition(label = "ken_burns")
    val zoomScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ken_burns_scale"
    )

    Box(
        modifier = modifier
            .testTag("hero_carousel")
            .fillMaxWidth()
            .height(bannerHeight)
            .background(CineflixTheme.colors.backgroundPrimary)
    ) {
        // Backdrop image with Ken-Burns scale
        if (currentItem.backdropRes != null) {
            Image(
                painter = painterResource(id = currentItem.backdropRes),
                contentDescription = currentItem.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(zoomScale)
            )
        }

        // Atmospheric Multi-stage Gradient Overlays (Top, Bottom, and Side Vignette)
        // 1. Bottom fade to deep charcoal black
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CineflixTheme.colors.heroGradientOverlay)
        )

        // 2. Top-down subtle dark vignette for sticky navbar readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xCC0B0B0F), Color.Transparent)
                    )
                )
        )

        // Hero Content Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Badges row: Platform + Top 10 + Rating + Match %
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlatformBadge(badgeText = currentItem.platformBadge)
                if (currentItem.isTop10) {
                    Top10Badge(rank = currentItem.top10Rank)
                }
                RatingBadge(rating = currentItem.imdbRating)
                MatchBadge(matchPercent = currentItem.matchScore)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Title
            Text(
                text = currentItem.title,
                style = CineflixTheme.typography.h1,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Tagline / Genres & Specs
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${currentItem.releaseYear} • ${currentItem.duration} • ${currentItem.ageRating}",
                    style = CineflixTheme.typography.bodySmall,
                    color = CineflixTheme.colors.textSecondary
                )
                currentItem.qualityBadges.take(2).forEach { quality ->
                    QualityBadge(label = quality)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Synopsis preview
            Text(
                text = currentItem.synopsis,
                style = CineflixTheme.typography.bodyMedium,
                color = CineflixTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CineflixPrimaryButton(
                    text = "Play Now",
                    icon = Icons.Default.PlayArrow,
                    useGradient = false,
                    onClick = { onPlayClick(currentItem) },
                    testTag = "hero_play_button"
                )

                CineflixSecondaryButton(
                    text = if (isBookmarked) "Added" else "Watchlist",
                    icon = if (isBookmarked) Icons.Default.Check else Icons.Default.Add,
                    onClick = { onWatchlistToggle(currentItem) },
                    testTag = "hero_watchlist_button"
                )

                CineflixIconButton(
                    icon = Icons.Default.Info,
                    contentDescription = "More Details",
                    onClick = { onDetailsClick(currentItem) },
                    testTag = "hero_info_button"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pager indicator dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.indices.forEach { index ->
                    val isSelected = index == currentIndex
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(4.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) CineflixTheme.colors.accentRed
                                else Color(0x66FFFFFF)
                            )
                            .clickable { currentIndex = index }
                    )
                }
            }
        }
    }
}
