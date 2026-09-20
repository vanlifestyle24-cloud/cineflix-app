package com.example.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import coil.compose.AsyncImage
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.MediaItem
import com.example.ui.theme.CineflixTheme

/**
 * CINEFLIX Card Components
 * - Poster Card (2:3 aspect ratio, interactive press/hover scale, shadow glow)
 * - Landscape Card (16:9 for Continue Watching with progress indicator)
 * - Top 10 Styled Rank Card (Netflix signature 3D stylized rank typography)
 * - Shimmer Skeleton Card (Motion loading state)
 */

@Composable
fun MoviePosterCard(
    media: MediaItem,
    onClick: () -> Unit,
    onBookmarkToggle: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    isBookmarked: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        label = "card_scale"
    )

    Box(
        modifier = modifier
            .testTag("movie_card_${media.id}")
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(CineflixTheme.colors.cardBackground)
            .border(
                width = if (isPressed) 1.5.dp else 1.dp,
                color = if (isPressed) CineflixTheme.colors.accentPink else CineflixTheme.colors.cardBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
    ) {
        Column {
            // Poster area (aspect ratio 2:3)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
            ) {
                if (!media.posterUrl.isNullOrBlank() || !media.backdropUrl.isNullOrBlank()) {
                    val imageUrl = media.posterUrl ?: media.backdropUrl
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "${media.title} poster",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (media.backdropRes != null) {
                    Image(
                        painter = painterResource(id = media.backdropRes),
                        contentDescription = "${media.title} poster",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1E1E2F), Color(0xFF0F0F1A))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = media.title.take(2).uppercase(),
                            style = CineflixTheme.typography.h2,
                            color = CineflixTheme.colors.textTertiary
                        )
                    }
                }

                // Vertical dark gradient vignette overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CineflixTheme.colors.cardGradientOverlay)
                )

                // Top badges (Rating & Quality)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RatingBadge(rating = media.imdbRating)
                    if (media.qualityBadges.isNotEmpty()) {
                        QualityBadge(label = media.qualityBadges.first())
                    }
                }

                // Quick Play Action button on poster
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .border(1.dp, Color(0x66FFFFFF), CircleShape)
                        .clickable { onPlayClick(media) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Quick play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Bottom badges on poster
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MatchBadge(matchPercent = media.matchScore)
                }

                // Bookmark toggle on poster
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x80000000))
                        .clickable { onBookmarkToggle(media) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) CineflixTheme.colors.accentRed else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Title & Meta Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CineflixTheme.colors.cardBackground)
                    .padding(8.dp)
            ) {
                Text(
                    text = media.title,
                    style = CineflixTheme.typography.h6,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${media.releaseYear} • ${media.primaryGenre}",
                        style = CineflixTheme.typography.caption,
                        color = CineflixTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = media.duration,
                        style = CineflixTheme.typography.monoBadge,
                        color = CineflixTheme.colors.textTertiary
                    )
                }
            }
        }
    }
}

/**
 * Landscape Card (16:9) for "Continue Watching" with watch progress
 */
@Composable
fun LandscapeMediaCard(
    media: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        label = "landscape_scale"
    )

    Box(
        modifier = modifier
            .testTag("continue_card_${media.id}")
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(CineflixTheme.colors.cardBackground)
            .border(1.dp, CineflixTheme.colors.cardBorder, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                if (media.backdropRes != null) {
                    Image(
                        painter = painterResource(id = media.backdropRes),
                        contentDescription = "${media.title} preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF161622))
                    )
                }

                // Dark gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xB30B0B0F))
                            )
                        )
                )

                // Play icon in center
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .border(1.dp, Color(0x66FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Progress Bar at bottom
                if (media.watchProgressPercent != null) {
                    LinearProgressIndicator(
                        progress = { media.watchProgressPercent },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp),
                        color = CineflixTheme.colors.accentRed,
                        trackColor = Color(0x4DFFFFFF)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = media.title,
                        style = CineflixTheme.typography.h6,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Resume • ${media.primaryGenre}",
                        style = CineflixTheme.typography.caption,
                        color = CineflixTheme.colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                RatingBadge(rating = media.imdbRating)
            }
        }
    }
}

/**
 * Top 10 Styled Rank Card
 * Displays the bold outline rank number "1", "2", "3" adjacent to or overlapping the poster
 */
@Composable
fun Top10RankCard(
    rank: Int,
    media: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .testTag("top10_rank_card_$rank")
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Large stylized rank number
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                style = CineflixTheme.typography.h1.copy(
                    fontSize = 52.sp,
                    lineHeight = 52.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black
                ),
                color = CineflixTheme.colors.textTertiary.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Poster
        Box(
            modifier = Modifier
                .width(110.dp)
                .aspectRatio(0.68f)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, CineflixTheme.colors.cardBorder, RoundedCornerShape(6.dp))
        ) {
            if (media.backdropRes != null) {
                Image(
                    painter = painterResource(id = media.backdropRes),
                    contentDescription = media.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CineflixTheme.colors.cardGradientOverlay)
            )
            Text(
                text = media.title,
                style = CineflixTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Skeleton Loader with Shimmer Animation
 */
@Composable
fun SkeletonMediaCard(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            CineflixTheme.colors.shimmerBase,
            CineflixTheme.colors.shimmerHighlight,
            CineflixTheme.colors.shimmerBase
        ),
        start = Offset(translateAnim - 500f, translateAnim - 500f),
        end = Offset(translateAnim, translateAnim)
    )

    Column(
        modifier = modifier
            .testTag("skeleton_card")
            .clip(RoundedCornerShape(8.dp))
            .background(CineflixTheme.colors.cardBackground)
            .border(1.dp, CineflixTheme.colors.cardBorder, RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .background(shimmerBrush)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
        }
    }
}
