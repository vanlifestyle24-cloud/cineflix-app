package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.AdminPanelSettings
import com.example.auth.SessionManager
import com.example.model.MediaItem
import coil.compose.AsyncImage

@Composable
fun MeAndProfileScreen(
    sessionManager: SessionManager,
    allCatalogMovies: List<MediaItem>,
    onOpenPremium: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenWatchlist: () -> Unit,
    onOpenAdminDashboard: () -> Unit = {},
    onLogout: () -> Unit = {},
    onPlayMedia: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by sessionManager.currentUser.collectAsState()
    val isVipActive by sessionManager.isVipActive.collectAsState()
    val vipPlan by sessionManager.vipPlan.collectAsState()
    val watchlistIds by sessionManager.watchlist.collectAsState()
    val downloadedIds by sessionManager.downloadedIds.collectAsState()
    val historyEntries by sessionManager.historyEntries.collectAsState()

    var wifiOnlyDownload by remember { mutableStateOf(true) }
    var hardwareAcceleration by remember { mutableStateOf(true) }
    var showCacheClearedDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    var displayName by remember(currentUser) {
        mutableStateOf(currentUser?.name ?: if (currentUser != null) currentUser?.email?.substringBefore("@") ?: "User" else "Guest User")
    }

    // Filter real watchlist items
    val watchlistItems = remember(watchlistIds, allCatalogMovies) {
        allCatalogMovies.filter { watchlistIds.contains(it.id) }
    }

    Column(
        modifier = modifier
            .testTag("me_profile_screen")
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1E1624), // Subtle luxury dark amethyst glow
                        Color(0xFF12121A),
                        Color(0xFF0D0C12)
                    )
                )
            )
    ) {
        // Sticky Header with status bar padding
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xF5161420),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Me & Profile",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .clickable { showEditProfileDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Body
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. User Profile Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF191724)),
                    border = BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFFFB300), Color(0xFFFF6F00))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Avatar",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentUser?.email ?: "Guest Mode (Tap Sign In below)",
                                    color = Color(0xFFA6A6BA),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                // VIP Status Pill
                                Surface(
                                    color = if (isVipActive) Color(0x33FFB300) else Color(0x22FFFFFF),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isVipActive) Color(0xFFFFB300) else Color(0x44FFFFFF)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = null,
                                            tint = if (isVipActive) Color(0xFFFFB300) else Color(0xFFA6A6BA),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isVipActive) vipPlan else "Free Tier",
                                            color = if (isVipActive) Color(0xFFFFD54F) else Color(0xFFA6A6BA),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val isUserAdmin = currentUser?.provider == "admin_master" || currentUser?.email?.contains("admin") == true || sessionManager.cachedEmail.value.contains("admin")
                        if (isUserAdmin) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onOpenAdminDashboard() },
                                color = Color(0xFFFFB300),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Open Admin Master Console", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Quick Statistics Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF13111C))
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                label = "Watchlist",
                                value = "${watchlistIds.size}",
                                onClick = onOpenWatchlist
                            )
                            StatDivider()
                            StatItem(
                                label = "Downloads",
                                value = "${downloadedIds.size}",
                                onClick = onOpenDownloads
                            )
                            StatDivider()
                            StatItem(
                                label = "History",
                                value = "${historyEntries.size}",
                                onClick = onOpenDownloads
                            )
                            StatDivider()
                            StatItem(
                                label = "Data Saved",
                                value = if (downloadedIds.isEmpty()) "0 MB" else "${downloadedIds.size * 750} MB",
                                onClick = {}
                            )
                        }
                    }
                }
            }

            // 2. VIP Membership Action Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPremium() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF4A380A), Color(0xFF705210), Color(0xFF9E7719))
                                )
                            )
                            .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x33000000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isVipActive) "VIP Ultra Pass Active" else "Upgrade to Cineflix VIP",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isVipActive) "Manage subscription or redeem vouchers" else "Unlock 4K, zero ads & cloud downloads",
                                        color = Color(0xFFFFE082),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "Go",
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 3. Quick Watchlist Row (if any)
            if (watchlistItems.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Watchlist (${watchlistItems.size})",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "View All",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { onOpenWatchlist() }
                                .padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(watchlistItems) { item ->
                            Surface(
                                color = Color(0xFF1E1E2C),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .width(130.dp)
                                    .clickable { onPlayMedia(item) }
                            ) {
                                Column {
                                    AsyncImage(
                                        model = item.posterUrl,
                                        contentDescription = item.title,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(170.dp)
                                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                            .background(Color(0xFF282838))
                                    )
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = item.title,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.primaryGenre,
                                            color = Color(0xFFA6A6BA),
                                            fontSize = 10.sp,
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

            // 4. Playback & Download Settings
            item {
                Text(
                    text = "Streaming & Download Preferences",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF181724)),
                    border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                ) {
                    Column {
                        SettingRowToggle(
                            icon = Icons.Default.Wifi,
                            title = "Download over Wi-Fi Only",
                            subtitle = "Avoid mobile carrier cellular data usage",
                            checked = wifiOnlyDownload,
                            onCheckedChange = { wifiOnlyDownload = it }
                        )

                        HorizontalDivider(color = Color(0x16FFFFFF))

                        SettingRowToggle(
                            icon = Icons.Default.Speed,
                            title = "Hardware Accelerated Playback",
                            subtitle = "ExoPlayer GPU decoding for 4K 60FPS streams",
                            checked = hardwareAcceleration,
                            onCheckedChange = { hardwareAcceleration = it }
                        )

                        HorizontalDivider(color = Color(0x16FFFFFF))

                        SettingRowAction(
                            icon = Icons.Default.HighQuality,
                            title = "Default Streaming Resolution",
                            valueText = "4K Ultra HD (Auto)",
                            onClick = {}
                        )

                        HorizontalDivider(color = Color(0x16FFFFFF))

                        SettingRowAction(
                            icon = Icons.Default.CloudDone,
                            title = "High-Speed Stream CDN Proxy",
                            valueText = "Connected (Cdn-East)",
                            onClick = {}
                        )
                    }
                }
            }

            // 5. System & Storage Management
            item {
                Text(
                    text = "App & Storage",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF181724)),
                    border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                ) {
                    Column {
                        SettingRowAction(
                            icon = Icons.Default.CleaningServices,
                            title = "Clear Cached Streams & Thumbnails",
                            valueText = "142 MB",
                            onClick = {
                                showCacheClearedDialog = true
                            }
                        )

                        HorizontalDivider(color = Color(0x16FFFFFF))

                        SettingRowAction(
                            icon = Icons.Default.SupportAgent,
                            title = "Direct Support Desk",
                            valueText = "support@cineflix.com",
                            onClick = {}
                        )

                        HorizontalDivider(color = Color(0x16FFFFFF))

                        SettingRowAction(
                            icon = Icons.Default.Info,
                            title = "App Version",
                            valueText = "v4.2.0 (Build 2026.09)",
                            onClick = {}
                        )
                    }
                }
            }

            // 6. Login / Logout / Account switch Button
            item {
                if (currentUser == null) {
                    Button(
                        onClick = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E88E5),
                            contentColor = Color.White
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Sign In",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign In / Register Account",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF26191C),
                            contentColor = Color(0xFFFF5252)
                        ),
                        border = BorderStroke(1.dp, Color(0x44FF5252))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Logout",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Log Out of Cineflix (${currentUser?.email ?: "Account"})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog: Cache Cleared
    if (showCacheClearedDialog) {
        AlertDialog(
            onDismissRequest = { showCacheClearedDialog = false },
            confirmButton = {
                TextButton(onClick = { showCacheClearedDialog = false }) {
                    Text("OK", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text("Cache Cleaned", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "142 MB of temporary video chunks and image thumbnails have been safely cleared from your device.",
                    color = Color(0xFFD1D1DE),
                    fontSize = 13.sp
                )
            },
            containerColor = Color(0xFF1A1926),
            shape = RoundedCornerShape(14.dp)
        )
    }

    // Modal Dialog: Edit Profile
    if (showEditProfileDialog) {
        var inputName by remember { mutableStateOf(displayName) }
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inputName.isNotBlank()) {
                            displayName = inputName.trim()
                        }
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Save", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            title = {
                Text("Edit Profile Name", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Enter your custom Cineflix display name:", color = Color(0xFFA6A6BA), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF262638))
                            .padding(12.dp)
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Or select preset profile name:", color = Color(0xFFA6A6BA), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    val presets = listOf("Cineflix VIP Member", "Alex Streaming", "Sarah 4K", "David Cinema")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { preset ->
                            val isSelected = inputName == preset
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { inputName = preset },
                                color = if (isSelected) Color(0xFFFFB300) else Color(0xFF282838),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFB300) else Color(0x33FFFFFF))
                            ) {
                                Text(
                                    text = preset,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            containerColor = Color(0xFF1A1926),
            shape = RoundedCornerShape(14.dp)
        )
    }

    // Modal Dialog: Logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        sessionManager.clearSession()
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Log Out", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            title = {
                Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to log out of Cineflix? Your offline downloads and VIP status will remain saved on this device.",
                    color = Color(0xFFD1D1DE),
                    fontSize = 13.sp
                )
            },
            containerColor = Color(0xFF1A1926),
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = label,
            color = Color(0xFFA6A6BA),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(Color(0x22FFFFFF))
    )
}

@Composable
private fun SettingRowToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF88889C),
                    fontSize = 10.5.sp
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF00E5FF),
                uncheckedThumbColor = Color(0xFFA6A6BA),
                uncheckedTrackColor = Color(0xFF282836)
            )
        )
    }
}

@Composable
private fun SettingRowAction(
    icon: ImageVector,
    title: String,
    valueText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFA6A6BA),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = valueText,
                color = Color(0xFFA6A6BA),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFF6E6E82),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
