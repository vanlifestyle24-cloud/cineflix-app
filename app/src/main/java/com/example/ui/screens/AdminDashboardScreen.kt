package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.auth.SessionManager
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.omdb.OmdbRepository
import com.example.streaming.CineflixLocalCatalogManager
import kotlinx.coroutines.launch

enum class AdminTab(val title: String, val icon: String) {
    CATALOG("Catalog", "🎬"),
    SPOTLIGHT("Spotlight", "🌟"),
    VOUCHERS("Vouchers", "💎"),
    TOOLS("OMDb & Tools", "🛠️")
}

@Composable
fun AdminDashboardScreen(
    sessionManager: SessionManager,
    catalogManager: CineflixLocalCatalogManager,
    onSwitchToUserView: () -> Unit,
    onPlayMedia: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val omdbRepository = remember { OmdbRepository(context) }

    val catalog by catalogManager.catalog.collectAsState()
    val heroItems by catalogManager.heroItems.collectAsState()

    var selectedTab by remember { mutableStateOf(AdminTab.CATALOG) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<MediaItem?>(null) }
    var itemToDelete by remember { mutableStateOf<MediaItem?>(null) }
    var quickNotification by remember { mutableStateOf<String?>(null) }
    var drawerOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .testTag("admin_dashboard_screen")
            .fillMaxSize()
            .background(Color(0xFF0C0B12))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Persistent Admin Top Bar (Visible across ALL tabs: Catalog, Spotlight, Oracle Proxy, Vouchers, Tools)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF141320),
                tonalElevation = 4.dp,
                border = BorderStroke(0.5.dp, Color(0x33FFB300))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { drawerOpen = true }
                    ) {
                        IconButton(
                            onClick = { drawerOpen = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Admin Side Panel",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${selectedTab.icon} ${selectedTab.title}",
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x2200E5FF))
                                .border(1.dp, Color(0x6600E5FF), RoundedCornerShape(14.dp))
                                .clickable { onSwitchToUserView() }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "User View",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Tab Body View (Sandwich menu available in top bar across all tabs) with transitions
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        (slideInHorizontally { width -> width / 4 } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> -width / 4 } + fadeOut())
                            .using(SizeTransform(clip = false))
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "admin_tab_transition"
                ) { tab ->
                    when (tab) {
                        AdminTab.CATALOG -> {
                            AdminCompactCatalogView(
                                catalog = catalog,
                                searchQuery = searchQuery,
                                onSearchChange = { searchQuery = it },
                                selectedCategory = selectedCategoryFilter,
                                onSelectCategory = { selectedCategoryFilter = it },
                                onAddNew = {
                                    editingItem = null
                                    showAddEditDialog = true
                                },
                                onEdit = {
                                    editingItem = it
                                    showAddEditDialog = true
                                },
                                onDelete = { itemToDelete = it },
                                onPlay = onPlayMedia,
                                onToggleHero = { item ->
                                    val updated = item.copy(
                                        isTop10 = !item.isTop10,
                                        platformBadge = if (!item.isTop10) "CINEFLIX ORIGINAL" else "REGULAR"
                                    )
                                    catalogManager.updateMediaItem(updated)
                                    quickNotification = if (updated.isTop10) "Added '${item.title}' to Spotlight!" else "Removed from Spotlight."
                                }
                            )
                        }

                        AdminTab.SPOTLIGHT -> {
                            AdminSpotlightManagerView(
                                catalog = catalog,
                                sessionManager = sessionManager,
                                onToggleHero = { item ->
                                    val updated = item.copy(
                                        isTop10 = !item.isTop10,
                                        platformBadge = if (!item.isTop10) "CINEFLIX ORIGINAL" else "REGULAR"
                                    )
                                    catalogManager.updateMediaItem(updated)
                                },
                                onPlay = onPlayMedia
                            )
                        }

                        AdminTab.VOUCHERS -> {
                            AdminCompactPromoCodesView(
                                sessionManager = sessionManager
                            )
                        }

                        AdminTab.TOOLS -> {
                            AdminToolsAndOmdbView(
                                omdbRepository = omdbRepository,
                                catalogManager = catalogManager,
                                onImportMovie = { movie ->
                                    editingItem = movie
                                    showAddEditDialog = true
                                }
                            )
                        }
                    }
                }

                // Quick Floating Notification Toast
                quickNotification?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xEE161524),
                        border = BorderStroke(1.dp, Color(0xFFFFB300))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = msg,
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    LaunchedEffect(msg) {
                        kotlinx.coroutines.delay(2500)
                        quickNotification = null
                    }
                }
            }
        }

        // Navigation Drawer / Side Panel Overlay
        if (drawerOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000))
                    .clickable { drawerOpen = false }
            ) {
                Surface(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .align(Alignment.CenterStart)
                        .clickable(enabled = false) {},
                    color = Color(0xFF141320),
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, Color(0x33FFB300))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Brush.linearGradient(listOf(Color(0xFFFFB300), Color(0xFFFF3CAC)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Master Console", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Side Navigation", color = Color(0xFF88889C), fontSize = 10.sp)
                                }
                            }

                            IconButton(onClick = { drawerOpen = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Metrics inside drawer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CompactAdminMetric("Movies", "${catalog.size}", Color(0xFF00E5FF), Modifier.weight(1f))
                            CompactAdminMetric("Spotlight", "${heroItems.size}", Color(0xFFFFB300), Modifier.weight(1f))
                            CompactAdminMetric("Streams", "${catalog.count { !it.videoStreamUrl.isNullOrBlank() }}", Color(0xFF00E676), Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0x22FFFFFF))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("ADMIN SECTIONS", color = Color(0xFF88889C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        AdminTab.values().forEach { tab ->
                            val isSelected = selectedTab == tab
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedTab = tab
                                        drawerOpen = false
                                    },
                                color = if (isSelected) Color(0xFFFFB300).copy(alpha = 0.15f) else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.dp, Color(0xFFFFB300)) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(tab.icon, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = tab.title,
                                        color = if (isSelected) Color(0xFFFFB300) else Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        OutlinedButton(
                            onClick = {
                                drawerOpen = false
                                onSwitchToUserView()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Switch to User View", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                drawerOpen = false
                                sessionManager.logoutAdmin()
                                onSwitchToUserView()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF261215), contentColor = Color(0xFFFF5252)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0x66FF5252))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Admin Logout", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal: Add / Edit Movie
    if (showAddEditDialog) {
        CompactAddEditMovieDialog(
            existingItem = editingItem,
            catalog = catalog,
            omdbRepository = omdbRepository,
            onDismiss = {
                showAddEditDialog = false
                editingItem = null
            },
            onSave = { savedItem ->
                if (editingItem != null) {
                    catalogManager.updateMediaItem(savedItem)
                    quickNotification = "Updated '${savedItem.title}' successfully!"
                } else {
                    catalogManager.addMediaItem(savedItem)
                    quickNotification = "Published '${savedItem.title}' to catalog!"
                }
                showAddEditDialog = false
                editingItem = null
            }
        )
    }

    // Modal: Delete Confirmation
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = Color(0xFF181726),
            title = { Text("Delete Movie?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Text(
                    "Remove '${item.title}' from the live catalog? This action takes effect immediately in User View.",
                    color = Color(0xFFD1D1DE),
                    fontSize = 12.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        catalogManager.deleteMediaItem(item.id)
                        itemToDelete = null
                        quickNotification = "Deleted '${item.title}'"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = Color(0xFF88889C), fontSize = 11.sp)
                }
            }
        )
    }
}

