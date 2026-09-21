package com.example.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import com.example.omdb.OmdbRepository
import com.example.omdb.MovieComment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import com.example.auth.SessionManager
import com.example.streaming.findActivity
import com.example.model.MediaItem
import com.example.model.StreamingTier
import com.example.streaming.CineflixExoPlayerView
import com.example.ui.theme.CineflixTheme

/**
 * CINEFLIX Modal Dialogs
 * 1. Trailer Player Modal (Interactive video preview with screenshot-exact OTT detail layout)
 * 2. Subscription Plans Modal (Mobile, Standard, Ultra 4K tiers)
 * 3. Playback & App Settings Modal
 */

@Composable
fun TrailerPlayerModal(
    media: MediaItem,
    catalogMovies: List<MediaItem> = emptyList(),
    sessionManager: SessionManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var currentMedia by remember { mutableStateOf(media) }
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("Hindi dub") }
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("For you") } // "For you" vs "Comments"
    var isAddedToList by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var showToastMessage by remember { mutableStateOf<String?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var lastToastTime by remember { mutableLongStateOf(0L) }
    var showUpNext by remember { mutableStateOf(false) }
    var nextEpisode by remember { mutableStateOf<MediaItem?>(null) }

    // Auto-hide toast after 3 seconds
    LaunchedEffect(showToastMessage) {
        if (showToastMessage != null) {
            delay(3000)
            showToastMessage = null
        }
    }

    // Reset orientation to portrait / default when modal is dismissed
    DisposableEffect(Unit) {
        onDispose {
            context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Download Simulation Logic
    val activeDownloads by sessionManager.activeDownloads.collectAsStateWithLifecycle()
    LaunchedEffect(activeDownloads) {
        activeDownloads.forEach { (id, currentProgress) ->
            if (currentProgress < 1.0f) {
                delay(800)
                val next = (currentProgress + 0.15f).coerceAtMost(1.0f)
                if (next >= 1.0f) {
                    sessionManager.completeDownload(id)
                    showToastMessage = "Download Complete: ${media.title}"
                } else {
                    sessionManager.updateDownloadProgress(id, next)
                }
            }
        }
        }

    // ── Next Episode Helper ─────────────────────────────────────────────
    fun findNextEpisode(current: MediaItem, catalog: List<MediaItem>): MediaItem? {
        if (current.parentSeriesId.isNullOrBlank()) return null
        val seriesId = current.parentSeriesId!
        val sameSeries = catalog.filter {
            (it.parentSeriesId ?: "") == seriesId && it.id != current.id
        }
        val nextInSeason = sameSeries
            .filter { it.seasonNumber == current.seasonNumber && it.episodeNumber == current.episodeNumber + 1 }
            .sortedBy { it.episodeNumber }
            .firstOrNull()
        if (nextInSeason != null) return nextInSeason
        val nextSeason = sameSeries
            .filter { it.seasonNumber == current.seasonNumber + 1 && it.episodeNumber == 1 }
            .sortedBy { it.episodeNumber }
            .firstOrNull()
        return nextSeason
    }

    // Auto-advance when current episode ends
    LaunchedEffect(showUpNext) {
        if (!showUpNext) return@LaunchedEffect
        delay(3000)
        val next = nextEpisode
        if (next != null) {
            currentMedia = next
            isPlaying = true
            selectedLanguageUrl = null
            showUpNext = false
            sessionManager.addWatchHistory(
                mediaId = next.id,
                watched = "0m",
                total = next.duration,
                progress = 0f
            )
        }
    }

    // Intercept back button when in landscape mode to return to portrait first
    BackHandler(enabled = isLandscape) {
        context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    // Clean title
    val displayTitle = remember(currentMedia) {
        currentMedia.title.trim()
    }

    val coroutineScope = rememberCoroutineScope()
    val omdbRepository = remember { OmdbRepository(context) }
    val omdbComments by omdbRepository.movieComments.collectAsStateWithLifecycle()

    LaunchedEffect(currentMedia.id) {
        omdbRepository.fetchCommentsForMovie(currentMedia.title)
    }

    // Resolve stream URL
    var selectedLanguageUrl by remember { mutableStateOf<String?>(null) }
    
    val streamUrl = remember(currentMedia, selectedLanguageUrl) {
        selectedLanguageUrl ?: currentMedia.videoStreamUrl
    }

    // Dynamic recommendation list for "For you" section that automatically adapts to the currently playing video
    val forYouList = remember(currentMedia, catalogMovies) {
        val currentGenres = currentMedia.genres
        val currentPrimaryGenre = currentMedia.primaryGenre

        val ranked = catalogMovies
            .filter { it.id != currentMedia.id && !it.title.equals(currentMedia.title, ignoreCase = true) }
            .map { item ->
                var score = 0
                if (item.type == currentMedia.type) score += 5
                if (item.primaryGenre.equals(currentPrimaryGenre, ignoreCase = true)) score += 4
                score += item.genres.count { g -> currentGenres.any { it.equals(g, ignoreCase = true) } } * 3
                if (item.audioLanguages.any { it.contains("Hindi", ignoreCase = true) }) score += 2
                Pair(item, score)
            }
            .sortedByDescending { it.second }
            .map { it.first }

        if (ranked.isNotEmpty()) {
            ranked.take(9)
        } else {
            listOf(
                MediaItem(
                    id = "rec_${currentMedia.id.hashCode()}_1",
                    title = if (currentMedia.type == com.example.model.MediaType.TV_SERIES) "Stranger Things 5" else "Moana 2",
                    tagline = "Experience the adventure",
                    synopsis = "A journey into the unknown.",
                    primaryGenre = currentMedia.primaryGenre,
                    genres = currentMedia.genres,
                    releaseYear = 2024,
                    duration = "2h 15m",
                    posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=400&q=80",
                    videoStreamUrl = "https://rumble.com/v7fpkry-gtrgtrgrtg.html"
                ),
                MediaItem(
                    id = "rec_${currentMedia.id.hashCode()}_2",
                    title = if (currentMedia.type == com.example.model.MediaType.TV_SERIES) "Mirzapur S3" else "Devara Part 1",
                    tagline = "The epic saga continues",
                    synopsis = "A high-stakes drama unfold.",
                    primaryGenre = currentMedia.primaryGenre,
                    genres = currentMedia.genres,
                    releaseYear = 2024,
                    duration = "2h 20m",
                    posterUrl = "https://images.unsplash.com/photo-1574375927938-d5a98e8ffe85?auto=format&fit=crop&w=400&q=80",
                    videoStreamUrl = "https://rumble.com/v7fpkry-gtrgtrgrtg.html"
                ),
                MediaItem(
                    id = "rec_${currentMedia.id.hashCode()}_3",
                    title = if (currentMedia.type == com.example.model.MediaType.TV_SERIES) "The Boys S4" else "Dhamaal 4",
                    tagline = "Unstoppable madness and action",
                    synopsis = "Laughter and non-stop thrill.",
                    primaryGenre = currentMedia.primaryGenre,
                    genres = currentMedia.genres,
                    releaseYear = 2024,
                    duration = "2h 05m",
                    posterUrl = "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?auto=format&fit=crop&w=400&q=80",
                    videoStreamUrl = "https://rumble.com/v7fpkry-gtrgtrgrtg.html"
                ),
                MediaItem(
                    id = "rec_${currentMedia.id.hashCode()}_4",
                    title = "Inception",
                    tagline = "Your mind is the scene of the crime.",
                    synopsis = "A thief who steals corporate secrets through dream-sharing technology.",
                    primaryGenre = "Sci-Fi",
                    genres = listOf("Sci-Fi", "Action", "Thriller"),
                    releaseYear = 2010,
                    duration = "2h 28m",
                    posterUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=400&q=80",
                    videoStreamUrl = "https://rumble.com/v7fpkry-gtrgtrgrtg.html"
                ),
                MediaItem(
                    id = "rec_${currentMedia.id.hashCode()}_5",
                    title = "Interstellar",
                    tagline = "Mankind was born on Earth. It was never meant to die here.",
                    synopsis = "A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival.",
                    primaryGenre = "Sci-Fi",
                    genres = listOf("Sci-Fi", "Adventure", "Drama"),
                    releaseYear = 2014,
                    duration = "2h 49m",
                    posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=400&q=80",
                    videoStreamUrl = "https://rumble.com/v7fpkry-gtrgtrgrtg.html"
                ),
                MediaItem(
                    id = "rec_${currentMedia.id.hashCode()}_6",
                    title = "RRR: Rise Roar Revolt",
                    tagline = "Fire and Water come together.",
                    synopsis = "A tale of two legendary revolutionaries and their journey away from home.",
                    primaryGenre = "Action",
                    genres = listOf("Action", "Drama"),
                    releaseYear = 2022,
                    duration = "3h 07m",
                    posterUrl = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=400&q=80",
                    videoStreamUrl = "https://rumble.com/v7fpkry-gtrgtrgrtg.html"
                )
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .testTag("trailer_player_modal")
                .fillMaxSize()
                .background(Color(0xFF0F0E17))
        ) {
            if (isLandscape) {
                // Immersive Fullscreen Landscape Mode
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    CineflixExoPlayerView(
                        streamUrl = streamUrl ?: "",
                        isPlaying = isPlaying,
                        isMuted = isMuted,
                        title = displayTitle,
                        skipIntroStartSec = currentMedia.skipIntroStartSec,
                        skipIntroEndSec = currentMedia.skipIntroEndSec,
                        modifier = Modifier.fillMaxSize(),
                        onPlayPauseToggle = { isPlaying = it },
                        onCloseClick = {
                            context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        },
                        onError = { err ->
                            showToastMessage = "Stream error: $err"
                        },
                        onEnded = {
                            val next = findNextEpisode(currentMedia, catalogMovies)
                            if (next != null) {
                                nextEpisode = next
                                showUpNext = true
                            } else {
                                showToastMessage = "Series completed"
                            }
                        }
                    )
                }
            } else {
                // Portrait Layout with Top 16:9 Viewport & Details
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. Single Unified Video Player Viewport (16:9) with Built-in OTT HUD Controls
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                    ) {
                        CineflixExoPlayerView(
                            streamUrl = streamUrl ?: "",
                            isPlaying = isPlaying,
                            isMuted = isMuted,
                            title = displayTitle,
                            skipIntroStartSec = currentMedia.skipIntroStartSec,
                            skipIntroEndSec = currentMedia.skipIntroEndSec,
                            modifier = Modifier.fillMaxSize(),
                            onPlayPauseToggle = { isPlaying = it },
                            onCloseClick = onDismiss,
                            onError = { err ->
                                showToastMessage = "Stream error: $err (Connecting Oracle Cloud Proxy fallback)"
                            },
                            onEnded = {
                                val next = findNextEpisode(currentMedia, catalogMovies)
                                if (next != null) {
                                    nextEpisode = next
                                    showUpNext = true
                                } else {
                                    showToastMessage = "Series completed"
                                }
                            }
                        )
                    }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Media Title & Metadata Row (Clean single title)
                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showInfoDialog = true }
                    ) {
                        Text(
                            text = displayTitle,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Info >",
                            color = Color(0xFFA6A6BA),
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Global",
                            tint = Color(0xFFA6A6BA),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(text = "|", color = Color(0x55FFFFFF), fontSize = 12.sp)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "%.1f".format(currentMedia.imdbRating),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(text = "|", color = Color(0x55FFFFFF), fontSize = 12.sp)
                        Text(text = "${currentMedia.releaseYear}", color = Color(0xFFA6A6BA), fontSize = 12.sp)
                        Text(text = "|", color = Color(0x55FFFFFF), fontSize = 12.sp)
                        Text(text = "India", color = Color(0xFFA6A6BA), fontSize = 12.sp)
                        Text(text = "|", color = Color(0x55FFFFFF), fontSize = 12.sp)
                        Text(
                            text = currentMedia.primaryGenre.ifBlank { currentMedia.genres.firstOrNull() ?: "Drama" },
                            color = Color(0xFFA6A6BA),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Action Buttons Row (Add to list, Share, Download, VIP)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Add to list
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val newState = sessionManager?.toggleWatchlist(currentMedia.id) ?: !isAddedToList
                                isAddedToList = newState
                                showToastMessage = if (isAddedToList) "Added to Watchlist ✓" else "Removed from Watchlist"
                            },
                        color = Color(0xFF201F2B),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isAddedToList) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = "Add",
                                tint = if (isAddedToList) Color(0xFF00E676) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAddedToList) "Added" else "Add to list",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Share Button (Real Android Share Intent)
                    Surface(
                        modifier = Modifier
                            .weight(0.9f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                try {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, "Stream $displayTitle on CINEFLIX: $streamUrl")
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share Movie")
                                    context.startActivity(shareIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Sharing failed, link copied!", Toast.LENGTH_SHORT).show()
                                }
                            },
                        color = Color(0xFF201F2B),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Share",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Golden Download Pill Button
                    Surface(
                        modifier = Modifier
                            .weight(1.15f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                isDownloading = true
                                sessionManager?.addDownload(currentMedia.id)
                                showToastMessage = "Downloading $displayTitle (1080p MP4)..."
                            },
                        color = Color(0xFF42300D), // Gold highlighted container
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFD54F))
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Download",
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isDownloading) "Downloading..." else "Download",
                                color = Color(0xFFFFD54F),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Multi-Language Selection Section
                if (currentMedia.languageLinks.isNotEmpty()) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                        Text(
                            text = "Switch Language",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Default track
                            LanguageChip(
                                label = "Original",
                                isSelected = selectedLanguageUrl == null,
                                onClick = { selectedLanguageUrl = null }
                            )
                            currentMedia.languageLinks.forEach { (lang, url) ->
                                LanguageChip(
                                    label = lang,
                                    isSelected = selectedLanguageUrl == url,
                                    onClick = {
                                        selectedLanguageUrl = url
                                        showToastMessage = "Switched to $lang track"
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // 5. Resources Section
                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Information",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Powered by Cineflix Cloud ⚡",
                                color = Color(0xFFA6A6BA),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Language Selector Dropdown Chip
                    Box {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showLanguageDropdown = true },
                            color = Color(0xFF201F2B),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedLanguage,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Dropdown",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showLanguageDropdown,
                            onDismissRequest = { showLanguageDropdown = false },
                            modifier = Modifier.background(Color(0xFF201F2B))
                        ) {
                            listOf("Hindi dub", "English [Original]", "Tamil dub", "Telugu dub").forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang, color = Color.White) },
                                    onClick = {
                                        selectedLanguage = lang
                                        showLanguageDropdown = false
                                        showToastMessage = "Audio track switched to $lang"
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Resource / Episode Stream Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                isPlaying = true
                                showToastMessage = "Streaming $displayTitle (Oracle Cloud 4K Proxy)"
                            },
                        color = Color(0xFF2A2838), // Dark slate container
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = displayTitle,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 5. Tab Headers: "For you" vs "Comments(4) •"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // For you tab
                    Column(
                        modifier = Modifier.clickable { selectedTab = "For you" }
                    ) {
                        Text(
                            text = "For you",
                            color = if (selectedTab == "For you") Color.White else Color(0xFF88889C),
                            fontSize = 15.sp,
                            fontWeight = if (selectedTab == "For you") FontWeight.Bold else FontWeight.Normal
                        )
                        if (selectedTab == "For you") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(2.5.dp)
                                    .background(Color.White, RoundedCornerShape(2.dp))
                            )
                        }
                    }

                    // Comments tab
                    Row(
                        modifier = Modifier.clickable { selectedTab = "Comments" },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Comments(4)",
                            color = if (selectedTab == "Comments") Color.White else Color(0xFF88889C),
                            fontSize = 15.sp,
                            fontWeight = if (selectedTab == "Comments") FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30)) // Red notification dot
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 6. "For you" 3-Column Recommendations Grid
                if (selectedTab == "For you") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        forYouList.chunked(3).forEach { rowList ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowList.forEach { recMovie ->
                                    val recPoster = recMovie.posterUrl ?: recMovie.backdropUrl ?: ""
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.68f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1C1B26))
                                            .clickable {
                                                currentMedia = recMovie
                                                isPlaying = true
                                                sessionManager?.addWatchHistory(
                                                    mediaId = recMovie.id,
                                                    watched = "1m",
                                                    total = recMovie.duration.ifBlank { "2h 10m" },
                                                    progress = 0.05f
                                                )
                                                showToastMessage = "Now playing: ${recMovie.title}"
                                            }
                                    ) {
                                        AsyncImage(
                                            model = recPoster,
                                            contentDescription = recMovie.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Language badge top right
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp),
                                            color = Color(0xCC000000),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = recMovie.audioLanguages.firstOrNull()?.replace(" [Original]", "") ?: "Hindi",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }

                                        // Title shadow bar bottom
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Transparent, Color(0xF0000000))
                                                    )
                                                )
                                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = recMovie.title,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                for (i in 0 until (3 - rowList.size)) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    // Comments Preview
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (omdbComments.isEmpty()) {
                            Text("No reviews yet. Be the first!", color = Color(0xFF88889C), fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        } else {
                            for (comment in omdbComments) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF1E1C2B),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = comment.author, color = Color(0xFFFFD54F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = comment.text, color = Color.White, fontSize = 12.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

            // Info Dialog when tapping Info >
            if (showInfoDialog) {
                Dialog(onDismissRequest = { showInfoDialog = false }) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clip(RoundedCornerShape(16.dp)),
                        color = Color(0xFF1B1A28),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(text = displayTitle, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentMedia.synopsis.ifBlank { "Full HD / 4K streaming powered by Cineflix Oracle Cloud Proxy server." },
                                color = Color(0xFFA6A6BA),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showInfoDialog = false },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                            ) {
                                Text("Close", color = Color.White)
                            }
                        }
                    }
                }
            }

            // Up Next overlay when an episode finishes
            if (showUpNext && nextEpisode != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 78.dp, start = 20.dp, end = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xCC181726),
                        border = BorderStroke(1.dp, Color(0xFFFFB300))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Up Next",
                                    color = Color(0xFFFFB300),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = nextEpisode!!.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Season ${nextEpisode!!.seasonNumber} • Episode ${nextEpisode!!.episodeNumber} • starts in 3s",
                                    color = Color(0xFF88889C),
                                    fontSize = 10.sp
                                )
                            }
                            IconButton(
                                onClick = { showUpNext = false },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Next Episode",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Toast status bar at bottom if action performed
            showToastMessage?.let { msg ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp, start = 20.dp, end = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xE6101018),
                    border = BorderStroke(1.dp, Color(0xFFFFD54F))
                ) {
                    Text(
                        text = msg,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (isSelected) Color(0xFFFFB300) else Color(0xFF201F2B),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFB300) else Color(0x33FFFFFF))
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun SubscriptionModal(
    onDismiss: () -> Unit
) {
    var selectedTier by remember { mutableStateOf(StreamingTier.PREMIUM_4K) }
    var isAnnual by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .testTag("subscription_modal")
                .fillMaxSize()
                .background(CineflixTheme.colors.backgroundOverlay)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CineflixTheme.colors.cardBorder, RoundedCornerShape(16.dp))
                    .clickable(enabled = false) {},
                colors = CardDefaults.cardColors(containerColor = CineflixTheme.colors.backgroundSecondary)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Title & Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Choose Your Plan",
                                style = CineflixTheme.typography.h3,
                                color = Color.White
                            )
                            Text(
                                text = "Cancel anytime. No ads on premium.",
                                style = CineflixTheme.typography.bodySmall,
                                color = CineflixTheme.colors.textSecondary
                            )
                        }
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Monthly vs Annual Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(CineflixTheme.colors.cardBackground)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (!isAnnual) CineflixTheme.colors.accentRed else Color.Transparent)
                                .clickable { isAnnual = false }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Monthly",
                                style = CineflixTheme.typography.buttonSmall,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isAnnual) CineflixTheme.colors.primaryGradientBrush else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                                .clickable { isAnnual = true }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Annual (Save 20%)",
                                style = CineflixTheme.typography.buttonSmall,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Plan Cards
                    StreamingTier.entries.forEach { tier ->
                        val isSelected = tier == selectedTier
                        Box(
                            modifier = Modifier
                                .testTag("plan_tier_${tier.name}")
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) CineflixTheme.colors.cardBackgroundHover
                                    else CineflixTheme.colors.cardBackground
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) CineflixTheme.colors.accentPink else CineflixTheme.colors.cardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedTier = tier }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = tier.label,
                                            style = CineflixTheme.typography.h5,
                                            color = Color.White
                                        )
                                        if (tier == StreamingTier.PREMIUM_4K) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            QualityBadge(label = "BEST VALUE")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${tier.resolution} • Up to ${tier.devices} screens simultaneously",
                                        style = CineflixTheme.typography.bodySmall,
                                        color = CineflixTheme.colors.textSecondary
                                    )
                                }

                                Text(
                                    text = if (isAnnual) tier.priceAnnual else tier.priceMonthly,
                                    style = CineflixTheme.typography.h5,
                                    color = if (isSelected) CineflixTheme.colors.accentPink else Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // CTA
                    CineflixPrimaryButton(
                        text = "Continue with ${selectedTier.label}",
                        useGradient = true,
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "btn_confirm_subscription"
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsModal(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var videoQuality by remember { mutableStateOf("4K Ultra HD (Dolby Vision)") }
    var smartDownloads by remember { mutableStateOf(true) }
    var cellularDataSaver by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .testTag("settings_modal")
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
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Playback Settings",
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

                    // Streaming Quality Selector
                    Text(
                        text = "STREAMING VIDEO QUALITY",
                        style = CineflixTheme.typography.monoBadge,
                        color = CineflixTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf("Auto (Recommended)", "4K Ultra HD (Dolby Vision)", "High Definition (1080p)", "Data Saver (720p)").forEach { q ->
                        val isSelected = q == videoQuality
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { videoQuality = q }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = q,
                                style = CineflixTheme.typography.bodyMedium,
                                color = if (isSelected) CineflixTheme.colors.accentRed else Color.White
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = CineflixTheme.colors.accentRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Smart Downloads Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Smart Auto-Downloads",
                                style = CineflixTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Automatically download next episodes when on Wi-Fi",
                                style = CineflixTheme.typography.caption,
                                color = CineflixTheme.colors.textSecondary
                            )
                        }
                        Switch(
                            checked = smartDownloads,
                            onCheckedChange = { smartDownloads = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CineflixTheme.colors.accentRed
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Cellular Data Saver Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cellular Data Saver",
                                style = CineflixTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Limit streaming to SD quality on mobile data",
                                style = CineflixTheme.typography.caption,
                                color = CineflixTheme.colors.textSecondary
                            )
                        }
                        Switch(
                            checked = cellularDataSaver,
                            onCheckedChange = { cellularDataSaver = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CineflixTheme.colors.accentRed
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    CineflixPrimaryButton(
                        text = "Save Preferences",
                        onClick = {
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "btn_save_settings"
                    )
                }
            }
        }
    }
}
