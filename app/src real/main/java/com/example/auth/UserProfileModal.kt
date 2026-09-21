package com.example.auth

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.components.CineflixGhostButton
import com.example.components.CineflixPrimaryButton
import com.example.components.QualityBadge
import com.example.ui.theme.CineflixTheme

@Composable
fun UserProfileModal(
    user: UserProfile,
    watchlistCount: Int,
    onLogout: () -> Unit,
    onUpgradePlan: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .testTag("user_profile_modal_overlay")
                .fillMaxSize()
                .background(CineflixTheme.colors.backgroundOverlay)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CineflixTheme.colors.cardBorder, RoundedCornerShape(16.dp))
                    .clickable(enabled = false) {},
                colors = CardDefaults.cardColors(containerColor = CineflixTheme.colors.backgroundSecondary)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Account Profile",
                            style = CineflixTheme.typography.h3,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CineflixTheme.colors.cardBackground)
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // User Avatar and Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    if (user.provider == "google") CineflixTheme.colors.primaryGradientBrush
                                    else SolidColor(CineflixTheme.colors.cardBackground)
                                )
                                .border(2.dp, CineflixTheme.colors.accentRed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.name.take(1).uppercase(),
                                style = CineflixTheme.typography.h3,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.name,
                                    style = CineflixTheme.typography.h4,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                QualityBadge(label = if (user.provider == "google") "GOOGLE AUTH" else "SUPABASE")
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = user.email,
                                style = CineflixTheme.typography.bodySmall,
                                color = CineflixTheme.colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = CineflixTheme.colors.cardBorder)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Local Cache Status Card (Highlights user's request: cash / local persistence)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CineflixTheme.colors.cardBackground)
                            .border(1.dp, CineflixTheme.colors.cardBorder, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = CineflixTheme.colors.accentPink,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Mobile Cache & Rate Limiting",
                                    style = CineflixTheme.typography.h6,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Login session & watchlist are saved locally on this device. Future launches load instantly from mobile cache without repeating auth requests to Supabase.",
                                style = CineflixTheme.typography.caption,
                                color = CineflixTheme.colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Watchlist Count Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CineflixTheme.colors.cardBackground)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = CineflixTheme.colors.accentRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Saved in Watchlist",
                                style = CineflixTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "$watchlistCount titles",
                            style = CineflixTheme.typography.monoBadge,
                            color = CineflixTheme.colors.accentPink
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cloud Sync Status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CineflixTheme.colors.cardBackground)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = CineflixTheme.colors.success,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Supabase Cloud Sync",
                                style = CineflixTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "ONLINE",
                            style = CineflixTheme.typography.monoBadge,
                            color = CineflixTheme.colors.success
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Plan CTA
                    CineflixPrimaryButton(
                        text = "Upgrade Streaming Plan",
                        useGradient = true,
                        onClick = {
                            onDismiss()
                            onUpgradePlan()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "profile_btn_upgrade"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Logout Button
                    CineflixGhostButton(
                        text = "Log Out from Device",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        onClick = {
                            onLogout()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "profile_btn_logout"
                    )
                }
            }
        }
    }
}