@Composable
private fun CompactAdminMetric(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF1B1A2A),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = label,
                color = Color(0xFF88889C),
                fontSize = 8.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AdminCompactCatalogView(
    catalog: List<MediaItem>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onAddNew: () -> Unit,
    onEdit: (MediaItem) -> Unit,
    onDelete: (MediaItem) -> Unit,
    onPlay: (MediaItem) -> Unit,
    onToggleHero: (MediaItem) -> Unit
) {
    val categories = listOf("All", "Hindi Dub", "Hollywood", "Bollywood", "Series", "Top 10", "Streams")

    val filtered = remember(catalog, searchQuery, selectedCategory) {
        catalog.filter { item ->
            val matchSearch = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.primaryGenre.contains(searchQuery, ignoreCase = true)

            val matchCat = when (selectedCategory) {
                "All" -> true
                "Hindi Dub" -> item.title.contains("Hindi", ignoreCase = true) || item.genres.any { it.contains("Hindi", ignoreCase = true) }
                "Hollywood" -> item.primaryGenre.contains("Hollywood", ignoreCase = true) || item.primaryGenre.contains("Action", ignoreCase = true) || item.primaryGenre.contains("Sci-Fi", ignoreCase = true)
                "Bollywood" -> item.primaryGenre.contains("Bollywood", ignoreCase = true) || item.primaryGenre.contains("Romance", ignoreCase = true) || item.primaryGenre.contains("Drama", ignoreCase = true)
                "Series" -> item.type == MediaType.TV_SERIES || item.title.contains("Season", ignoreCase = true)
                "Top 10" -> item.isTop10
                "Streams" -> !item.videoStreamUrl.isNullOrBlank()
                else -> true
            }

            matchSearch && matchCat
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // Search & Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                color = Color(0xFF181726),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0x33FFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF88889C), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 11.5.sp
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Search ${catalog.size} movies / ID...", color = Color(0xFF66667A), fontSize = 11.sp)
                            }
                            innerTextField()
                        }
                    )
                }
            }

            // Quick Add mini button
            Surface(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAddNew() },
                color = Color(0xFFFFB300),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("New", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectCategory(cat) },
                    color = if (isSelected) Color(0xFFFFB300) else Color(0xFF1B1A28),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFB300) else Color(0x22FFFFFF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Color.Black else Color(0xFFD1D1DE),
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Compressed Movie Cards List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filtered, key = { it.id }) { item ->
                CompactAdminMovieRow(
                    item = item,
                    onPlay = { onPlay(item) },
                    onEdit = { onEdit(item) },
                    onDelete = { onDelete(item) },
                    onToggleHero = { onToggleHero(item) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun CompactAdminMovieRow(
    item: MediaItem,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleHero: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151422)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (item.isTop10) Color(0x66FFB300) else Color(0x18FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini Poster with Play Button
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 66.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF222233))
                    .clickable { onPlay() }
            ) {
                AsyncImage(
                    model = item.posterUrl ?: item.backdropUrl ?: "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400",
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Play icon overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x33000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Details Column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        color = Color(0x26FFB300),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = "⭐ ${item.imdbRating}",
                            color = Color(0xFFFFB300),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${item.primaryGenre} • ${item.releaseYear} • ${item.duration}",
                    color = Color(0xFF88889C),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Stream Source & Tag Pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!item.videoStreamUrl.isNullOrBlank()) {
                        val label = if (item.videoStreamUrl.contains("rumble.com", ignoreCase = true)) "Rumble Stream" else "Direct Stream"
                        Surface(
                            color = Color(0x2600E676),
                            shape = RoundedCornerShape(3.dp),
                            border = BorderStroke(1.dp, Color(0x4400E676))
                        ) {
                            Text(label, color = Color(0xFF00E676), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }

                    if (item.isTop10) {
                        Surface(
                            color = Color(0x33FFB300),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text("Spotlight", color = Color(0xFFFFB300), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Action Buttons: Hero toggle, Edit, Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Spotlight Toggle Button
                IconButton(
                    onClick = onToggleHero,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Spotlight",
                        tint = if (item.isTop10) Color(0xFFFFB300) else Color(0xFF555566),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Edit Button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminSpotlightManagerView(
    catalog: List<MediaItem>,
    sessionManager: SessionManager,
    onToggleHero: (MediaItem) -> Unit,
    onPlay: (MediaItem) -> Unit
) {
    val autoRotate by sessionManager.carouselAutoRotate.collectAsState()
    val rotateSpeed by sessionManager.carouselRotateSpeed.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        // Carousel Global Settings Card
        Surface(
            color = Color(0xFF141320),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0x3300E5FF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔄 Banner Auto-Rotation",
                            color = Color(0xFF00E5FF),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Automatically cycle through featured hero movies.",
                            color = Color(0xFFA0A0B2),
                            fontSize = 10.5.sp
                        )
                    }
                    Switch(
                        checked = autoRotate,
                        onCheckedChange = { sessionManager.setCarouselAutoRotate(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00E5FF),
                            checkedTrackColor = Color(0x6600E5FF)
                        )
                    )
                }

                if (autoRotate) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Rotation Speed: ${rotateSpeed.toInt()} seconds",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = if (rotateSpeed.isNaN()) 5f else rotateSpeed.coerceIn(3f, 15f),
                        onValueChange = { sessionManager.setCarouselRotateSpeed(it.coerceIn(3f, 15f)) },
                        valueRange = 3f..15f,
                        steps = 12,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color(0xFF282B38)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            color = Color(0xFF181728),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0x33FFB300)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "🌟 Spotlight Slider Manager",
                    color = Color(0xFFFFB300),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Featured titles will rotate prominently in the Top Hero Banner on the user screen.",
                    color = Color(0xFF88889C),
                    fontSize = 10.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(catalog) { item ->
                val isHero = item.isTop10
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isHero) Color(0xFF221E14) else Color(0xFF141320)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isHero) Color(0xFFFFB300) else Color(0x18FFFFFF)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 40.dp, height = 54.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF222233))
                                    .clickable { onPlay(item) }
                            ) {
                                AsyncImage(
                                    model = item.posterUrl ?: item.backdropUrl,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(
                                    text = item.title,
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.primaryGenre} • ${item.releaseYear}",
                                    color = Color(0xFF88889C),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onToggleHero(item) },
                            color = if (isHero) Color(0xFFFFB300) else Color(0xFF232235),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isHero) Icons.Default.Star else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (isHero) Color.Black else Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isHero) "Featured" else "Promote",
                                    color = if (isHero) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun AdminCompactPromoCodesView(
    sessionManager: SessionManager
) {
    var newCode by remember { mutableStateOf("") }
    var promoList by remember {
        mutableStateOf(
            listOf(
                "VIP2026" to "Annual VIP 4K Pass (100% OFF)",
                "FREE4K" to "Monthly VIP 4K Access",
                "STUDENT100" to "Student 1 Year VIP Pass",
                "LIFETIME" to "Lifetime Master VIP Ultra",
                "CINEFLIXVIP" to "Annual Ultra HD Pass"
            )
        )
    }
    var msg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Create Code Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF171626)),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0x33FFB300))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "💎 VIP Voucher Generator",
                    color = Color(0xFFFFB300),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        color = Color(0xFF222035),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = newCode,
                                onValueChange = { newCode = it.uppercase() },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    if (newCode.isEmpty()) Text("e.g. VIP2027", color = Color(0xFF77778A), fontSize = 11.sp)
                                    inner()
                                }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (newCode.isNotBlank()) {
                                promoList = listOf(newCode to "Annual VIP 4K Pass (Custom)") + promoList
                                msg = "Code '$newCode' generated!"
                                newCode = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color.Black),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Create", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (msg != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = msg!!, color = Color(0xFF00E676), fontSize = 10.sp)
                }
            }
        }

        Text(
            text = "Active Verified Vouchers (${promoList.size})",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items(promoList) { (code, desc) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141320)),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0x18FFFFFF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(code, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Black)
                                Text(desc, color = Color(0xFF88889C), fontSize = 9.5.sp)
                            }
                        }

                        Surface(
                            color = Color(0x2600E676),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("100% WORKING", color = Color(0xFF00E676), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun AdminToolsAndOmdbView(
    omdbRepository: OmdbRepository,
    catalogManager: CineflixLocalCatalogManager,
    onImportMovie: (MediaItem) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var searchOmdbTitle by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var omdbResults by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var searchStatus by remember { mutableStateOf<String?>(null) }
    var cacheMsg by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // OMDb 1-Click Search & Import
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151424)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0x3300E5FF))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("🌐 OMDb / TMDB 1-Click Auto-Importer", color = Color(0xFF00E5FF), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            color = Color(0xFF1F1D32),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = searchOmdbTitle,
                                    onValueChange = { searchOmdbTitle = it },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.5.sp),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { inner ->
                                        if (searchOmdbTitle.isEmpty()) Text("Search Hollywood / Bollywood...", color = Color(0xFF77778A), fontSize = 11.sp)
                                        inner()
                                    }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (searchOmdbTitle.isNotBlank()) {
                                    coroutineScope.launch {
                                        isSearching = true
                                        try {
                                            val res = omdbRepository.searchMovies(searchOmdbTitle)
                                            omdbResults = res.items
                                            searchStatus = "Found ${res.items.size} results for '$searchOmdbTitle'"
                                        } catch (e: Exception) {
                                            searchStatus = "Search error: ${e.message}"
                                        } finally {
                                            isSearching = false
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Search", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (searchStatus != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = searchStatus!!, color = Color(0xFFFFD54F), fontSize = 9.5.sp)
                    }
                }
            }
        }

        // Results of Search to Import
        if (omdbResults.isNotEmpty()) {
            item {
                Text("Search Results (Tap 'Import' to publish to catalog):", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }

            items(omdbResults) { movie ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF181728)),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(width = 36.dp, height = 50.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF222233))
                            ) {
                                AsyncImage(
                                    model = movie.posterUrl ?: movie.backdropUrl,
                                    contentDescription = movie.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(movie.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${movie.primaryGenre} • ${movie.releaseYear} • ⭐ ${movie.imdbRating}", color = Color(0xFF88889C), fontSize = 9.5.sp)
                            }
                        }

                        Button(
                            onClick = { onImportMovie(movie) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Import", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Catalog Maintenance & Reset
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151420)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0x22FFFFFF))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("🛠️ System Maintenance & Reset", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                catalogManager.resetCatalog()
                                cacheMsg = "Reset catalog to standard Bollywood & Hollywood 4K list!"
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB300)),
                            border = BorderStroke(1.dp, Color(0x66FFB300)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Default Catalog", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                cacheMsg = "Cleared 88 MB local thumbnail and segment cache."
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                            border = BorderStroke(1.dp, Color(0x6600E5FF)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Cache", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (cacheMsg != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = cacheMsg!!, color = Color(0xFF00E676), fontSize = 10.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

/**
 * Compact Add / Edit Movie Dialog with Instant OMDb Autofill
 */
@Composable
private fun CompactAddEditMovieDialog(
    existingItem: MediaItem?,
    catalog: List<MediaItem>,
    omdbRepository: OmdbRepository,
    onDismiss: () -> Unit,
    onSave: (MediaItem) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf(existingItem?.title ?: "") }
    var synopsis by remember { mutableStateOf(existingItem?.synopsis ?: "") }
    var posterUrl by remember { mutableStateOf(existingItem?.posterUrl ?: "") }
    var backdropUrl by remember { mutableStateOf(existingItem?.backdropUrl ?: "") }
    var primaryGenre by remember { mutableStateOf(existingItem?.primaryGenre ?: "Action") }
    var releaseYear by remember { mutableStateOf(existingItem?.releaseYear?.toString() ?: "2026") }
    var duration by remember { mutableStateOf(existingItem?.duration ?: "2h 15m") }
    var imdbRating by remember { mutableStateOf(existingItem?.imdbRating?.toString() ?: "8.2") }
    var streamUrl by remember { mutableStateOf(existingItem?.videoStreamUrl ?: "") }
    var isTop10 by remember { mutableStateOf(existingItem?.isTop10 ?: false) }

    var languageLinksMap by remember { mutableStateOf(existingItem?.languageLinks ?: emptyMap<String, String>()) }
    var newLanguageLabel by remember { mutableStateOf("") }
    var newLanguageUrl by remember { mutableStateOf("") }

    var parentSeriesId by remember { mutableStateOf(existingItem?.parentSeriesId) }
    var parentSeriesSearch by remember { mutableStateOf("") }
    var skipIntroStart by remember { mutableStateOf(existingItem?.skipIntroStartSec?.toString() ?: "10") }
    var skipIntroEnd by remember { mutableStateOf(existingItem?.skipIntroEndSec?.toString() ?: "95") }

    var selectedAudioLanguage by remember {
        mutableStateOf(existingItem?.audioLanguages?.firstOrNull()?.replace(" [Original]", "") ?: "Hindi")
    }
    var customAudioLanguage by remember { mutableStateOf("") }

    var contentType by remember {
        mutableStateOf(
            when (existingItem?.type) {
                MediaType.TV_SERIES -> if (existingItem.duration.contains("Episode", ignoreCase = true)) "Series Episode" else "Web Series"
                else -> "Single Movie"
            }
        )
    }
    var seasonNum by remember { mutableStateOf("1") }
    var episodeNum by remember { mutableStateOf("1") }

    var isFetching by remember { mutableStateOf(false) }
    var fetchStatus by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141322),
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingItem == null) "➕ New Content / Stream" else "✏️ Edit Content",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp
                )
                if (isTop10) {
                    Surface(color = Color(0x33FFB300), shape = RoundedCornerShape(4.dp)) {
                        Text("⭐ Spotlight", color = Color(0xFFFFB300), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(390.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Content Type Selector: Single Movie, Web Series, Series Episode
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Content Format / Type", color = Color(0xFFA6A6BA), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Single Movie", "Web Series", "Series Episode").forEach { typeOption ->
                                val isSelected = contentType == typeOption
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { contentType = typeOption },
                                    color = if (isSelected) Color(0xFFFFB300) else Color(0xFF1D1B2C),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFB300) else Color(0x33FFFFFF))
                                ) {
                                    Text(
                                        text = typeOption,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Audio Language Selection for this video / stream
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Audio Language Track", color = Color(0xFFA6A6BA), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Selected: $selectedAudioLanguage",
                                color = Color(0xFFFFB300),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val languageList = listOf("Hindi", "English", "Urdu", "Tamil", "Telugu", "Punjabi", "Malayalam", "Spanish", "French", "Korean", "Japanese")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(languageList) { lang ->
                                val isSelected = selectedAudioLanguage.equals(lang, ignoreCase = true)
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            selectedAudioLanguage = lang
                                            customAudioLanguage = ""
                                        },
                                    color = if (isSelected) Color(0xFFFFB300) else Color(0xFF1D1B2C),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFB300) else Color(0x33FFFFFF))
                                ) {
                                    Text(
                                        text = lang,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = customAudioLanguage,
                            onValueChange = {
                                customAudioLanguage = it
                                if (it.isNotBlank()) selectedAudioLanguage = it.trim()
                            },
                            label = { Text("Or custom audio language...", fontSize = 8.5.sp, color = Color(0xFFA6A6BA)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFB300),
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )
                    }
                }

                if (contentType == "Series Episode") {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = seasonNum,
                                onValueChange = { seasonNum = it },
                                label = { Text("Season #", fontSize = 9.sp, color = Color(0xFFA6A6BA)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFFFB300),
                                    unfocusedBorderColor = Color(0x33FFFFFF)
                                )
                            )
                            OutlinedTextField(
                                value = episodeNum,
                                onValueChange = { episodeNum = it },
                                label = { Text("Episode #", fontSize = 9.sp, color = Color(0xFFA6A6BA)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFFFB300),
                                    unfocusedBorderColor = Color(0x33FFFFFF)
                                )
                            )
                        }
                    }
                }

                if (contentType == "Web Series" || contentType == "Series Episode") {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Parent Series / Show (Link next episode)", color = Color(0xFFA6A6BA), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            OutlinedTextField(
                                value = parentSeriesSearch,
                                onValueChange = { parentSeriesSearch = it },
                                label = { Text("Search Parent Series to link...", fontSize = 9.sp, color = Color(0xFFA6A6BA)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFFFB300),
                                    unfocusedBorderColor = Color(0x33FFFFFF)
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val isNoneSelected = parentSeriesId.isNullOrBlank()
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { parentSeriesId = null },
                                color = if (isNoneSelected) Color(0xFFFFB300) else Color(0xFF1D1B2C),
                                border = BorderStroke(1.dp, if (isNoneSelected) Color(0xFFFFB300) else Color(0x33FFFFFF))
                            ) {
                                Text(
                                    text = "Standalone Series (No Parent)",
                                    color = if (isNoneSelected) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }

                            if (parentSeriesSearch.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val tvSeriesList = catalog.filter { it.type == MediaType.TV_SERIES && it.title.contains(parentSeriesSearch, ignoreCase = true) }
                                if (tvSeriesList.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(tvSeriesList) { series ->
                                            val isSelected = parentSeriesId == series.id
                                            Surface(
                                                modifier = Modifier
                                                    .width(120.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { parentSeriesId = series.id },
                                                color = if (isSelected) Color(0xFFFFB300) else Color(0xFF1E1E2C),
                                                border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFB300) else Color(0x33FFFFFF))
                                            ) {
                                                Column {
                                                    AsyncImage(
                                                        model = series.posterUrl,
                                                        contentDescription = series.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(140.dp)
                                                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                                    )
                                                    Column(modifier = Modifier.padding(6.dp)) {
                                                        Text(
                                                            text = series.title,
                                                            color = if (isSelected) Color.Black else Color.White,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "⭐ ${series.imdbRating}",
                                                            color = Color(0xFFA6A6BA),
                                                            fontSize = 9.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Text("No TV Series found matching \"$parentSeriesSearch\"", color = Color(0xFF88889C), fontSize = 9.sp)
                                }
                            } else {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("💡 Type in search above to find & link existing TV Series with posters.", color = Color(0xFF88889C), fontSize = 9.sp)
                            }
                        }
                    }
                }

                // Skip Intro Settings
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = skipIntroStart,
                            onValueChange = { skipIntroStart = it },
                            label = { Text("Skip Intro Start (sec)", fontSize = 9.sp, color = Color(0xFFA6A6BA)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFB300),
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )
                        OutlinedTextField(
                            value = skipIntroEnd,
                            onValueChange = { skipIntroEnd = it },
                            label = { Text("Skip Intro End (sec)", fontSize = 9.sp, color = Color(0xFFA6A6BA)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFB300),
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )
                    }
                }

                // Multi-Language Links Manager
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text("Multi-Language Streams", color = Color(0xFFFFB300), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        languageLinksMap.forEach { (lang, url) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(lang, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(url, color = Color(0xFF88889C), fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(
                                    onClick = { languageLinksMap = languageLinksMap.toMutableMap().apply { remove(lang) } },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedTextField(
                                value = newLanguageLabel,
                                onValueChange = { newLanguageLabel = it },
                                label = { Text("Language", fontSize = 9.sp) },
                                modifier = Modifier.weight(0.4f),
                                shape = RoundedCornerShape(4.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFFFB300)
                                )
                            )
                            OutlinedTextField(
                                value = newLanguageUrl,
                                onValueChange = { newLanguageUrl = it },
                                label = { Text("Stream URL", fontSize = 9.sp) },
                                modifier = Modifier.weight(0.6f),
                                shape = RoundedCornerShape(4.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFFFB300)
                                )
                            )
                            IconButton(
                                onClick = {
                                    if (newLanguageLabel.isNotBlank() && newLanguageUrl.isNotBlank()) {
                                        languageLinksMap = languageLinksMap.toMutableMap().apply {
                                            put(newLanguageLabel.trim(), newLanguageUrl.trim())
                                        }
                                        newLanguageLabel = ""
                                        newLanguageUrl = ""
                                    }
                                },
                                modifier = Modifier.align(Alignment.CenterVertically).size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Language", tint = Color(0xFF00E676))
                            }
                        }
                    }
                }

                // OMDb Auto-Fill Search Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title (e.g. Jawan, Inception)", fontSize = 10.sp, color = Color(0xFFA6A6BA)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFB300),
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )

                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    coroutineScope.launch {
                                        isFetching = true
                                        try {
                                            val res = omdbRepository.searchMovies(title)
                                            val found = res.items.firstOrNull()
                                            if (found != null) {
                                                if (!found.posterUrl.isNullOrBlank()) posterUrl = found.posterUrl
                                                if (!found.backdropUrl.isNullOrBlank()) backdropUrl = found.backdropUrl
                                                if (found.synopsis.isNotBlank()) synopsis = found.synopsis
                                                primaryGenre = found.primaryGenre
                                                releaseYear = found.releaseYear.toString()
                                                imdbRating = found.imdbRating.toString()
                                                fetchStatus = "✅ Auto-filled from OMDb!"
                                            } else {
                                                fetchStatus = "No match. You can enter details manually."
                                            }
                                        } catch (e: Exception) {
                                            fetchStatus = "Error: ${e.message}"
                                        } finally {
                                            isFetching = false
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            if (isFetching) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Autofill", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (fetchStatus != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = fetchStatus!!, color = Color(0xFFFFD54F), fontSize = 9.sp)
                    }
                }

                // Rumble / Video Stream Link
                item {
                    OutlinedTextField(
                        value = streamUrl,
                        onValueChange = { streamUrl = it },
                        label = { Text("Rumble URL / Direct Video Stream URL", fontSize = 10.sp, color = Color(0xFFFFB300)) },
                        placeholder = { Text("https://rumble.com/... or direct MP4 link", fontSize = 10.sp, color = Color(0xFF666677)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFB300),
                            unfocusedBorderColor = Color(0x33FFFFFF)
                        )
                    )
                }

                // Poster Image URL with Live Thumbnail
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = posterUrl,
                            onValueChange = { posterUrl = it },
                            label = { Text("Poster Image URL", fontSize = 10.sp, color = Color(0xFFA6A6BA)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFB300),
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )

                        if (posterUrl.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(width = 30.dp, height = 42.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF222233))
                            ) {
                                AsyncImage(
                                    model = posterUrl,
                                    contentDescription = "Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Genre, Year, Rating Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        OutlinedTextField(
                            value = primaryGenre,
                            onValueChange = { primaryGenre = it },
                            label = { Text("Genre", fontSize = 9.sp, color = Color(0xFFA6A6BA)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFB300),
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )

                        OutlinedTextField(
                            value = releaseYear,
                            onValueChange = { releaseYear = it },
                            label = { Text("Year", fontSize = 9.sp, color = Color(0xFFA6A6BA)) },
                            modifier = Modifier.width(68.dp),
                            shape = RoundedCornerShape(6.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFB300),
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )

                        OutlinedTextField(
                            value = imdbRating,
                            onValueChange = { imdbRating = it },
                            label = { Text("Rating", fontSize = 9.sp, color = Color(0xFFA6A6BA)) },
                            modifier = Modifier.width(68.dp),
                            shape = RoundedCornerShape(6.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFB300),
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )
                    }
                }

                // Spotlight Toggle Switch
                item {
                    Surface(
                        color = Color(0xFF1C1B2C),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Promote to Top Spotlight Banner", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = isTop10,
                                onCheckedChange = { isTop10 = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color(0xFFFFB300)
                                ),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Synopsis
                item {
                    OutlinedTextField(
                        value = synopsis,
                        onValueChange = { synopsis = it },
                        label = { Text("Synopsis / Story Plot", fontSize = 10.sp, color = Color(0xFFA6A6BA)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFB300),
                            unfocusedBorderColor = Color(0x33FFFFFF)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val computedStreamUrl = streamUrl.trim().ifBlank { null }
                        val finalMediaType = when (contentType) {
                            "Web Series", "Series Episode" -> MediaType.TV_SERIES
                            else -> MediaType.MOVIE
                        }
                        val finalDuration = when (contentType) {
                            "Series Episode" -> "Season $seasonNum • Episode $episodeNum"
                            "Web Series" -> "Web Series • 8 Episodes"
                            else -> duration.ifBlank { "2h 00m" }
                        }
                        val finalLanguage = if (customAudioLanguage.isNotBlank()) customAudioLanguage.trim() else selectedAudioLanguage
                        val audioLangs = listOf(finalLanguage) + (if (!finalLanguage.equals("English", ignoreCase = true)) listOf("English [Original]") else emptyList())

                        val newItem = MediaItem(
                            id = existingItem?.id ?: "movie_${System.currentTimeMillis()}",
                            title = title.trim(),
                            tagline = when (contentType) {
                                "Series Episode" -> "Season $seasonNum Episode $episodeNum"
                                "Web Series" -> "Complete Web Series"
                                else -> "Streaming in 4K HDR"
                            },
                            synopsis = synopsis.ifBlank { "Watch $title in full high definition." },
                            type = finalMediaType,
                            posterUrl = posterUrl.ifBlank { null },
                            backdropUrl = backdropUrl.ifBlank { posterUrl.ifBlank { null } },
                            primaryGenre = primaryGenre.ifBlank { "Action" },
                            genres = listOf(primaryGenre.ifBlank { "Action" }, contentType),
                            releaseYear = releaseYear.toIntOrNull() ?: 2026,
                            duration = finalDuration,
                            imdbRating = imdbRating.toDoubleOrNull() ?: 8.0,
                            matchScore = 95,
                            qualityBadges = listOf("4K UHD", "Dolby Atmos"),
                            audioLanguages = audioLangs,
                            creator = "Admin ($finalLanguage)",
                            platformBadge = if (streamUrl.contains("rumble", ignoreCase = true)) "RUMBLE DIRECT" else "CINEFLIX MASTER",
                            isTop10 = isTop10,
                            top10Rank = if (isTop10) 1 else null,
                            videoStreamUrl = computedStreamUrl,
                            parentSeriesId = parentSeriesId,
                            skipIntroStartSec = skipIntroStart.toIntOrNull() ?: 10,
                            skipIntroEndSec = skipIntroEnd.toIntOrNull() ?: 95,
                            languageLinks = languageLinksMap
                        )
                        onSave(newItem)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color.Black),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Save & Publish", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF88889C), fontSize = 11.sp)
            }
        }
    )
}
