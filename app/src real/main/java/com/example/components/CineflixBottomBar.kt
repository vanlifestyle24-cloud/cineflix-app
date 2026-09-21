package com.example.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The 4 main bottom navigation destinations requested by user:
 * 1. Home
 * 2. Premium
 * 3. Downloads & History
 * 4. Me & Profile
 */
enum class CineflixBottomTab(
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Default.Home, "bottom_tab_home"),
    PREMIUM("Premium", Icons.Default.WorkspacePremium, "bottom_tab_premium"),
    DOWNLOADS_HISTORY("Downloads", Icons.Default.FileDownload, "bottom_tab_downloads_history"),
    ME_PROFILE("Profile", Icons.Default.Person, "bottom_tab_me_profile")
}

@Composable
fun CineflixBottomBar(
    currentTab: CineflixBottomTab,
    onTabSelected: (CineflixBottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("cineflix_bottom_bar")
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // 3D Floating Elevated Pill Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(26.dp),
                    spotColor = Color(0xFF00E5FF).copy(alpha = 0.25f),
                    ambientColor = Color.Black
                ),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xF513121C), // Obsidian gloss
            border = BorderStroke(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0x66FFFFFF),
                        Color(0x2200E5FF),
                        Color(0x33FFB300),
                        Color(0x22FFFFFF)
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CineflixBottomTab.values().forEach { tab ->
                    val isSelected = currentTab == tab

                    val activeColor = when (tab) {
                        CineflixBottomTab.PREMIUM -> Color(0xFFFFD54F) // Gold for VIP
                        CineflixBottomTab.DOWNLOADS_HISTORY -> Color(0xFF00E5FF) // Neon Cyan
                        CineflixBottomTab.ME_PROFILE -> Color(0xFFFF4081) // Neon Pink-Amethyst
                        CineflixBottomTab.HOME -> Color(0xFFE50914) // Cinematic Netflix Red
                    }

                    val animScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.12f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "tab_scale"
                    )

                    val animColor by animateColorAsState(
                        targetValue = if (isSelected) activeColor else Color(0xFF8E8EA6),
                        label = "tab_color"
                    )

                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .testTag(tab.testTag)
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) {
                                    Brush.verticalGradient(
                                        listOf(
                                            activeColor.copy(alpha = 0.22f),
                                            activeColor.copy(alpha = 0.06f)
                                        )
                                    )
                                } else {
                                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        0.8.dp,
                                        activeColor.copy(alpha = 0.5f),
                                        RoundedCornerShape(20.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                onTabSelected(tab)
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.scale(animScale)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(24.dp)
                            ) {
                                if (isSelected) {
                                    // 3D Neon Radial Back-Glow
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(
                                                        activeColor.copy(alpha = 0.6f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )
                                }

                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = animColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = tab.label,
                                color = animColor,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
