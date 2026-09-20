package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.auth.SessionManager
import com.example.model.MediaItem
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import java.io.File

data class DownloadRecord(
    val id: String,
    val mediaItem: MediaItem,
    val fileSize: String,
    val resolution: String,
    val isComplete: Boolean = true,
    var progress: Float = 1.0f,
    val speed: String? = null
)

data class WatchHistoryRecord(
    val id: String,
    val mediaItem: MediaItem,
    val watchedTime: String,
    val totalTime: String,
    val progressPercent: Float,
    val lastWatchedDate: String
)

@Composable
fun DownloadsAndHistoryScreen(
    sessionManager: SessionManager,
    allCatalogMovies: List<MediaItem>,
    onPlayMedia: (MediaItem) -> Unit,
    onBrowseMore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Downloads, 1 = History
    val context = LocalContext.current

    // Real-time Storage calculation using StatFs
    var totalBytes by remember { mutableLongStateOf(0L) }
    var availableBytes by remember { mutableLongStateOf(0L) }
    var usedBytes by remember { mutableLongStateOf(0L) }
    var storageProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        totalBytes = totalBlocks * blockSize
        availableBytes = availableBlocks * blockSize
        usedBytes = totalBytes - availableBytes
        storageProgress = if (totalBytes > 0) usedBytes.toFloat() / totalBytes.toFloat() else 0f
    }

    // Real-time reactive downloads and history synced with mobile SharedPreferences
    val downloadedIds by sessionManager.downloadedIds.collectAsStateWithLifecycle()
    val activeDownloads by sessionManager.activeDownloads.collectAsStateWithLifecycle()
    val historyEntries by sessionManager.historyEntries.collectAsStateWithLifecycle()

    val downloadedItems = remember(downloadedIds, activeDownloads, allCatalogMovies) {
        val completed = downloadedIds.mapNotNull { id ->
            allCatalogMovies.find { it.id == id }?.let { media ->
                DownloadRecord(
                    id = "dl_${media.id}",
                    mediaItem = media,
                    fileSize = if (media.duration.contains("h")) "1.8 GB" else "950 MB",
                    resolution = if (media.qualityBadges.contains("4K UHD")) "4K UHD" else "1080p FHD",
                    isComplete = true,
                    progress = 1.0f
                )
            }
        }
        val active = activeDownloads.mapNotNull { (id, progress) ->
            allCatalogMovies.find { it.id == id }?.let { media ->
                DownloadRecord(
                    id = "dl_active_${media.id}",
                    mediaItem = media,
                    fileSize = if (media.duration.contains("h")) "1.8 GB" else "950 MB",
                    resolution = if (media.qualityBadges.contains("4K UHD")) "4K UHD" else "1080p FHD",
                    isComplete = false,
                    progress = progress,
                    speed = "12.4 MB/s"
                )
            }
        }
        active + completed
    }

    val historyItems = remember(historyEntries, allCatalogMovies) {
        historyEntries.mapNotNull { entry ->
            allCatalogMovies.find { it.id == entry.mediaId }?.let { media ->
                WatchHistoryRecord(
                    id = "hist_${media.id}",
                    mediaItem = media,
                    watchedTime = entry.watchedTime,
                    totalTime = entry.totalTime,
                    progressPercent = entry.progressPercent,
                    lastWatchedDate = "Recent"
                )
            }
        }
    }

    Column(
        modifier = modifier
            .testTag("downloads_history_screen")
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF131A26), // Subtle cool dark slate glow
                        Color(0xFF10121A),
                        Color(0xFF0C0D12)
                    )
                )
            )
    ) {
        // Sticky Header with status bar padding
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xF5141622),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Downloads & History",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (selectedTab == 1 && historyItems.isNotEmpty()) {
                        Text(
                            text = "Clear All",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { sessionManager.clearWatchHistory() }
                                .padding(4.dp)
                        )
                    } else if (selectedTab == 0 && downloadedItems.isNotEmpty()) {
                        Text(
                            text = "Clear All",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { sessionManager.clearDownloads() }
                                .padding(4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Segmented Tab Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E202F))
                        .padding(4.dp)
                ) {
                    // Downloads Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 0) Color(0xFF00E5FF) else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = if (selectedTab == 0) Color(0xFF00222B) else Color(0xFFA6A6BA),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Downloads (${downloadedItems.size})",
                                color = if (selectedTab == 0) Color(0xFF00222B) else Color(0xFFA6A6BA),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Watch History Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 1) Color(0xFF00E5FF) else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = if (selectedTab == 1) Color(0xFF00222B) else Color(0xFFA6A6BA),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "History (${historyItems.size})",
                                color = if (selectedTab == 1) Color(0xFF00222B) else Color(0xFFA6A6BA),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Body Content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (selectedTab == 0) {
                // 1. Device Storage Information Card with Real-time StatFs Data
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141622)),
                        border = BorderStroke(1.dp, Color(0x22FFFFFF))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = "Storage",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Internal Device Storage",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${Formatter.formatShortFileSize(context, usedBytes)} used / ${Formatter.formatShortFileSize(context, totalBytes)}",
                                    color = Color(0xFFA0A0B2),
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Storage Slider (Indicator only, showing usage)
                            Slider(
                                value = if (storageProgress.isNaN()) 0f else storageProgress.coerceIn(0f, 1f),
                                onValueChange = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth().height(12.dp),
                                colors = SliderDefaults.colors(
                                    disabledThumbColor = Color.Transparent,
                                    disabledActiveTrackColor = Color(0xFF00E5FF),
                                    disabledInactiveTrackColor = Color(0xFF282B38)
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "💡 Local encryption active for offline access",
                                    color = Color(0xFF88889C),
                                    fontSize = 10.5.sp
                                )
                                Text(
                                    text = "${Formatter.formatShortFileSize(context, availableBytes)} Free",
                                    color = Color(0xFF00E676),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 2. Downloaded Items List
                if (downloadedItems.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = Color(0xFF6E6E82),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No downloaded movies",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Content you download for offline viewing will appear here.",
                                color = Color(0xFF88889C),
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    items(downloadedItems, key = { it.id }) { record ->
                        DownloadedMediaCard(
                            record = record,
                            onPlay = { onPlayMedia(record.mediaItem) },
                            onDelete = { sessionManager.removeDownload(record.mediaItem.id) }
                        )
                    }
                }

                // 3. Download More Button
                item {
                    OutlinedButton(
                        onClick = onBrowseMore,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browse & Download More Content", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                // WATCH HISTORY TAB
                if (historyItems.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 50.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = Color(0xFF6E6E82),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Your Watch History is clean",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Start watching movies from Home or Trending to track progress.",
                                color = Color(0xFF88889C),
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    items(historyItems, key = { it.id }) { record ->
                        WatchHistoryCard(
                            record = record,
                            onResume = { onPlayMedia(record.mediaItem) },
                            onRemove = { sessionManager.removeWatchHistory(record.mediaItem.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadedMediaCard(
    record: DownloadRecord,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171824)),
        border = BorderStroke(1.dp, Color(0x1AFFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster thumbnail
            Box(
                modifier = Modifier
                    .size(width = 70.dp, height = 96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF222230))
            ) {
                AsyncImage(
                    model = record.mediaItem.posterUrl ?: record.mediaItem.backdropUrl,
                    contentDescription = record.mediaItem.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (record.isComplete) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.mediaItem.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0x3300E5FF),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = record.resolution,
                            color = Color(0xFF00E5FF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = record.fileSize,
                        color = Color(0xFFA6A6BA),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (!record.isComplete) {
                    // Downloading state
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Downloading... ${(record.progress * 100).toInt()}%",
                                color = Color(0xFFFFD54F),
                                fontSize = 10.5.sp
                            )
                            Text(
                                text = record.speed ?: "12 MB/s",
                                color = Color(0xFFA6A6BA),
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { record.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFFFFD54F),
                            trackColor = Color(0xFF282836)
                        )
                    }
                } else {
                    // Play Offline button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF00E676))
                            .clickable { onPlay() }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Play Offline",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete action button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFF5252))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun WatchHistoryCard(
    record: WatchHistoryRecord,
    onResume: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171824)),
        border = BorderStroke(1.dp, Color(0x1AFFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster
            Box(
                modifier = Modifier
                    .size(width = 70.dp, height = 96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF222230))
            ) {
                AsyncImage(
                    model = record.mediaItem.posterUrl ?: record.mediaItem.backdropUrl,
                    contentDescription = record.mediaItem.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Progress overlay at bottom of poster
                LinearProgressIndicator(
                    progress = { record.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(4.dp),
                    color = Color(0xFF00E676),
                    trackColor = Color(0x88000000)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info & Resume
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.mediaItem.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Watched ${record.watchedTime} of ${record.totalTime}",
                    color = Color(0xFFA6A6BA),
                    fontSize = 11.sp
                )
                Text(
                    text = record.lastWatchedDate,
                    color = Color(0xFF88889C),
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF00E5FF))
                        .clickable { onResume() }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color(0xFF00222B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (record.progressPercent >= 0.95f) "Watch Again" else "Resume",
                        color = Color(0xFF00222B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Remove button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x16FFFFFF))
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove from history",
                    tint = Color(0xFFA6A6BA),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
