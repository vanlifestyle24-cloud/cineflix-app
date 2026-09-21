package com.example.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CineflixTheme

import com.example.auth.UserProfile

enum class CineflixNavDestination(val label: String) {
    HOME("Home"),
    MOVIES("Movies"),
    SERIES("Series"),
    SPECIALS("Specials"),
    WATCHLIST("My List"),
    DESIGN_SYSTEM("Design System")
}

/**
 * CINEFLIX Sticky Glassmorphic Navigation Bar
 */
@Composable
fun CineflixNavbar(
    currentDestination: CineflixNavDestination,
    currentUser: UserProfile? = null,
    onNavigate: (CineflixNavDestination) -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogoutClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("cineflix_navbar")
            .fillMaxWidth()
            .background(CineflixTheme.colors.backgroundNavbar)
            .border(
                width = 0.5.dp,
                color = CineflixTheme.colors.cardBorder,
                shape = RoundedCornerShape(0.dp)
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo: Cineflix
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onNavigate(CineflixNavDestination.HOME) }
                    .padding(end = 12.dp)
            ) {
                // Glowing Play/C logo icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CineflixTheme.colors.primaryGradientBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Cineflix icon",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CINEFLIX",
                    style = CineflixTheme.typography.h4.copy(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = CineflixTheme.colors.accentRed
                )
            }

            // Navigation Links (Tablet/Desktop or quick pills)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Quick toggle for Design System vs Home
                val isSpec = currentDestination == CineflixNavDestination.DESIGN_SYSTEM
                Box(
                    modifier = Modifier
                        .testTag("nav_item_spec")
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSpec) CineflixTheme.colors.accentRed.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .border(
                            1.dp,
                            if (isSpec) CineflixTheme.colors.accentRed else Color.Transparent,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onNavigate(
                                if (isSpec) CineflixNavDestination.HOME
                                else CineflixNavDestination.DESIGN_SYSTEM
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isSpec) "Exit Spec" else "Design Tokens",
                        style = CineflixTheme.typography.monoBadge,
                        color = if (isSpec) CineflixTheme.colors.accentRed else CineflixTheme.colors.textSecondary
                    )
                }
            }

            // Right Action Controls: Search, Notifications, Settings, Profile
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Trigger
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onSearchClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = CineflixTheme.colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Notifications Bell with Badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onNotificationsClick),
                    contentAlignment = Alignment.Center
                ) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = CineflixTheme.colors.accentRed,
                                contentColor = Color.White
                            ) {
                                Text("3", style = CineflixTheme.typography.caption)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = CineflixTheme.colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Settings Modal Trigger
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onSettingsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = CineflixTheme.colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // User Profile & Log In / Log Out buttons
                if (currentUser != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Avatar (opens profile modal)
                        Box(
                            modifier = Modifier
                                .testTag("nav_profile_avatar")
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (currentUser.provider == "google") CineflixTheme.colors.primaryGradientBrush
                                    else SolidColor(CineflixTheme.colors.accentRed)
                                )
                                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(8.dp))
                                .clickable(onClick = onProfileClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser.name.take(1).uppercase(),
                                style = CineflixTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        // Explicit Log Out Button
                        if (onLogoutClick != null) {
                            Box(
                                modifier = Modifier
                                    .testTag("nav_logout_button")
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x33FF4C4C))
                                    .border(1.dp, Color(0x66FF4C4C), RoundedCornerShape(16.dp))
                                    .clickable(onClick = onLogoutClick)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Log Out",
                                        tint = CineflixTheme.colors.accentRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Log Out",
                                        style = CineflixTheme.typography.monoBadge,
                                        color = CineflixTheme.colors.accentRed
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .testTag("nav_signin_button")
                            .clip(RoundedCornerShape(16.dp))
                            .background(CineflixTheme.colors.accentRed)
                            .clickable(onClick = onProfileClick)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Log In",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Log In",
                                style = CineflixTheme.typography.buttonSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
