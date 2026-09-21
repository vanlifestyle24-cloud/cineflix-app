package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.AuthModal
import com.example.auth.SessionManager
import com.example.auth.SupabaseAuthRepository
import com.example.auth.UserProfile
import com.example.auth.UserProfileModal
import com.example.components.CineflixBottomBar
import com.example.components.CineflixBottomTab
import com.example.components.CineflixFooter
import com.example.components.CineflixGenreChip
import com.example.components.CineflixHeroCarousel
import com.example.components.CineflixNavbar
import com.example.components.CineflixNavDestination
import com.example.components.CineflixPrimaryButton
import com.example.components.CineflixSearchBar
import com.example.components.LandscapeMediaCard
import com.example.components.MoviePosterCard
import com.example.components.QualityBadge
import com.example.components.SettingsModal
import com.example.components.SubscriptionModal
import com.example.components.Top10RankCard
import com.example.components.TrailerPlayerModal
import com.example.model.MediaItem
import com.example.model.MockMediaRepository
import com.example.omdb.CacheSource
import com.example.omdb.OmdbRepository
import com.example.streaming.CineflixExoPlayerView
import com.example.streaming.CineflixLocalCatalogManager
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.DownloadsAndHistoryScreen
import com.example.ui.screens.MeAndProfileScreen
import com.example.ui.screens.PremiumScreen
import com.example.ui.theme.CineflixTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * CINEFLIX Streaming Home Experience
 * Combining:
 * - Netflix: Bold crimson accents, Top 10 styled typography cards, Match % badges
 * - Prime Video: Clean dark charcoal (#111118), X-Ray row navigation with back/next arrows
 * - Disney+ Hotstar: Deep royal glassmorphism, VIP badges, Specials row
 * - JioCinema: Vibrant purple-pink gradients (#7B2FF7 → #FF3CAC) and live filters
 * - Supabase Auth & Local Mobile Storage Cache (Rate-Limit Protected)
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Supabase Auth & Local Device Cache
    val sessionManager = remember { SessionManager(context) }
    val authRepository = remember { SupabaseAuthRepository(sessionManager) }

    val currentUser by sessionManager.currentUser.collectAsStateWithLifecycle()
    val userWatchlist by sessionManager.watchlist.collectAsStateWithLifecycle()
    val isAdminMode by sessionManager.isAdminMode.collectAsStateWithLifecycle()

    // 100% In-App Self-Contained Media & Stream Catalog Manager
    val catalogManager = remember { CineflixLocalCatalogManager(context) }
    val allItems by catalogManager.catalog.collectAsStateWithLifecycle()
    val heroItems by catalogManager.heroItems.collectAsStateWithLifecycle()
    val isCatalogSyncing by catalogManager.isSyncing.collectAsStateWithLifecycle()
    val catalogSyncMessage by catalogManager.syncMessage.collectAsStateWithLifecycle()

    // Navigation & View State
    var currentNav by remember { mutableStateOf(CineflixNavDestination.HOME) }
    var currentBottomTab by remember { mutableStateOf(CineflixBottomTab.HOME) }
    var selectedGenre by remember { mutableStateOf("All Genres") }
    var searchQuery by remember { mutableStateOf("") }

    // OMDb Dual-Key Search with Dedicated Supabase Database Cache
    val omdbRepository = remember { OmdbRepository(context) }
    var isSearchingOmdb by remember { mutableStateOf(false) }
    var omdbStatusMessage by remember { mutableStateOf<String?>(null) }
    var omdbCacheBadge by remember { mutableStateOf<String?>(null) }
    var isRateLimited by remember { mutableStateOf(false) }
    var omdbSearchResults by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    // Reactive search effect with debouncing
    LaunchedEffect(searchQuery) {
        val clean = searchQuery.trim()
        if (clean.length >= 2) {
            isSearchingOmdb = true
            delay(300)
            try {
                val result = omdbRepository.searchMovies(clean)
                omdbSearchResults = result.items
                omdbStatusMessage = result.statusMessage
                omdbCacheBadge = result.source.badgeText
                isRateLimited = result.isRateLimited
            } catch (e: Exception) {
                omdbStatusMessage = "Search error: ${e.message}"
            } finally {
                isSearchingOmdb = false
            }
        } else {
            omdbSearchResults = emptyList()
            omdbStatusMessage = null
            omdbCacheBadge = null
            isRateLimited = false
            isSearchingOmdb = false
        }
    }

    // Modal states
    var activeTrailerMedia by remember { mutableStateOf<MediaItem?>(null) }
    var showSubscriptionModal by remember { mutableStateOf(false) }
    var showSettingsModal by remember { mutableStateOf(false) }
    var showAuthModal by remember { mutableStateOf(false) }
    var showProfileModal by remember { mutableStateOf(false) }

    val playMediaItem: (MediaItem) -> Unit = { media ->
        if (currentUser == null) {
            // User not logged in, force login modal
            showAuthModal = true
        } else {
            sessionManager.addWatchHistory(
                mediaId = media.id,
                watched = "5m",
                total = media.duration.ifBlank { "2h 10m" },
                progress = 0.05f
            )
            activeTrailerMedia = media
        }
    }

    BackHandler(enabled = isAdminMode || currentBottomTab != CineflixBottomTab.HOME || currentNav != CineflixNavDestination.HOME) {
        if (isAdminMode) {
            sessionManager.setAdminMode(false)
        } else if (currentNav != CineflixNavDestination.HOME) {
            currentNav = CineflixNavDestination.HOME
        } else if (currentBottomTab != CineflixBottomTab.HOME) {
            currentBottomTab = CineflixBottomTab.HOME
        }
    }

    Box(
        modifier = modifier
            .testTag("home_screen_container")
            .fillMaxSize()
            .background(CineflixTheme.colors.backgroundPrimary)
    ) {
        if (isAdminMode) {
            AdminDashboardScreen(
                sessionManager = sessionManager,
                catalogManager = catalogManager,
                onSwitchToUserView = { sessionManager.setAdminMode(false) },
                onPlayMedia = { activeTrailerMedia = it },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Sticky Frosted Glass Top Navigation Bar (shown on secondary destinations)
                if (currentNav != CineflixNavDestination.HOME) {
                    CineflixNavbar(
                        currentDestination = currentNav,
                        currentUser = currentUser,
                        onNavigate = { currentNav = it },
                        onSearchClick = {
                            searchQuery = if (searchQuery.isEmpty()) "Inception" else ""
                        },
                        onNotificationsClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("3 new releases: Astral Horizon, Midnight Express & Berlin Season 2")
                            }
                        },
                        onSettingsClick = { showSettingsModal = true },
                        onProfileClick = {
                            if (currentUser == null) {
                                showAuthModal = true
                            } else {
                                showProfileModal = true
                            }
                        },
                        onLogoutClick = {
                            authRepository.logout()
                            showAuthModal = true
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Logged out successfully.")
                            }
                        }
                    )
                }

                // Body Area with smooth transitions
                AnimatedContent(
                    targetState = currentNav to currentBottomTab,
                    transitionSpec = {
                        if (targetState.first != initialState.first || targetState.second != initialState.second) {
                            (slideInHorizontally { width -> width / 4 } + fadeIn())
                                .togetherWith(slideOutHorizontally { width -> -width / 4 } + fadeOut())
                        } else {
                            fadeIn().togetherWith(fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    modifier = Modifier.weight(1f),
                    label = "screen_transition"
                ) { (nav, tab) ->
                    when {
                        nav == CineflixNavDestination.DESIGN_SYSTEM -> {
                            DesignSystemScreen(
                                onOpenTrailerModal = { heroItems.firstOrNull()?.let { playMediaItem(it) } },
                                onOpenSubscriptionModal = { showSubscriptionModal = true },
                                onOpenSettingsModal = { showSettingsModal = true },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
    
                        nav == CineflixNavDestination.WATCHLIST -> {
                            WatchlistView(
                                savedItems = allItems.filter { userWatchlist.contains(it.id) },
                                currentUser = currentUser,
                                onOpenAuth = { showAuthModal = true },
                                onLogout = {
                                    authRepository.logout()
                                    showAuthModal = true
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Logged out successfully.")
                                    }
                                },
                                onOpenMedia = playMediaItem,
                                onToggleBookmark = { media ->
                                    sessionManager.toggleWatchlist(media.id)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
    
                        else -> {
                            when (tab) {
                                CineflixBottomTab.HOME -> {
                                    CineflixModernUserView(
                                        liveMovies = allItems,
                                        allCatalogMovies = allItems,
                                        isSyncing = isCatalogSyncing,
                                        syncMessage = catalogSyncMessage,
                                        userWatchlist = userWatchlist,
                                        sessionManager = sessionManager,
                                        onRefreshSync = { catalogManager.fetchCatalogFromSupabase() },
                                        onOpenMedia = playMediaItem,
                                        onPlayMedia = playMediaItem,
                                        onToggleWatchlist = { media ->
                                            val added = sessionManager.toggleWatchlist(media.id)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    if (added) "Saved to Watchlist (Cached Locally)"
                                                    else "Removed from Watchlist"
                                                )
                                            }
                                        },
                                        onOpenSettings = { currentBottomTab = CineflixBottomTab.ME_PROFILE },
                                        onOpenWatchlist = { currentNav = CineflixNavDestination.WATCHLIST },
                                        onOpenPremium = { currentBottomTab = CineflixBottomTab.PREMIUM },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
    
                                CineflixBottomTab.PREMIUM -> {
                                    PremiumScreen(
                                        sessionManager = sessionManager,
                                        onNavigateToHome = { currentBottomTab = CineflixBottomTab.HOME },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
    
                                CineflixBottomTab.DOWNLOADS_HISTORY -> {
                                    DownloadsAndHistoryScreen(
                                        sessionManager = sessionManager,
                                        allCatalogMovies = allItems,
                                        onPlayMedia = playMediaItem,
                                        onBrowseMore = { currentBottomTab = CineflixBottomTab.HOME },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
    
                                CineflixBottomTab.ME_PROFILE -> {
                                    MeAndProfileScreen(
                                        sessionManager = sessionManager,
                                        allCatalogMovies = allItems,
                                        onOpenPremium = { currentBottomTab = CineflixBottomTab.PREMIUM },
                                        onOpenDownloads = { currentBottomTab = CineflixBottomTab.DOWNLOADS_HISTORY },
                                        onOpenWatchlist = { currentNav = CineflixNavDestination.WATCHLIST },
                                        onOpenAdminDashboard = { sessionManager.setAdminMode(true) },
                                        onLogout = {
                                            currentBottomTab = CineflixBottomTab.HOME
                                            showAuthModal = true
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Logged out successfully.")
                                            }
                                        },
                                        onPlayMedia = playMediaItem,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }

                // Always visible Bottom Navigation Bar with 4 buttons:
                // Home, Premium, Downloads & History, Me & Profile
                CineflixBottomBar(
                    currentTab = currentBottomTab,
                    onTabSelected = { tab ->
                        currentBottomTab = tab
                        currentNav = CineflixNavDestination.HOME
                    }
                )
            }
        }

        // Snackbar Host for Notifications
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 70.dp)
        )

        // Modal Dialogs
        activeTrailerMedia?.let { media ->
            TrailerPlayerModal(
                media = media,
                catalogMovies = allItems,
                sessionManager = sessionManager,
                onDismiss = { activeTrailerMedia = null }
            )
        }

        if (showSubscriptionModal) {
            SubscriptionModal(
                onDismiss = { showSubscriptionModal = false }
            )
        }

        if (showSettingsModal) {
            SettingsModal(
                onDismiss = { showSettingsModal = false }
            )
        }

        if (showAuthModal) {
            AuthModal(
                sessionManager = sessionManager,
                authRepository = authRepository,
                onDismiss = { showAuthModal = false },
                onAuthSuccess = { user ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Welcome, ${user.name}! Session cached on device.")
                    }
                }
            )
        }

        if (showProfileModal) {
            currentUser?.let { user ->
                UserProfileModal(
                    user = user,
                    watchlistCount = userWatchlist.size,
                    onLogout = {
                        authRepository.logout()
                        showProfileModal = false
                        showAuthModal = true
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Logged out successfully.")
                        }
                    },
                    onUpgradePlan = { showSubscriptionModal = true },
                    onDismiss = { showProfileModal = false }
                )
            }
        }
    }
}

/**
 * Watchlist view for reading and managing saved favorites offline and online
 */
@Composable
private fun WatchlistView(
    savedItems: List<MediaItem>,
    currentUser: UserProfile?,
    onOpenAuth: () -> Unit,
    onLogout: (() -> Unit)? = null,
    onOpenMedia: (MediaItem) -> Unit,
    onToggleBookmark: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .testTag("watchlist_view")
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1440.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "My Watchlist",
                            style = CineflixTheme.typography.h2,
                            color = Color.White
                        )
                        Text(
                            text = "Saved locally on your device • Instant offline access",
                            style = CineflixTheme.typography.caption,
                            color = CineflixTheme.colors.textSecondary
                        )
                    }

                    if (currentUser == null) {
                        CineflixPrimaryButton(
                            text = "Log In to Sync",
                            onClick = onOpenAuth,
                            testTag = "watchlist_btn_signin"
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QualityBadge(label = "SYNCED")
                            if (onLogout != null) {
                                Box(
                                    modifier = Modifier
                                        .testTag("watchlist_btn_logout")
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0x33FF4C4C))
                                        .border(1.dp, Color(0x66FF4C4C), RoundedCornerShape(16.dp))
                                        .clickable(onClick = onLogout)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
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
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (savedItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CineflixTheme.colors.cardBackground)
                            .border(1.dp, CineflixTheme.colors.cardBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = CineflixTheme.colors.textTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your Watchlist is empty",
                                style = CineflixTheme.typography.h5,
                                color = Color.White
                            )
                            Text(
                                text = "Browse titles and tap the bookmark icon to save",
                                style = CineflixTheme.typography.caption,
                                color = CineflixTheme.colors.textSecondary
                            )
                        }
                    }
                } else {
                    savedItems.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { item ->
                                MoviePosterCard(
                                    media = item,
                                    isBookmarked = true,
                                    onClick = { onOpenMedia(item) },
                                    onPlayClick = { onOpenMedia(item) },
                                    onBookmarkToggle = { onToggleBookmark(item) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Row Header with Title, Subtitle, optional Badge and Prime/Netflix style Scroll Arrows
 */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    onBackArrowClick: (() -> Unit)? = null,
    onNextArrowClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = CineflixTheme.typography.h4,
                    color = Color.White
                )
                if (badge != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    QualityBadge(label = badge)
                }
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = CineflixTheme.typography.caption,
                    color = CineflixTheme.colors.textSecondary
                )
            }
        }

        if (onBackArrowClick != null && onNextArrowClick != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CineflixTheme.colors.cardBackground)
                        .border(1.dp, CineflixTheme.colors.cardBorder, CircleShape)
                        .clickable(onClick = onBackArrowClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Scroll back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CineflixTheme.colors.cardBackground)
                        .border(1.dp, CineflixTheme.colors.cardBorder, CircleShape)
                        .clickable(onClick = onNextArrowClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Scroll forward",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
