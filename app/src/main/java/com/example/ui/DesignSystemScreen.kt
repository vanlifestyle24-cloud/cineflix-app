package com.example.ui

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.AgeRatingBadge
import com.example.components.CineflixGenreChip
import com.example.components.CineflixGhostButton
import com.example.components.CineflixIconButton
import com.example.components.CineflixPrimaryButton
import com.example.components.CineflixSecondaryButton
import com.example.components.MatchBadge
import com.example.components.PlatformBadge
import com.example.components.QualityBadge
import com.example.components.RatingBadge
import com.example.components.SkeletonMediaCard
import com.example.components.Top10Badge
import com.example.ui.theme.CineflixTheme

/**
 * CINEFLIX Design System Specification & Token Showcase
 * Interactive live inspector for designers, developers, and QA.
 */
@Composable
fun DesignSystemScreen(
    onOpenTrailerModal: () -> Unit,
    onOpenSubscriptionModal: () -> Unit,
    onOpenSettingsModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableStateOf("Colors") }

    Column(
        modifier = modifier
            .testTag("design_system_screen")
            .fillMaxSize()
            .background(CineflixTheme.colors.backgroundPrimary)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1440.dp)
        ) {
            Column {
                // Header Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CineflixTheme.colors.backgroundElevated),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = CineflixTheme.colors.primaryGradientBrush)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlatformBadge(badgeText = "DESIGN SYSTEM V2.4")
                            QualityBadge(label = "WCAG AA")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "CINEFLIX OTT DESIGN SYSTEM",
                            style = CineflixTheme.typography.h2,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Combining the signature architectural elements of Netflix (bold crimson, top 10 badges), Prime Video (x-ray charcoal layouts), Disney+ Hotstar (deep royal glassmorphism), and JioCinema (vibrant purple-pink gradients).",
                            style = CineflixTheme.typography.bodyMedium,
                            color = CineflixTheme.colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section Selector Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Colors", "Typography", "Components", "Breakpoints & Motion").forEach { tab ->
                        val isSelected = tab == selectedTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) CineflixTheme.colors.accentRed
                                    else CineflixTheme.colors.cardBackground
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) CineflixTheme.colors.accentRed else CineflixTheme.colors.cardBorder,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedTab = tab }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                style = CineflixTheme.typography.buttonSmall,
                                color = if (isSelected) Color.White else CineflixTheme.colors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                when (selectedTab) {
                    "Colors" -> ColorTokensSection()
                    "Typography" -> TypographyTokensSection()
                    "Components" -> ComponentsCatalogSection(
                        onOpenTrailer = onOpenTrailerModal,
                        onOpenSubscription = onOpenSubscriptionModal,
                        onOpenSettings = onOpenSettingsModal
                    )
                    "Breakpoints & Motion" -> BreakpointsAndMotionSection()
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun ColorTokensSection() {
    Column {
        Text(
            text = "1. COLOR PALETTE SPECIFICATION",
            style = CineflixTheme.typography.h4,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "WCAG AA Contrast Compliant tokens for high visual fidelity in dark rooms.",
            style = CineflixTheme.typography.bodySmall,
            color = CineflixTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Grid of Color Swatches
        val swatches = listOf(
            Triple("Background Primary", "#0B0B0F", CineflixTheme.colors.backgroundPrimary),
            Triple("Background Secondary", "#111118", CineflixTheme.colors.backgroundSecondary),
            Triple("Background Elevated", "#171722", CineflixTheme.colors.backgroundElevated),
            Triple("Accent 1: Netflix Red", "#E50914", CineflixTheme.colors.accentRed),
            Triple("Accent 2: Violet Pink", "#7B2FF7 → #FF3CAC", CineflixTheme.colors.accentPurple),
            Triple("Text Primary", "#FFFFFF", CineflixTheme.colors.textPrimary),
            Triple("Text Secondary", "#A0A0B2", CineflixTheme.colors.textSecondary),
            Triple("Text Tertiary", "#6E6E82", CineflixTheme.colors.textTertiary),
            Triple("Success Green", "#22C55E", CineflixTheme.colors.success),
            Triple("Error Red", "#EF4444", CineflixTheme.colors.error),
            Triple("Warning Amber", "#F59E0B", CineflixTheme.colors.warning),
            Triple("Card Glass Background", "rgba(26,26,40, 0.4)", CineflixTheme.colors.cardBackground)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            swatches.forEach { (name, hex, color) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CineflixTheme.colors.cardBackground)
                        .border(1.dp, CineflixTheme.colors.cardBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = name, style = CineflixTheme.typography.h6, color = Color.White)
                            Text(text = hex, style = CineflixTheme.typography.monoBadge, color = CineflixTheme.colors.textSecondary)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x3322C55E))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "AA PASS", style = CineflixTheme.typography.monoBadge, color = CineflixTheme.colors.success)
                    }
                }
            }
        }
    }
}

