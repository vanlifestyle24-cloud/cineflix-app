package com.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CineflixTheme

/**
 * CINEFLIX Multi-Column Responsive Footer
 */
@Composable
fun CineflixFooter(
    onSubscriptionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLanguage by remember { mutableStateOf("English (India / US)") }
    var languageMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .testTag("cineflix_footer")
            .fillMaxWidth()
            .background(CineflixTheme.colors.backgroundSecondary)
            .border(0.5.dp, CineflixTheme.colors.cardBorder, RoundedCornerShape(0.dp))
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        // Language Selector & Plan CTA
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language dropdown
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CineflixTheme.colors.cardBackground)
                        .border(1.dp, CineflixTheme.colors.cardBorder, RoundedCornerShape(6.dp))
                        .clickable { languageMenuOpen = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = CineflixTheme.colors.textSecondary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = selectedLanguage,
                        style = CineflixTheme.typography.bodySmall,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = CineflixTheme.colors.textSecondary
                    )
                }

                DropdownMenu(
                    expanded = languageMenuOpen,
                    onDismissRequest = { languageMenuOpen = false },
                    modifier = Modifier.background(CineflixTheme.colors.backgroundElevated)
                ) {
                    listOf("English (India / US)", "Hindi (हिंदी)", "Tamil (தமிழ்)", "Telugu (తెలుగు)", "Spanish (Español)").forEach { lang ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = lang,
                                    style = CineflixTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            },
                            onClick = {
                                selectedLanguage = lang
                                languageMenuOpen = false
                            }
                        )
                    }
                }
            }

            // Upgrade / Subscribe CTA
            CineflixPrimaryButton(
                text = "Upgrade Plan",
                useGradient = true,
                onClick = onSubscriptionClick,
                testTag = "footer_upgrade_button"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Multi-column link sections
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Column 1: Platform & Experience
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PLATFORM",
                    style = CineflixTheme.typography.h6,
                    color = CineflixTheme.colors.accentRed
                )
                Spacer(modifier = Modifier.height(10.dp))
                listOf("Originals", "Trending Movies", "TV Series", "Live Sports (Jio)", "Hotstar Specials").forEach { item ->
                    Text(
                        text = item,
                        style = CineflixTheme.typography.bodySmall,
                        color = CineflixTheme.colors.textSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // Column 2: Tech Specs & Audio
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "EXPERIENCE",
                    style = CineflixTheme.typography.h6,
                    color = CineflixTheme.colors.accentPurple
                )
                Spacer(modifier = Modifier.height(10.dp))
                listOf("4K Ultra HD", "Dolby Vision", "Dolby Atmos 5.1", "Offline Downloads", "Parental Controls").forEach { item ->
                    Text(
                        text = item,
                        style = CineflixTheme.typography.bodySmall,
                        color = CineflixTheme.colors.textSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // Column 3: Help & Legal
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SUPPORT",
                    style = CineflixTheme.typography.h6,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                listOf("Help Center", "Device Compatibility", "Speed Test", "Privacy & Cookies", "WCAG AA Specs").forEach { item ->
                    Text(
                        text = item,
                        style = CineflixTheme.typography.bodySmall,
                        color = CineflixTheme.colors.textSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = CineflixTheme.colors.cardBorder)
        Spacer(modifier = Modifier.height(16.dp))

        // Copyright and design system attribution
        Text(
            text = "CINEFLIX OTT DESIGN SYSTEM • Combining the best of Netflix, Prime Video, Disney+ Hotstar & JioCinema.",
            style = CineflixTheme.typography.caption,
            color = CineflixTheme.colors.textTertiary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "WCAG AA Contrast Compliant • 60fps Glassmorphic Theme • Material 3 Adaptive Layout",
            style = CineflixTheme.typography.monoBadge,
            color = CineflixTheme.colors.textTertiary
        )
    }
}
