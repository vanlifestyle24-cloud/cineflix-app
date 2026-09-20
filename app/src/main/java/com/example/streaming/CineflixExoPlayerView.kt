package com.example.streaming

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.autoMirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.compose.ui.draw.alpha
import com.example.ui.theme.CineflixTheme

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

enum class VideoQualityOption(val label: String, val badge: String, val width: Int, val height: Int) {
    AUTO("Auto (Adaptive)", "AUTO", 0, 0),
    FHD_1080P("1080p Full HD", "1080p", 1920, 1080),
    HD_720P("720p HD", "720p", 1280, 720),
    SD_480P("480p SD", "480p", 854, 480),
    DATA_SAVER_360P("360p Data Saver", "360p", 640, 360)
}

enum class AspectRatioMode(val label: String, val mode: Int) {
    FIT("16:9 Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    ZOOM("Zoom Crop", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    FILL("Stretch", AspectRatioFrameLayout.RESIZE_MODE_FILL)
}

/**
 * Categorizes a stream URL so we know whether to try the WebView resolver at all.
 */
private sealed class StreamUrlType {
    object DirectMedia : StreamUrlType()          // Already an mp4/m3u8/sp.rmbl.ws URL
    data class RumblePage(val pageUrl: String) : StreamUrlType() // rumble.com/watch/... or .../v<id>...
    data class RumbleEmbed(val embedUrl: String) : StreamUrlType() // rumble.com/embed/...
    data class Other(val url: String) : StreamUrlType()
}

private fun classifyStreamUrl(streamUrl: String): StreamUrlType {
    val lower = streamUrl.lowercase()
    // Direct playable media (already a CDN URL)
    if (lower.contains("sp.rmbl.ws") || lower.contains(".m3u8") || lower.contains(".mp4")) {
        return StreamUrlType.DirectMedia
    }
    // Rumble page URL (most common format for unlisted videos)
    if (lower.contains("rumble.com/") && (lower.contains("/v") || lower.contains("/watch/"))) {
        return StreamUrlType.RumblePage(streamUrl)
    }
    // Rumble embed URL (older format, may return 410)
    if (lower.contains("rumble.com/embed/")) {
        return StreamUrlType.RumbleEmbed(streamUrl)
    }
    return StreamUrlType.Other(streamUrl)
}

/**
 * Resolve a Rumble URL to the URL that should be loaded in the WebView.
 * For unlisted videos, we load the PAGE URL directly (not the embed URL)
 * and extract the CDN URL from the page content.
 * Returns null if this URL type cannot be resolved via WebView.
 */
private fun resolveRumbleUrl(type: StreamUrlType): String? {
    when (type) {
        is StreamUrlType.RumblePage -> {
            // Use page URL directly — Rumble unlisted videos are accessible via their page URL
            // and the CDN URL is embedded in the page HTML/JSON-LD
            return type.pageUrl
        }
        is StreamUrlType.RumbleEmbed -> {
            // Try embed first; if it fails (410), fall back to extracting from page URL
            // Extract the video ID from embed URL to build page URL
            val embedUrl = type.embedUrl
            val idMatch = Regex("""/embed/(v?\w+)""").find(embedUrl)
            val videoId = idMatch?.groupValues?.get(1) ?: return embedUrl
            return "https://rumble.com/v${videoId.removePrefix("v")}-video.html"
        }
        is StreamUrlType.DirectMedia -> return null
        is StreamUrlType.Other -> return null
    }
}

/**
 * Full-Featured Custom ExoPlayer Composable with Professional OTT Controls
 */
@OptIn(UnstableApi::class)
@Composable
fun CineflixExoPlayerView(
    streamUrl: String,
    isPlaying: Boolean,
    isMuted: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    skipIntroStartSec: Int? = null,
    skipIntroEndSec: Int? = null,
    onProgressUpdate: ((currentMs: Long, durationMs: Long) -> Unit)? = null,
    onPlayPauseToggle: ((Boolean) -> Unit)? = null,
    onCloseClick: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isBuffering by remember { mutableStateOf(true) }
    var streamError by remember { mutableStateOf<String?>(null) }

    // ──── URL classification & resolution ───────────────────────────────────
    val urlType = remember(streamUrl) { classifyStreamUrl(streamUrl) }
    var rumbleCdnUrl by remember { mutableStateOf<String?>(null) }
    var isRumbleResolving by remember { mutableStateOf(false) }
    var rumbleResolutionState by remember { mutableStateOf<RumbleResolutionState>(RumbleResolutionState.Idle) }

    LaunchedEffect(streamUrl) {
        rumbleCdnUrl = null
        streamError = null
        isRumbleResolving = false
        rumbleResolutionState = RumbleResolutionState.Idle

        when (val t = urlType) {
            is StreamUrlType.DirectMedia -> {
                // Already a direct playable URL — no resolution needed
                isRumbleResolving = false
                rumbleResolutionState = RumbleResolutionState.Ready
            }
            is StreamUrlType.RumbleEmbed, is StreamUrlType.RumblePage -> {
                val embedUrl = normalizeRumbleUrl(t)
                if (embedUrl != null) {
                    isRumbleResolving = true
                    rumbleResolutionState = RumbleResolutionState.Resolving
                } else {
                    rumbleResolutionState = RumbleResolutionState.Ready
                }
            }
            is StreamUrlType.Other -> {
                // Try to load directly; ExoPlayer will fail if not playable
                rumbleResolutionState = RumbleResolutionState.Ready
            }
        }
    }

    val resolvedStreamUrl = when {
        urlType is StreamUrlType.DirectMedia -> streamUrl
        rumbleCdnUrl != null -> rumbleCdnUrl!!
        else -> ""
    }

    // Playback state
    var localIsPlaying by remember { mutableStateOf(isPlaying) }
    var localIsMuted by remember { mutableStateOf(isMuted) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isUserDraggingSlider by remember { mutableStateOf(false) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedSpeed by remember { mutableFloatStateOf(1.0f) }
    var selectedQuality by remember { mutableStateOf(VideoQualityOption.AUTO) }
    var selectedAspectMode by remember { mutableStateOf(AspectRatioMode.FIT) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var seekFeedbackText by remember { mutableStateOf<String?>(null) }

    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile; Cineflix/1.0)")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(30000)
            .setReadTimeoutMs(30000)
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }
    var playerViewInstance by remember { mutableStateOf<PlayerView?>(null) }

    // Bind URL into ExoPlayer
    LaunchedEffect(resolvedStreamUrl) {
        if (resolvedStreamUrl.isNotBlank() && exoPlayer != null) {
            isBuffering = true
            streamError = null
            try {
                val mediaItem = MediaItem.fromUri(resolvedStreamUrl)
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                exoPlayer?.play()
                localIsPlaying = true
                rumbleResolutionState = RumbleResolutionState.Ready
            } catch (e: Exception) {
                streamError = e.message
                onError?.invoke(e.message ?: "Failed to prepare stream")
            }
        }
    }

    // Sync Play/Pause from parent
    LaunchedEffect(isPlaying) {
        localIsPlaying = isPlaying
        if (isPlaying) exoPlayer?.play() else exoPlayer?.pause()
    }
    // Sync Mute state
    LaunchedEffect(isMuted) {
        localIsMuted = isMuted
        exoPlayer?.volume = if (isMuted) 0f else 1f
    }
    // Apply speed changes
    LaunchedEffect(selectedSpeed) {
        exoPlayer?.playbackParameters = PlaybackParameters(selectedSpeed)
    }
    // Apply resolution / quality constraints
    LaunchedEffect(selectedQuality, exoPlayer) {
        if (exoPlayer == null) return@LaunchedEffect
        val currentParams = exoPlayer.trackSelectionParameters
        val newParams = if (selectedQuality == VideoQualityOption.AUTO) {
            currentParams.buildUpon().clearVideoSizeConstraints().build()
        } else {
            currentParams.buildUpon()
                .setMaxVideoSize(selectedQuality.width, selectedQuality.height)
                .build()
        }
        exoPlayer.trackSelectionParameters = newParams
    }
    // Apply Aspect Ratio mode
    LaunchedEffect(selectedAspectMode, playerViewInstance, LocalConfiguration.current.orientation) {
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape && selectedAspectMode == AspectRatioMode.FIT) {
            playerViewInstance?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        } else {
            playerViewInstance?.resizeMode = selectedAspectMode.mode
        }
    }
    // Periodic time & progress ticker
    LaunchedEffect(exoPlayer, localIsPlaying) {
        while (true) {
            if (exoPlayer?.playbackState == Player.STATE_READY && exoPlayer?.isPlaying == true) {
                val current = exoPlayer?.currentPosition ?: 0L
                val dur = (exoPlayer?.duration ?: 0L).coerceAtLeast(1L)
                currentPositionMs = current
                durationMs = dur
                if (!isUserDraggingSlider) {
                    sliderPosition = (current.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                }
                onProgressUpdate?.invoke(current, dur)
            }
            delay(500)
        }
    }
    // Auto-hide controls timer (3.5 seconds)
    LaunchedEffect(areControlsVisible, lastInteractionTime, localIsPlaying, isScreenLocked) {
        if (areControlsVisible) {
            delay(3500)
            areControlsVisible = false
        }
    }
    // Clear seek feedback after 800ms
    LaunchedEffect(seekFeedbackText) {
        if (seekFeedbackText != null) {
            delay(800)
            seekFeedbackText = null
        }
    }

    // Player State Listeners
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    durationMs = exoPlayer?.duration?.coerceAtLeast(1L) ?: 0L
                    currentPositionMs = exoPlayer?.currentPosition ?: 0L
                }
            }
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                localIsPlaying = isPlayingNow
                onPlayPauseToggle?.invoke(isPlayingNow)
            }
            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                val errorMsg = error.message ?: ""
                val is403 = error.cause is HttpDataSource.InvalidResponseCodeException || errorMsg.contains("403")
                val is500 = errorMsg.contains("500")
                if ((is403 || is500) && streamUrl.contains("stream?v=")) {
                    coroutineScope.launch {
                        delay(1000)
                        val retryUrl = if (streamUrl.contains("&t=")) {
                            streamUrl.substringBefore("&t=") + "&t=${System.currentTimeMillis()}"
                        } else {
                            "$streamUrl&t=${System.currentTimeMillis()}"
                        }
                        try {
                            exoPlayer?.setMediaItem(MediaItem.fromUri(retryUrl))
                            exoPlayer?.prepare()
                            exoPlayer?.play()
                            streamError = null
                        } catch (e: Exception) {
                            streamError = "Proxy block detected. Try refreshing Admin script."
                            onError?.invoke(streamError ?: "Playback error")
                        }
                    }
                } else {
                    streamError = error.message ?: "Playback error"
                    onError?.invoke(error.message ?: "Playback error encountered")
                }
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
            exoPlayer?.stop()
            exoPlayer?.release()
        }
    }

    // Seek Helpers
    val seekForward10s: () -> Unit = {
        val current = exoPlayer?.currentPosition ?: 0L
        val target = (current + 10000).coerceAtMost(durationMs)
        exoPlayer?.seekTo(target)
        currentPositionMs = target
        seekFeedbackText = "+10s"
        lastInteractionTime = System.currentTimeMillis()
    }
    val seekBackward10s: () -> Unit = {
        val current = exoPlayer?.currentPosition ?: 0L
        val target = (current - 10000).coerceAtLeast(0L)
        exoPlayer?.seekTo(target)
        currentPositionMs = target
        seekFeedbackText = "-10s"
        lastInteractionTime = System.currentTimeMillis()
    }
    val togglePlayPause: () -> Unit = {
        if (exoPlayer?.isPlaying == true) { exoPlayer?.pause(); localIsPlaying = false }
        else { exoPlayer?.play(); localIsPlaying = true }
        onPlayPauseToggle?.invoke(localIsPlaying)
        lastInteractionTime = System.currentTimeMillis()
    }

    // ──── RUMBLE RESOLUTION (only for RumblePage/RumbleEmbed types) ─────────
    val needsResolver = (urlType is StreamUrlType.RumbleEmbed || urlType is StreamUrlType.RumblePage)
        && rumbleCdnUrl == null
        && rumbleResolutionState !is RumbleResolutionState.Failed
        && rumbleResolutionState !is RumbleResolutionState.Ready
    if (needsResolver) {
        RumbleStreamResolver(
            embedUrl = normalizeRumbleUrl(urlType)!!,
            onUrlResolved = { directUrl ->
                rumbleCdnUrl = directUrl
                isRumbleResolving = false
                isBuffering = false
                rumbleResolutionState = RumbleResolutionState.Ready
            },
            onResolutionFailed = { error ->
                val msg = "Rumble stream failed: $error"
                streamError = msg
                isRumbleResolving = false
                isBuffering = false
                rumbleResolutionState = RumbleResolutionState.Failed(error)
                onError?.invoke(msg)
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                areControlsVisible = !areControlsVisible
                lastInteractionTime = System.currentTimeMillis()
            },
        contentAlignment = Alignment.Center
    ) {
        // Video Viewport AndroidView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = selectedAspectMode.mode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    playerViewInstance = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering / Rumble Resolution Indicator
        if (isBuffering || isRumbleResolving) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = CineflixTheme.colors.accentRed,
                        strokeWidth = 3.5.dp,
                        modifier = Modifier.size(44.dp)
                    )
                    if (isRumbleResolving) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Loading Rumble...",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Buffering...",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // ──── RESOLUTION ERROR / TIMEOUT UI ───────────────────────────────
        val failState = (rumbleResolutionState as? RumbleResolutionState.Failed)?.reason
        if (failState != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xBB000000))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Unable to load Rumble stream",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = failState,
                        color = Color(0xFFA6A6BA),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                rumbleResolutionState = RumbleResolutionState.Idle
                                rumbleCdnUrl = null
                                isRumbleResolving = true
                                isBuffering = true
                            },
                        color = CineflixTheme.colors.accentRed,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Retry",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (streamUrl.isNotBlank()) {
                        TextButton(onClick = { /* Could open in browser */ }) {
                            Text(
                                text = "Open link in browser",
                                color = Color(0xFF00E5FF),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Seek Feedback Overlay (+10s / -10s)
        AnimatedVisibility(
            visible = seekFeedbackText != null,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xCC000000))
                    .border(1.dp, CineflixTheme.colors.accentRed, RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = seekFeedbackText ?: "",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }

        // Skip Intro Floating Button
        val currentSec = currentPositionMs / 1000L
        val showSkipIntro = skipIntroStartSec != null && skipIntroEndSec != null &&
                currentSec >= skipIntroStartSec && currentSec <= skipIntroEndSec
        AnimatedVisibility(
            visible = showSkipIntro,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 76.dp, end = 24.dp)
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        if (skipIntroEndSec != null) {
                            exoPlayer?.seekTo(skipIntroEndSec * 1000L)
                            seekFeedbackText = "Skipped Intro"
                            coroutineScope.launch { delay(1200); seekFeedbackText = null }
                        }
                    },
                color = Color(0xFFFFB300),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Skip Intro",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Skip Intro",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Locked Mode HUD
        if (isScreenLocked) {
            AnimatedVisibility(
                visible = areControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xCC1E1E2C))
                        .border(1.dp, CineflixTheme.colors.accentRed, RoundedCornerShape(20.dp))
                        .clickable {
                            isScreenLocked = false
                            areControlsVisible = true
                            lastInteractionTime = System.currentTimeMillis()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Unlock Controls",
                            tint = CineflixTheme.colors.accentRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Unlock",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Standard Unlocked Controls HUD
        if (!isScreenLocked) {
            AnimatedVisibility(
                visible = areControlsVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xB3000000),
                                    Color(0x22000000),
                                    Color(0xB3000000)
                                )
                            )
                        )
                ) {
                    // TOP BAR
                    val configuration = LocalConfiguration.current
                    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            if (onCloseClick != null) {
                                IconButton(
                                    onClick = onCloseClick,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x66000000))
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Close Player",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (!title.isNullOrBlank()) {
                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val activity = context.findActivity()
                                    if (activity != null) {
                                        if (isLandscape) {
                                            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        } else {
                                            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        }
                                    }
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x66000000))
                            ) {
                                Icon(
                                    imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.AspectRatio,
                                    contentDescription = if (isLandscape) "Normal Screen" else "Landscape Mode",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    isScreenLocked = true
                                    areControlsVisible = false
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x66000000))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Lock Screen",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    areControlsVisible = false
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x66000000))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hide Controls",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // CENTER CONTROLS: -10s, Play/Pause, +10s
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0x80000000))
                                .border(1.dp, Color(0x44FFFFFF), CircleShape)
                                .clickable { seekBackward10s() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CineflixTheme.colors.accentRed)
                                .border(2.dp, Color.White, CircleShape)
                                .clickable { togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (localIsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (localIsPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0x80000000))
                                .border(1.dp, Color(0x44FFFFFF), CircleShape)
                                .clickable { seekForward10s() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // BOTTOM CONTROLS: Scrubber, Timestamps, Speed & Quality
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Slider(
                            value = if (sliderPosition.isNaN()) 0f else sliderPosition.coerceIn(0f, 1f),
                            onValueChange = {
                                isUserDraggingSlider = true
                                sliderPosition = it.coerceIn(0f, 1f)
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            onValueChangeFinished = {
                                isUserDraggingSlider = false
                                val targetMs = (sliderPosition * durationMs).toLong()
                                exoPlayer?.seekTo(targetMs)
                                currentPositionMs = targetMs
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = CineflixTheme.colors.accentRed,
                                activeTrackColor = CineflixTheme.colors.accentRed,
                                inactiveTrackColor = Color(0x55FFFFFF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatTimestamp(currentPositionMs)} / ${formatTimestamp(durationMs)}",
                                style = CineflixTheme.typography.monoTimestamp,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Speed Selector
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x66000000))
                                        .border(0.8.dp, Color(0x55FFFFFF), RoundedCornerShape(12.dp))
                                        .clickable { showSpeedDialog = true; lastInteractionTime = System.currentTimeMillis() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = "Playback Speed",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (selectedSpeed == 1.0f) "1x" else "${selectedSpeed}x",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                // Quality Selector
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x66000000))
                                        .border(0.8.dp, Color(0x55FFFFFF), RoundedCornerShape(12.dp))
                                        .clickable { showQualityDialog = true; lastInteractionTime = System.currentTimeMillis() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.HighQuality,
                                            contentDescription = "Resolution Quality",
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = selectedQuality.badge,
                                            color = Color(0xFF00E5FF),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                // Volume
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x66000000))
                                        .clickable {
                                            localIsMuted = !localIsMuted
                                            exoPlayer?.volume = if (localIsMuted) 0f else 1f
                                            lastInteractionTime = System.currentTimeMillis()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (localIsMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                        contentDescription = "Toggle Audio",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // SPEED SELECTOR DIALOG
        if (showSpeedDialog) {
            Dialog(onDismissRequest = { showSpeedDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141420)),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                        .padding(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = CineflixTheme.colors.accentRed, modifier = Modifier.size(20.dp))
                            Text(text = "Playback Speed", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            val isSelected = selectedSpeed == speed
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CineflixTheme.colors.accentRed.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable { selectedSpeed = speed; showSpeedDialog = false; lastInteractionTime = System.currentTimeMillis() }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                                    color = if (isSelected) CineflixTheme.colors.accentRed else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                                if (isSelected) Text(text = "✓", color = CineflixTheme.colors.accentRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // RESOLUTION / QUALITY SELECTOR DIALOG
        if (showQualityDialog) {
            Dialog(onDismissRequest = { showQualityDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141420)),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                        .padding(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.HighQuality, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                            Text(text = "Video Quality & Resolution", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        VideoQualityOption.values().forEach { option ->
                            val isSelected = selectedQuality == option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0x2200E5FF) else Color.Transparent)
                                    .clickable { selectedQuality = option; showQualityDialog = false; lastInteractionTime = System.currentTimeMillis() }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = option.label,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }
                                if (isSelected) Text(text = "✓", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ──── Rumble Resolution State ────────────────────────────────────────────────
private sealed class RumbleResolutionState {
    object Idle : RumbleResolutionState()
    object Resolving : RumbleResolutionState()
    data class Failed(val reason: String) : RumbleResolutionState()
    object Ready : RumbleResolutionState()
}

/**
 * Improved Rumble Stream Resolver with:
 * - Robust JS extraction (og:video:url, video src, source tags, script regex)
 * - Retry + timeout logic
 * - Software layer to avoid hardware acceleration issues
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun RumbleStreamResolver(
    embedUrl: String,
    onUrlResolved: (String) -> Unit,
    onResolutionFailed: (String) -> Unit
) {
    val context = LocalContext.current
    var hasResolved by remember { mutableStateOf(false) }
    var attempts by remember { mutableIntStateOf(0) }
    val maxAttempts = 120 // 60 seconds
    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    var pollingRunnable: Runnable? by remember { mutableStateOf(null) }

    // Reset on URL change
    LaunchedEffect(embedUrl) {
        hasResolved = false
        attempts = 0
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            pollingRunnable?.let { handler.removeCallbacks(it) }
        }
    }

    if (!hasResolved) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

                    val jsInterfaceName = "RumbleResolver"
                    addJavascriptInterface(object {
                        @android.webkit.JavascriptInterface
                        fun onVideoSrcFound(src: String) {
                            if (src.isNotBlank() && !hasResolved) {
                                hasResolved = true
                                onUrlResolved(src)
                            }
                        }
                    }, jsInterfaceName)

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            val localHandler = android.os.Handler(android.os.Looper.getMainLooper())
                            val checkVideoSrc = object : Runnable {
                                override fun run() {
                                    if (hasResolved || attempts > maxAttempts) return
                                    attempts++

                                    val js = """
                                        (function() {
                                            var result = '';
                                            // 1) Try og:video:url meta tag
                                            var meta = document.querySelector('meta[property="og:video:url"]');
                                            if (meta && meta.content && meta.content.indexOf('blob:') !== 0) {
                                                result = meta.content;
                                            }
                                            // 2) Try video element src
                                            if (!result) {
                                                var video = document.querySelector('video');
                                                if (video) {
                                                    if (video.src && video.src.indexOf('blob:') !== 0) {
                                                        result = video.src;
                                                    } else {
                                                        var sources = video ? video.getElementsByTagName('source') : [];
                                                        for (var i = 0; i < sources.length; i++) {
                                                            if (sources[i].src) { result = sources[i].src; break; }
                                                        }
                                                    }
                                                }
                                            }
                                            // 3) Search all script textContent for CDN URLs
                                            if (!result) {
                                                var scripts = document.getElementsByTagName('script');
                                                for (var i = 0; i < scripts.length; i++) {
                                                    var text = scripts[i].textContent || '';
                                                    var match = text.match(/"(https:\\/\\/sp\\.rmbl\\.ws\\/[^"]+?\\.(?:mp4|m3u8)[^"]*?)"/);
                                                    if (match) { result = match[1]; break; }
                                                    var match2 = text.match(/"(https:\\/\\/[^"]+?\\.(?:mp4|m3u8|m3u8\\?[^"]*))"/);
                                                    if (match2) { result = match2[1]; break; }
                                                    // Also try without quotes
                                                    var match3 = text.match(/(https:\\/\\/sp\\.rmbl\\.ws\\/[^"'\s]+?\\.(?:mp4|m3u8))/);
                                                    if (match3) { result = match3[1]; break; }
                                                }
                                            }
                                            // 4) Try JSON data in page
                                            if (!result) {
                                                var jsonScripts = document.querySelectorAll('script[type="application/json"]');
                                                for (var i = 0; i < jsonScripts.length; i++) {
                                                    try {
                                                        var data = JSON.parse(jsonScripts[i].textContent);
                                                        var str = JSON.stringify(data);
                                                        var match = str.match(/(https:\\/\\/[^"]+?\\.(?:mp4|m3u8)[^"]*?)/);
                                                        if (match) { result = match[1]; break; }
                                                    } catch(e) {}
                                                }
                                            }
                                            return result;
                                        })()
                                    """.trimIndent()

                                    evaluateJavascript(js) { res ->
                                        val cleanUrl = res?.trim('"')?.trim('\'')
                                        if (!cleanUrl.isNullOrBlank() && cleanUrl != "null" && cleanUrl != "undefined") {
                                            if (!hasResolved) {
                                                hasResolved = true
                                                onUrlResolved(cleanUrl)
                                            }
                                        } else {
                                            if (attempts <= maxAttempts) {
                                                localHandler.postDelayed(this, 500)
                                            }
                                        }
                                    }
                                }
                            }
                            pollingRunnable = checkVideoSrc
                            localHandler.post(checkVideoSrc)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            if (!hasResolved) {
                                hasResolved = true
                                onResolutionFailed("Page load error ($errorCode): ${description ?: "unknown"}")
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            errorResponse: WebResourceResponse?
                        ) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            val code = errorResponse?.statusCode ?: 0
                            if (!hasResolved) {
                                hasResolved = true
                                onResolutionFailed("HTTP $code — Rumble embed endpoint may be blocked or moved.")
                            }
                        }

                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            // Ignore redirects
                        }
                    }

                    setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                    loadUrl(embedUrl)
                }
            },
            modifier = Modifier.size(1.dp).alpha(0f)
        )
    }

    // Timeout check — if after 45 seconds still not resolved, fail
    LaunchedEffect(hasResolved) {
        if (!hasResolved) {
            delay(45000)
            if (!hasResolved) {
                onResolutionFailed("Timeout after 45s — Rumble embed page may be blocked or changed structure.")
            }
        }
    }
}

private fun formatTimestamp(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