@Composable
private fun TypographyTokensSection() {
    Column {
        Text(
            text = "2. TYPOGRAPHY SCALE SPECIFICATION",
            style = CineflixTheme.typography.h4,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Clean display scale for headings, performant system stack for body, monospace for technical metadata.",
            style = CineflixTheme.typography.bodySmall,
            color = CineflixTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        val typeScale = listOf(
            Triple("H1 Display", "32sp / Bold", CineflixTheme.typography.h1),
            Triple("H2 Title", "26sp / ExtraBold", CineflixTheme.typography.h2),
            Triple("H3 Subtitle", "22sp / Bold", CineflixTheme.typography.h3),
            Triple("H4 Section", "18sp / Bold", CineflixTheme.typography.h4),
            Triple("H5 Card Title", "16sp / SemiBold", CineflixTheme.typography.h5),
            Triple("H6 Item Title", "14sp / SemiBold", CineflixTheme.typography.h6),
            Triple("Body Large", "16sp / Regular", CineflixTheme.typography.bodyLarge),
            Triple("Body Medium", "14sp / Regular", CineflixTheme.typography.bodyMedium),
            Triple("Body Small", "12sp / Regular", CineflixTheme.typography.bodySmall),
            Triple("Caption", "11sp / Medium", CineflixTheme.typography.caption),
            Triple("Button Action", "14sp / Bold Tracking", CineflixTheme.typography.button),
            Triple("Monospace Rating", "12sp / Monospace Bold", CineflixTheme.typography.monoRating),
            Triple("Monospace Metadata", "10sp / Monospace Codec", CineflixTheme.typography.monoBadge)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            typeScale.forEach { (token, desc, style) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CineflixTheme.colors.cardBackground)
                        .border(1.dp, CineflixTheme.colors.cardBorder, RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = token, style = CineflixTheme.typography.monoBadge, color = CineflixTheme.colors.accentPink)
                        Text(text = desc, style = CineflixTheme.typography.monoBadge, color = CineflixTheme.colors.textTertiary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "The quick brown fox streams in 4K UHD Atmos",
                        style = style,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ComponentsCatalogSection(
    onOpenTrailer: () -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column {
        Text(
            text = "3. INTERACTIVE COMPONENT MATRIX",
            style = CineflixTheme.typography.h4,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Buttons Section
        Text(text = "BUTTON VARIANTS", style = CineflixTheme.typography.h5, color = CineflixTheme.colors.accentRed)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CineflixPrimaryButton(text = "Primary Red", onClick = {}, modifier = Modifier.weight(1f))
            CineflixPrimaryButton(text = "Gradient", useGradient = true, onClick = {}, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CineflixSecondaryButton(text = "Secondary Glass", onClick = {}, modifier = Modifier.weight(1f))
            CineflixGhostButton(text = "Ghost Outline", onClick = {}, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Badges Section
        Text(text = "BADGES & METADATA", style = CineflixTheme.typography.h5, color = CineflixTheme.colors.accentPurple)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RatingBadge(rating = 9.4)
            MatchBadge(matchPercent = 99)
            QualityBadge(label = "4K UHD")
            AgeRatingBadge(ageRating = "18+")
            Top10Badge(rank = 1)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlatformBadge(badgeText = "CINEFLIX ORIGINAL")
            PlatformBadge(badgeText = "PRIME EXCLUSIVE")
            PlatformBadge(badgeText = "HOTSTAR SPECIAL")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Genre Chips Section
        Text(text = "PILL GENRE CHIPS", style = CineflixTheme.typography.h5, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CineflixGenreChip(label = "Active Gradient", isSelected = true, onClick = {})
            CineflixGenreChip(label = "Default Glass", isSelected = false, onClick = {})
            CineflixGenreChip(label = "Sci-Fi", isSelected = false, onClick = {})
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Skeleton Shimmer Section
        Text(text = "LOADING SKELETON SHIMMER", style = CineflixTheme.typography.h5, color = CineflixTheme.colors.textSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkeletonMediaCard(modifier = Modifier.weight(1f))
            SkeletonMediaCard(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Modal Dialog Triggers
        Text(text = "MODAL DIALOG LAUNCHERS", style = CineflixTheme.typography.h5, color = CineflixTheme.colors.accentPink)
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CineflixPrimaryButton(
                text = "Launch Trailer Player Modal (Interactive)",
                icon = Icons.Default.PlayArrow,
                onClick = onOpenTrailer,
                modifier = Modifier.fillMaxWidth()
            )
            CineflixSecondaryButton(
                text = "Launch Subscription Tier Modal",
                onClick = onOpenSubscription,
                modifier = Modifier.fillMaxWidth()
            )
            CineflixGhostButton(
                text = "Launch Video & Audio Preferences Modal",
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BreakpointsAndMotionSection() {
    Column {
        Text(
            text = "4. RESPONSIVE BREAKPOINTS & MOTION SPECIFICATION",
            style = CineflixTheme.typography.h4,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))

        val breakpoints = listOf(
            Triple("Mobile", "320dp - 767dp", "2 Columns • 50vh Hero • Bottom Gesture Pill Insets"),
            Triple("Tablet", "768dp - 1023dp", "3-4 Columns • 70vh Hero • Side Rail Support"),
            Triple("Desktop", "1024dp - 1440dp", "5 Columns • 75vh Hero • Sticky Blur Top Nav"),
            Triple("Large Desktop", "1440dp+", "6 Columns • Max-width 1440dp Centered Container")
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            breakpoints.forEach { (device, range, layout) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CineflixTheme.colors.cardBackground)
                        .border(1.dp, CineflixTheme.colors.cardBorder, RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = device, style = CineflixTheme.typography.h5, color = Color.White)
                        Text(text = range, style = CineflixTheme.typography.monoBadge, color = CineflixTheme.colors.accentRed)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = layout, style = CineflixTheme.typography.bodySmall, color = CineflixTheme.colors.textSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "MOTION PRINCIPLES",
            style = CineflixTheme.typography.h5,
            color = CineflixTheme.colors.accentPink
        )
        Spacer(modifier = Modifier.height(8.dp))

        listOf(
            "Card Hover & Press" to "Scale 0.96f / 1.05f with shadow glow and border tint (FastOutSlowIn, 180ms)",
            "Ken-Burns Hero Animation" to "Continuous subtle ambient zoom from 1.0f to 1.08f over 7s cycle",
            "Shimmer Skeleton" to "Smooth 1200ms linear gradient sweep across card placeholders",
            "Page & Modal Transitions" to "Fade-in (200ms) with subtle slide-in vertically from 40dp offset"
        ).forEach { (title, desc) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = CineflixTheme.colors.success,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = title, style = CineflixTheme.typography.h6, color = Color.White)
                    Text(text = desc, style = CineflixTheme.typography.caption, color = CineflixTheme.colors.textSecondary)
                }
            }
        }
    }
}
