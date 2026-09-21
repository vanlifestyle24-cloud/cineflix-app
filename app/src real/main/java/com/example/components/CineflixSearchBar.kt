package com.example.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MediaItem
import com.example.ui.theme.CineflixTheme

/**
 * CINEFLIX Live Search Bar & Reactive Results Dropdown
 * Fully connected to:
 * - OMDb Dual API Keys (Primary: 38df43c1, Backup: 71610ba3)
 * - Supabase Database Cache (irhltbeyztlvscyurbfb)
 * - Live Rate Limit & Cache Status Indicator
 */
@Composable
fun CineflixSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    searchResults: List<MediaItem>,
    onSelectMedia: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    statusMessage: String? = null,
    cacheSourceBadge: String? = null,
    isRateLimited: Boolean = false,
    placeholderText: String = "Search movies via OMDb API & Supabase cache..."
) {
    Column(modifier = modifier) {
        // Search Input Field
        Box(
            modifier = Modifier
                .testTag("search_bar_input")
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(CineflixTheme.colors.cardBackgroundHover)
                .border(
                    width = if (isRateLimited) 1.5.dp else 1.dp,
                    color = if (isRateLimited) Color(0xFFFF9900) else CineflixTheme.colors.cardBorder,
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (isRateLimited) Color(0xFFFF9900) else CineflixTheme.colors.accentRed,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholderText,
                            style = CineflixTheme.typography.bodyMedium,
                            color = CineflixTheme.colors.textTertiary,
                            maxLines = 1
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        cursorBrush = SolidColor(CineflixTheme.colors.accentRed),
                        textStyle = CineflixTheme.typography.bodyMedium.copy(color = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // OMDb Live Active Connected Badge (visible in Search Box)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x33FFB300))
                        .border(0.5.dp, Color(0x66FFB300), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "OMDb LIVE",
                        style = CineflixTheme.typography.monoBadge.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB300)
                        )
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = CineflixTheme.colors.accentRed
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onClearClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = CineflixTheme.colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Quick Search Chips to instantly test OMDb API with 1 tap
        val quickKeywords = listOf("Avengers", "Batman", "Spider-Man", "Interstellar", "Oppenheimer", "Matrix")
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(quickKeywords) { keyword ->
                val isSelected = query.equals(keyword, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) CineflixTheme.colors.accentRed
                            else Color(0x22FFFFFF)
                        )
                        .clickable { onQueryChange(keyword) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = keyword,
                        style = CineflixTheme.typography.caption.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else CineflixTheme.colors.textSecondary
                        )
                    )
                }
            }
        }

        // Live Results Dropdown Preview
        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CineflixTheme.colors.backgroundElevated)
                    .border(1.dp, CineflixTheme.colors.cardBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Column {
                    // Caching & Rate-Limit Status Header Banner
                    if (statusMessage != null || cacheSourceBadge != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isRateLimited -> Color(0x33FF9900)
                                        cacheSourceBadge?.contains("SUPABASE", ignoreCase = true) == true -> Color(0x223ECF8E)
                                        cacheSourceBadge?.contains("LOCAL", ignoreCase = true) == true -> Color(0x2200E5FF)
                                        else -> Color(0x22E50914)
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = when {
                                        isRateLimited -> Color(0x66FF9900)
                                        cacheSourceBadge?.contains("SUPABASE", ignoreCase = true) == true -> Color(0x663ECF8E)
                                        cacheSourceBadge?.contains("LOCAL", ignoreCase = true) == true -> Color(0x6600E5FF)
                                        else -> Color(0x44E50914)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = when {
                                        isRateLimited -> Icons.Default.Warning
                                        cacheSourceBadge?.contains("SUPABASE", ignoreCase = true) == true -> Icons.Default.CloudDone
                                        else -> Icons.Default.Bolt
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        isRateLimited -> Color(0xFFFF9900)
                                        cacheSourceBadge?.contains("SUPABASE", ignoreCase = true) == true -> Color(0xFF3ECF8E)
                                        cacheSourceBadge?.contains("LOCAL", ignoreCase = true) == true -> Color(0xFF00E5FF)
                                        else -> CineflixTheme.colors.accentRed
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = statusMessage ?: "Live Search",
                                    style = CineflixTheme.typography.caption.copy(fontSize = 11.sp),
                                    color = Color.White
                                )
                            }

                            if (cacheSourceBadge != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (cacheSourceBadge.contains("SUPABASE", ignoreCase = true)) Color(0xFF3ECF8E)
                                            else Color(0xFFE50914)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = cacheSourceBadge,
                                        style = CineflixTheme.typography.monoBadge.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isLoading) "Searching Supabase & OMDb..." else "No results found for \"$query\"",
                                style = CineflixTheme.typography.bodyMedium,
                                color = CineflixTheme.colors.textTertiary
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                        ) {
                            items(searchResults) { media ->
                                Row(
                                    modifier = Modifier
                                        .testTag("search_result_${media.id}")
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onSelectMedia(media) }
                                        .padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Movie Poster Thumbnail
                                        Box(
                                            modifier = Modifier
                                                .size(width = 38.dp, height = 52.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(CineflixTheme.colors.cardBackground),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!media.posterUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = media.posterUrl,
                                                    contentDescription = media.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = CineflixTheme.colors.accentRed,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = media.title,
                                                style = CineflixTheme.typography.h6,
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "${media.releaseYear} • ${media.primaryGenre} • ${media.duration}",
                                                style = CineflixTheme.typography.caption,
                                                color = CineflixTheme.colors.textSecondary
                                            )
                                        }
                                    }

                                    RatingBadge(rating = media.imdbRating)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
