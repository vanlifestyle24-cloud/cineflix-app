package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import com.example.auth.SessionManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MediaItem
import com.example.model.MediaType

/**
 * Top Navigation Category Tabs matching the user screenshot:
 * Trending | Movie | TV | TV Channel | Cricket | Short TV
 */
enum class TopCategoryTab(val label: String) {
    TRENDING("Trending"),
    MOVIE("Movie"),
    TV("TV"),
    TV_CHANNEL("TV Channel"),
    CRICKET("Cricket"),
    SHORT_TV("Short TV")
}

/**
 * MovieBox / Cineflix Next-Gen UI
 * Exact replica of the user screenshot with full interactive playback logic,
 * live Telegram OTT streaming integration, VIP badges, number rank overlays,
 * promotional banners, and category browsing.
 */
@Composable
fun CineflixModernUserView(
    liveMovies: List<MediaItem>,
    allCatalogMovies: List<MediaItem>,
    isSyncing: Boolean,
    syncMessage: String,
    userWatchlist: Set<String>,
    sessionManager: SessionManager,
    onRefreshSync: () -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
    onPlayMedia: (MediaItem) -> Unit,
    onToggleWatchlist: (MediaItem) -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenWatchlist: () -> Unit = {},
    onOpenPremium: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTopTab by remember { mutableStateOf(TopCategoryTab.TRENDING) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMovieSubFilter by remember { mutableStateOf("TOP Movies") }
    var selectedTvSubFilter by remember { mutableStateOf("Top Series") }
    var showUnlockModal by remember { mutableStateOf(false) }
    var showStreamTesterModal by remember { mutableStateOf(false) }

    val heroItems = remember(allCatalogMovies) { allCatalogMovies.filter { it.isTop10 }.ifEmpty { allCatalogMovies.take(5) } }
    val autoRotate by sessionManager.carouselAutoRotate.collectAsState()
    val rotateSpeed by sessionManager.carouselRotateSpeed.collectAsState()
    val pagerState = rememberPagerState(pageCount = { heroItems.size })

    // Auto-rotation logic
    LaunchedEffect(autoRotate, rotateSpeed, heroItems.size) {
        if (autoRotate && heroItems.size > 1) {
            while (true) {
                delay((rotateSpeed * 1000).toLong())
                if (pagerState.pageCount > 0) {
                    val nextPagerPage = (pagerState.currentPage + 1) % pagerState.pageCount
                    pagerState.animateScrollToPage(nextPagerPage)
                }
            }
        }
    }
    var customStreamInput by remember {
        mutableStateOf("https://rumble.com/v7fpkry-gtrgtrgrtg.html")
    }
    var customStreamTitle by remember { mutableStateOf("Student of the Year") }

    // Live Rumble movie published from OTT dashboard
    val liveTelegramItem = liveMovies.firstOrNull() ?: MediaItem(
        id = "rumble-live-1",
        title = "Student of the Year",
        tagline = "Direct Rumble Cloud Stream",
        synopsis = "Synced directly from Rumble unlisted video stream.",
        duration = "15m 34s",
        releaseYear = 2026,
        primaryGenre = "Action",
        genres = listOf("Action", "Romance"),
        audioLanguages = listOf("Hindi", "English"),
        videoStreamUrl = "https://rumble.com/v7fpkry-gtrgtrgrtg.html",
        posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&auto=format&fit=crop&q=80",
        backdropUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=1200&auto=format&fit=crop&q=80"
    )

    // Dedicated Rich Item Collections matching the user screenshot
    val featuredHeroItems = remember(liveMovies) {
        listOf(
            MediaItem(
                id = "hero-neagley",
                title = "Neagley",
                tagline = "FROM THE WORLD OF REACHER",
                synopsis = "Frances Neagley investigates a corporate espionage conspiracy across the global defense industry.",
                duration = "48m",
                releaseYear = 2026,
                primaryGenre = "Action",
                genres = listOf("Action", "Crime", "Thriller"),
                audioLanguages = listOf("English [Original]", "Hindi"),
                backdropUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1200&auto=format&fit=crop&q=80",
                posterUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            ),
            MediaItem(
                id = "hero-irumudi",
                title = "Irumudi",
                tagline = "A Sacred Journey of Vengeance",
                synopsis = "A devoted pilgrim must defend his heritage against a syndicate trying to desecrate the sanctuary.",
                duration = "2h 14m",
                releaseYear = 2026,
                primaryGenre = "Action",
                genres = listOf("Action", "Drama"),
                audioLanguages = listOf("Hindi", "Tamil", "Telugu"),
                backdropUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200&auto=format&fit=crop&q=80",
                posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
            ),
            liveTelegramItem
        )
    }

    val trendingMovies = remember(liveMovies) {
        listOf(
            MediaItem(
                id = "mov-1",
                title = "Lust Stories 3",
                tagline = "Passions, Secrets, and Deceit",
                synopsis = "Four distinct Indian filmmakers explore love, modern relationships, and unexpected betrayals.",
                duration = "1h 58m",
                releaseYear = 2026,
                primaryGenre = "Drama",
                genres = listOf("Drama", "Romance"),
                audioLanguages = listOf("Hindi", "English"),
                posterUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=500&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
            ),
            MediaItem(
                id = "mov-2",
                title = "Best of the Best",
                tagline = "Championship of the Decade",
                synopsis = "Two underground martial arts prodigies clash in an arena where only honor survives.",
                duration = "2h 05m",
                releaseYear = 2026,
                primaryGenre = "Action",
                genres = listOf("Action", "Thriller"),
                audioLanguages = listOf("English [Original]", "Hindi"),
                posterUrl = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=500&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4"
            ),
            MediaItem(
                id = "mov-3",
                title = "Irumudi",
                tagline = "Sacred Vow",
                synopsis = "The high-octane devotional thriller taking box office records across India.",
                duration = "2h 14m",
                releaseYear = 2026,
                primaryGenre = "Action",
                genres = listOf("Action", "Drama"),
                audioLanguages = listOf("Hindi", "Tamil", "Telugu"),
                posterUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
            ),
            liveTelegramItem,
            MediaItem(
                id = "mov-4",
                title = "Animal",
                tagline = "A son's obsessive love.",
                synopsis = "A transformative saga of bloodshed and family allegiance.",
                duration = "3h 24m",
                releaseYear = 2024,
                primaryGenre = "Action",
                genres = listOf("Action", "Crime"),
                audioLanguages = listOf("Hindi", "Tamil", "Telugu"),
                posterUrl = "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=500&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
            )
        )
    }

    val trendingTvSeries = remember {
        listOf(
            MediaItem(
                id = "tv-1",
                title = "The Scandal",
                tagline = "Royal Intrigues in Joseon",
                synopsis = "When royal secrets are leaked, palace intrigue turns deadly for everyone involved.",
                duration = "Season 1 • 12 Eps",
                releaseYear = 2026,
                primaryGenre = "Historical Drama",
                genres = listOf("Drama", "Mystery"),
                audioLanguages = listOf("Korean [Original]", "Hindi", "English"),
                posterUrl = "https://images.unsplash.com/photo-1578328819058-b69f3a3b0f6b?w=500&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"
            ),
            MediaItem(
                id = "tv-2",
                title = "Waiting Hai",
                tagline = "Duty, Loyalty, Police Honor",
                synopsis = "Two fearless officers confront corrupt crime lords in Mumbai's underworld.",
                duration = "Season 2 • 8 Eps",
                releaseYear = 2026,
                primaryGenre = "Crime Drama",
                genres = listOf("Crime", "Action"),
                audioLanguages = listOf("Hindi"),
                posterUrl = "https://images.unsplash.com/photo-1533488765986-dfa2a9939acd?w=500&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4"
            ),
            MediaItem(
                id = "tv-3",
                title = "Neagley",
                tagline = "Top Military Investigator",
                synopsis = "Season premiering simultaneously worldwide.",
                duration = "Season 1 • 6 Eps",
                releaseYear = 2026,
                primaryGenre = "Action",
                genres = listOf("Action", "Thriller"),
                audioLanguages = listOf("English [Original]", "Hindi"),
                posterUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            ),
            MediaItem(
                id = "tv-4",
                title = "Mirzapur 3",
                tagline = "Gaddi Par Kaun Baithega?",
                synopsis = "The battle for Purvanchal reaches an unprecedented climax.",
                duration = "Season 3 • 10 Eps",
                releaseYear = 2025,
                primaryGenre = "Crime",
                genres = listOf("Crime", "Thriller"),
                audioLanguages = listOf("Hindi"),
                posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
            )
        )
    }

    val dontMissEnding = remember {
        listOf(
            MediaItem(
                id = "end-1",
                title = "I Became a Legend",
                tagline = "Awakened in a Magical Realm",
                synopsis = "An underdog warrior awakens dormant divine abilities to save his kingdom.",
                duration = "1h 45m",
                releaseYear = 2026,
                primaryGenre = "Anime/Fantasy",
                genres = listOf("Anime", "Fantasy"),
                audioLanguages = listOf("Japanese [Original]", "Hindi", "English"),
                posterUrl = "https://images.unsplash.com/photo-1563089145-599997674d42?w=500&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
            ),
            MediaItem(
                id = "end-2",
                title = "Vishwanath",
                tagline = "14 August Mystery",
                synopsis = "A lone umbrella traveler uncovers an eerie truth on Independence night.",
                duration = "2h 10m",
                releaseYear = 2026,
                primaryGenre = "Thriller",
                genres = listOf("Mystery", "Thriller"),
                audioLanguages = listOf("Hindi"),
                posterUrl = "https://images.unsplash.com/photo-1514306191717-452ec28c7814?w=500&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4"
            ),
            MediaItem(
                id = "end-3",
                title = "Arifureta",
                tagline = "Commonplace to World's Strongest",
                synopsis = "Betrayed into the deepest abyss, an alchemist rises above all gods.",
                duration = "1h 55m",
                releaseYear = 2026,
                primaryGenre = "Anime",
                genres = listOf("Anime", "Action"),
                audioLanguages = listOf("Japanese [Original]", "Hindi", "English"),
                posterUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=500&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
            )
        )
    }

    val wweShows = remember {
        listOf(
            MediaItem(
                id = "wwe-1",
                title = "WWE SmackDown Live",
                tagline = "Friday Night Spectacle",
                synopsis = "Roman Reigns, Cody Rhodes and the superstars clash in high-voltage matches.",
                duration = "2h 15m",
                releaseYear = 2026,
                primaryGenre = "Sports/Entertainment",
                genres = listOf("Action", "Sports"),
                posterUrl = "https://images.unsplash.com/photo-1517649763962-0c623266ddc0?w=500&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
            ),
            MediaItem(
                id = "wwe-2",
                title = "WWE NXT September Live",
                tagline = "The Next Generation",
                synopsis = "The fiercest rising contenders battle for gold and legacy.",
                duration = "1h 45m",
                releaseYear = 2026,
                primaryGenre = "Sports",
                genres = listOf("Sports", "Action"),
                posterUrl = "https://images.unsplash.com/photo-1549719386-74dfcbf7dbed?w=500&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"
            ),
            MediaItem(
                id = "wwe-3",
                title = "WWE RAW Superstars",
                tagline = "Monday Night Live on Netflix",
                synopsis = "World Heavyweight Championship rivalries take center stage.",
                duration = "2h 30m",
                releaseYear = 2026,
                primaryGenre = "Sports",
                genres = listOf("Sports", "Entertainment"),
                posterUrl = "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=500&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4"
            )
        )
    }

    val hotShortTv = remember {
        listOf(
            MediaItem(
                id = "short-1",
                title = "Bhikhari Boss",
                tagline = "Billionaire in Disguise",
                synopsis = "A secret billionaire tests the true intentions of his family.",
                duration = "Ep 1-45",
                releaseYear = 2026,
                primaryGenre = "Short Drama",
                genres = listOf("Drama", "Romance"),
                posterUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            ),
            MediaItem(
                id = "short-2",
                title = "Raj Tilak",
                tagline = "Royal Succession War",
                synopsis = "Family loyalty is shattered when a hidden will comes to light.",
                duration = "Ep 1-60",
                releaseYear = 2026,
                primaryGenre = "Short Drama",
                genres = listOf("Drama", "Suspense"),
                posterUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=500&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
            ),
            MediaItem(
                id = "short-3",
                title = "Golden Boy Ki Kahani",
                tagline = "From Rags to Empire",
                synopsis = "A young street hustler claims his right to the city's largest business empire.",
                duration = "Ep 1-35",
                releaseYear = 2026,
                primaryGenre = "Short Drama",
                genres = listOf("Romance", "Drama"),
                posterUrl = "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=500&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
            )
        )
    }

    val cricketShorts = remember {
        listOf(
            MediaItem(
                id = "cric-1",
                title = "2nd inning highlights - IND vs PAK",
                tagline = "Tense Finish in Colombo",
                synopsis = "Full thriller highlights of the last 4 overs under floodlights.",
                duration = "8m 12s",
                releaseYear = 2026,
                primaryGenre = "Cricket",
                genres = listOf("Sports", "Cricket"),
                posterUrl = "https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?w=500&auto=format&fit=crop&q=80",
                backdropUrl = "https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?w=800&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
            ),
            MediaItem(
                id = "cric-2",
                title = "Match 3 ODI Super Over Moments",
                tagline = "Record Breaking Run Chase",
                synopsis = "Captain's masterclass in the final balls.",
                duration = "6m 45s",
                releaseYear = 2026,
                primaryGenre = "Cricket",
                genres = listOf("Sports", "Cricket"),
                posterUrl = "https://images.unsplash.com/photo-1531415074868-036b1c5c53ec?w=500&auto=format&fit=crop&q=80",
                backdropUrl = "https://images.unsplash.com/photo-1531415074868-036b1c5c53ec?w=800&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
            ),
            MediaItem(
                id = "cric-3",
                title = "WHAT A MOMENT! 6 in Last Ball",
                tagline = "Historic Maximum",
                synopsis = "Crowd erupts as the stadium lights celebrate the win.",
                duration = "4m 20s",
                releaseYear = 2026,
                primaryGenre = "Cricket",
                genres = listOf("Sports", "Cricket"),
                posterUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=500&auto=format&fit=crop&q=80",
                backdropUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=800&auto=format&fit=crop&q=80",
                videoStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"
            )
        )
    }

    // Combined searchable catalog
    val allCatalogCombined = remember(allCatalogMovies, trendingMovies, trendingTvSeries, cricketShorts, hotShortTv, liveTelegramItem) {
        (listOf(liveTelegramItem) + trendingMovies + trendingTvSeries + allCatalogMovies + cricketShorts + hotShortTv).distinctBy { it.id }
    }

    val searchResults = remember(searchQuery, allCatalogCombined) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim().lowercase()
            allCatalogCombined.filter {
                it.title.lowercase().contains(q) ||
                it.primaryGenre.lowercase().contains(q) ||
                it.genres.any { g -> g.lowercase().contains(q) } ||
                it.tagline.lowercase().contains(q)
            }
        }
    }

    val filteredMovies = remember(selectedMovieSubFilter, trendingMovies, allCatalogMovies, liveTelegramItem) {
        val pool = (listOf(liveTelegramItem) + trendingMovies + allCatalogMovies).distinctBy { it.id }
        when (selectedMovieSubFilter) {
            "TOP Movies" -> pool
            "Bollywood" -> pool.filter { it.genres.any { g -> g.contains("Bollywood", ignoreCase = true) || g.contains("Hindi", ignoreCase = true) } || it.title.contains("Hindi") }
            "Hollywood" -> pool.filter { it.genres.any { g -> g.contains("Action", ignoreCase = true) || g.contains("Sci-Fi", ignoreCase = true) } }
            "New Punjabi" -> pool.filter { it.genres.any { g -> g.contains("Comedy", ignoreCase = true) || g.contains("Drama", ignoreCase = true) } }
            "Action" -> pool.filter { it.primaryGenre.contains("Action", ignoreCase = true) }
            else -> pool
        }
    }

    Column(
        modifier = modifier
            .testTag("cineflix_exact_user_view")
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF261D15), // warm golden-amber dark glow
                        Color(0xFF15141D),
                        Color(0xFF0E0E14),
                        Color(0xFF0A0A0F)
                    )
                )
            )
    ) {
        // Sticky Frosted Top Header with proper System Status Bar Padding
        // This ensures the header is pushed safely BELOW the Android status bar (clock 4:26, wifi, battery & camera cutout)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xF513121C),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF231A12), // matching top ambient glow
                                Color(0xFF171520),
                                Color(0xFF12111A)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(top = 4.dp, bottom = 4.dp)
            ) {
                // 1. Top App Bar & Search (Logo + Pill Search + Stream Tester + Search button)
                MovieBoxTopBar(
                    searchQuery = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {
                        val clean = searchQuery.trim()
                        if (clean.isNotBlank()) {
                            val matching = allCatalogCombined.find {
                                it.title.contains(clean, ignoreCase = true)
                            }
                            if (matching != null) onOpenMedia(matching)
                        }
                    },
                    onOpenSettings = onOpenSettings
                )

                // 2. Horizontal Category Tabs (Trending | Movie | TV | TV Channel | Cricket | Short TV)
                MovieBoxCategoryTabs(
                    selectedTab = selectedTopTab,
                    onTabSelected = {
                        selectedTopTab = it
                        searchQuery = ""
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
        }

        // Scrollable Content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // 3. SEARCH RESULTS MODE (Active when user typed in search bar)
        if (searchQuery.isNotBlank()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Results for \"$searchQuery\" (${searchResults.size})",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Clear",
                        color = Color(0xFF00E5FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { searchQuery = "" }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (searchResults.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No movies found matching \"$searchQuery\"",
                            color = Color(0xFFA6A6B8),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 Try searching for 'Student', 'Hindi', 'Neagley', or click the 🔗 button on top to paste a Rumble stream link.",
                            color = Color(0xFF88889C),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val chunkedResults = searchResults.chunked(3)
                items(chunkedResults) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (item in rowItems) {
                            Box(modifier = Modifier.weight(1f)) {
                                MovieBoxPosterCardNoRank(
                                    item = item,
                                    onPlay = { onPlayMedia(item) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        for (i in 0 until (3 - rowItems.size)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            // NORMAL TAB-BASED DISPLAY
            when (selectedTopTab) {
                TopCategoryTab.TRENDING -> {
                    // Hero Billboard with Auto-Rotating Pager
                    if (heroItems.isNotEmpty()) {
                        item {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth()
                            ) { page ->
                                val currentHero = heroItems[page]
                                MovieBoxHeroBillboard(
                                    heroItem = currentHero,
                                    featuredMiniItems = heroItems,
                                    onPlayItem = { onPlayMedia(it) },
                                    onSelectItem = { onOpenMedia(it) }
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    // Promo Banner 1: "Unlock Premium Benefits" with Urdu ribbon
                    item {
                        UnlockPremiumBanner(
                            onUnlockClick = { onOpenPremium() }
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                    }

                    // Trending Movies Section (with sub-filter chips & numbers 1, 2, 3...)
                    item {
                        SectionTitleWithAllLink(
                            title = "Trending Movies",
                            onAllClick = { selectedTopTab = TopCategoryTab.MOVIE }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FilterChipsRow(
                            chips = listOf("TOP Movies", "Cinema", "New Punjabi", "Bollywood", "Hollywood"),
                            selectedChip = selectedMovieSubFilter,
                            onChipSelect = { selectedMovieSubFilter = it }
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val displayedMovies = if (selectedMovieSubFilter == "TOP Movies") trendingMovies
                        else trendingMovies.filter { it.primaryGenre.contains("Action") }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(displayedMovies) { index, item ->
                                MovieBoxRankPosterCard(
                                    item = item,
                                    rankNumber = index + 1,
                                    onPlay = { onPlayMedia(item) },
                                    modifier = Modifier.width(110.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Trending TV Series Section
                    item {
                        SectionTitleWithAllLink(
                            title = "Trending TV Series",
                            onAllClick = { selectedTopTab = TopCategoryTab.TV }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FilterChipsRow(
                            chips = listOf("Top Series", "Western TV", "Indian Drama", "Reality-TV"),
                            selectedChip = selectedTvSubFilter,
                            onChipSelect = { selectedTvSubFilter = it }
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(trendingTvSeries) { index, item ->
                                MovieBoxRankPosterCard(
                                    item = item,
                                    rankNumber = index + 1,
                                    onPlay = { onPlayMedia(item) },
                                    modifier = Modifier.width(110.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Categories Section
                    item {
                        SectionHeaderSimple(title = "Categories")
                        Spacer(modifier = Modifier.height(10.dp))

                        CategoryCardsRow(
                            categories = listOf("All", "All Movies", "All Dramas", "Punjabi", "Korean", "Anime"),
                            onSelectCategory = { cat ->
                                when (cat) {
                                    "All Movies" -> selectedTopTab = TopCategoryTab.MOVIE
                                    "All Dramas" -> selectedTopTab = TopCategoryTab.TV
                                    "Punjabi" -> {
                                        selectedMovieSubFilter = "New Punjabi"
                                        selectedTopTab = TopCategoryTab.MOVIE
                                    }
                                    "Korean" -> selectedTopTab = TopCategoryTab.TV
                                    "Anime" -> {
                                        selectedMovieSubFilter = "Cinema"
                                        selectedTopTab = TopCategoryTab.MOVIE
                                    }
                                    else -> selectedTopTab = TopCategoryTab.TRENDING
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Don't Miss the Ending Section
                    item {
                        SectionHeaderSimple(title = "Don't Miss the Ending")
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(dontMissEnding) { item ->
                                MovieBoxPosterCardNoRank(
                                    item = item,
                                    onPlay = { onPlayMedia(item) },
                                    modifier = Modifier.width(110.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Netflix WWE Live & Replay Section
                    item {
                        SectionHeaderSimple(title = "Netflix WWE Live & Replay")
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(wweShows) { item ->
                                MovieBoxPosterCardNoRank(
                                    item = item,
                                    onPlay = { onPlayMedia(item) },
                                    modifier = Modifier.width(110.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // 🔥Hot Short TV Section
                    item {
                        SectionTitleWithAllLink(
                            title = "🔥Hot Short TV",
                            onAllClick = { selectedTopTab = TopCategoryTab.SHORT_TV }
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(hotShortTv) { item ->
                                MovieBoxPosterCardNoRank(
                                    item = item,
                                    onPlay = { onPlayMedia(item) },
                                    modifier = Modifier.width(110.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // 🔥 2025 Must-Watch Section
                    item {
                        SectionHeaderSimple(title = "🔥 2025 Must-Watch👁️")
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                MustWatchCollageCard(
                                    title = "TOP 20 South Asia\nOn Screen",
                                    imageUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
                                    onClick = { onPlayMedia(trendingMovies[0]) }
                                )
                            }
                            item {
                                MustWatchCollageCard(
                                    title = "TOP 20\nForeign Movies",
                                    imageUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=600&auto=format&fit=crop&q=80",
                                    onClick = { onPlayMedia(trendingMovies[1]) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Cricket Viral Shorts Section
                    item {
                        SectionHeaderSimple(title = "Cricket Viral Shorts")
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(cricketShorts) { item ->
                                CricketShortCard(
                                    item = item,
                                    onPlay = { onPlayMedia(item) },
                                    modifier = Modifier.width(150.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Coming Soon Section
                    item {
                        SectionHeaderSimple(title = "Coming Soon")
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(heroItems) { item ->
                                MovieBoxPosterCardNoRank(
                                    item = item,
                                    onPlay = { onPlayMedia(item) },
                                    modifier = Modifier.width(110.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                TopCategoryTab.MOVIE -> {
                    // Dedicated Movies Catalog
                    item {
                        SectionTitleWithAllLink(
                            title = "All Movies & Blockbusters",
                            onAllClick = {}
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FilterChipsRow(
                            chips = listOf("TOP Movies", "Cinema", "New Punjabi", "Bollywood", "Hollywood", "Action"),
                            selectedChip = selectedMovieSubFilter,
                            onChipSelect = { selectedMovieSubFilter = it }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    val chunked = filteredMovies.chunked(3)
                    items(chunked) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (item in rowItems) {
                                Box(modifier = Modifier.weight(1f)) {
                                    MovieBoxPosterCardNoRank(
                                        item = item,
                                        onPlay = { onPlayMedia(item) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            for (i in 0 until (3 - rowItems.size)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }

                TopCategoryTab.TV -> {
                    // Dedicated TV Series Catalog
                    item {
                        SectionTitleWithAllLink(
                            title = "TV Series & Web Dramas",
                            onAllClick = {}
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FilterChipsRow(
                            chips = listOf("Top Series", "Western TV", "Indian Drama", "Reality-TV"),
                            selectedChip = selectedTvSubFilter,
                            onChipSelect = { selectedTvSubFilter = it }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    val chunked = trendingTvSeries.chunked(3)
                    items(chunked) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (item in rowItems) {
                                Box(modifier = Modifier.weight(1f)) {
                                    MovieBoxPosterCardNoRank(
                                        item = item,
                                        onPlay = { onPlayMedia(item) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            for (i in 0 until (3 - rowItems.size)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }

                TopCategoryTab.TV_CHANNEL -> {
                    // Live TV & Rumble Cloud Channels
                    item {
                        SectionHeaderSimple(title = "Live TV & Rumble Cloud Streams")
                        Spacer(modifier = Modifier.height(12.dp))

                        // Featured: Student of the Year Rumble Cloud Stream Live Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .clickable { onPlayMedia(liveTelegramItem) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B26)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF00E676))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "RUMBLE CLOUD FEED (LIVE)",
                                            color = Color(0xFF00E676),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Surface(
                                        color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "4K ULTRA HD",
                                            color = Color(0xFF00E5FF),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = liveTelegramItem.title,
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Host Server: Direct Streaming Mode | Unlisted URL synced",
                                    color = Color(0xFFA6A6B8),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { onPlayMedia(liveTelegramItem) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00E676),
                                        contentColor = Color(0xFF002B18)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "▶ Stream 'Student of the Year' Now",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Other Live 24/7 Channels
                    item {
                        Text(
                            text = "24/7 Satellite & OTT Live Channels",
                            color = Color(0xFFD1D1DE),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val liveChannels = listOf(
                            Triple("Cineflix Cinema HD", "24/7 Non-stop Bollywood & Action", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&auto=format&fit=crop&q=80"),
                            Triple("Sports Live 1 HD", "Live Cricket, WWE & Football Feeds", "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=600&auto=format&fit=crop&q=80"),
                            Triple("Discovery & Wildlife HD", "Nature, Science & World Documentaries", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80")
                        )

                        for (ch in liveChannels) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                                    .clickable { onPlayMedia(trendingMovies[0]) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF14141F))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF232336)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LiveTv,
                                            contentDescription = "Channel",
                                            tint = Color(0xFF00E5FF)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = ch.first, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = ch.second, color = Color(0xFFA0A0B2), fontSize = 11.5.sp)
                                    }
                                    Text(
                                        text = "LIVE",
                                        color = Color(0xFF00E676),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                TopCategoryTab.CRICKET -> {
                    // Dedicated Cricket View
                    item {
                        SectionHeaderSimple(title = "Cricket Viral Shorts & Match Highlights")
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    items(cricketShorts) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .clickable { onPlayMedia(item) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161622))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(
                                        model = item.posterUrl,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0x33000000)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 2)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = item.synopsis, color = Color(0xFFA0A0B2), fontSize = 11.5.sp, maxLines = 2)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Duration: ${item.duration}", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }

                TopCategoryTab.SHORT_TV -> {
                    // Dedicated Short TV View
                    item {
                        SectionTitleWithAllLink(
                            title = "Hot Short TV Mini-Series",
                            onAllClick = {}
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    val chunked = hotShortTv.chunked(3)
                    items(chunked) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (item in rowItems) {
                                Box(modifier = Modifier.weight(1f)) {
                                    MovieBoxPosterCardNoRank(
                                        item = item,
                                        onPlay = { onPlayMedia(item) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            for (i in 0 until (3 - rowItems.size)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
    }

    // Modal: Unlock Premium Benefits Dialog
    if (showUnlockModal) {
        AlertDialog(
            onDismissRequest = { showUnlockModal = false },
            confirmButton = {
                TextButton(onClick = { showUnlockModal = false }) {
                    Text("Upgrade Now", color = Color(0xFFF5C518), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockModal = false }) {
                    Text("Close", color = Color.White)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "VIP",
                        tint = Color(0xFFF5C518)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock Premium VIP", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("✨ 100% Ad-Free Streaming Experience", color = Color(0xFFD1D1DE), fontSize = 13.sp)
                    Text("✨ 4K Ultra HD & Dolby Atmos Audio", color = Color(0xFFD1D1DE), fontSize = 13.sp)
                    Text("✨ High-Speed Dedicated Rumble Streaming CDN", color = Color(0xFFD1D1DE), fontSize = 13.sp)
                    Text("✨ Direct Cloud Playback & Multi-Screen Access", color = Color(0xFFD1D1DE), fontSize = 13.sp)
                }
            },
            containerColor = Color(0xFF1B1B26),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal: Stream Link / YouTube Video ID Tester Dialog
    if (showStreamTesterModal) {
        AlertDialog(
            onDismissRequest = { showStreamTesterModal = false },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedInput = customStreamInput.trim()
                        val mediaToPlay = MediaItem(
                            id = "custom-stream-${System.currentTimeMillis()}",
                            title = customStreamTitle.ifBlank { "Rumble Stream Video" },
                            tagline = "Direct Custom Stream Feed",
                            synopsis = "Streaming directly from unlisted Rumble source.",
                            duration = "Live",
                            releaseYear = 2026,
                            primaryGenre = "Stream",
                            genres = listOf("Rumble", "Direct", "Live 4K"),
                            videoStreamUrl = trimmedInput,
                            posterUrl = liveTelegramItem.posterUrl,
                            backdropUrl = liveTelegramItem.backdropUrl
                        )
                        showStreamTesterModal = false
                        onPlayMedia(mediaToPlay)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252),
                        contentColor = Color.White
                    )
                ) {
                    Text("▶ Play in ExoPlayer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStreamTesterModal = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Stream Link",
                        tint = Color(0xFFFF5252)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rumble Stream Link Tester", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "💡 Rumble Unlisted Video URL ya Direct Video Link yahan daal kar test karein:",
                        color = Color(0xFFD1D1DE),
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = customStreamTitle,
                        onValueChange = { customStreamTitle = it },
                        label = { Text("Title (e.g. Inception / Student of the Year)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF5252),
                            unfocusedBorderColor = Color(0x55FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customStreamInput,
                        onValueChange = { customStreamInput = it },
                        label = { Text("Rumble Unlisted URL / MP4 Stream") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0x55FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            customStreamTitle = "Student of the Year"
                            customStreamInput = "https://rumble.com/v7fpkry-gtrgtrgrtg.html"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242436)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Paste Sample Rumble Unlisted Video", color = Color(0xFF00E5FF), fontSize = 12.sp)
                    }
                }
            },
            containerColor = Color(0xFF1B1B26),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Top App Bar: App Icon + Translucent Pill Search Box + Search Action
 */
@Composable
private fun MovieBoxTopBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search Input Pill (with subtle translucent frosted glass)
        Row(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x33FFFFFF))
                .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xCCFFFFFF),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Search movies, Student of Year...",
                        color = Color(0x88FFFFFF),
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(Color(0xFF00E676)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Search Action Button
        Text(
            text = "Search",
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable { onSearch() }
                .padding(horizontal = 4.dp, vertical = 6.dp)
        )
    }
}

/**
 * Top Categories Tab Row: Trending | Movie | TV | TV Channel | Cricket | Short TV
 */
@Composable
private fun MovieBoxCategoryTabs(
    selectedTab: TopCategoryTab,
    onTabSelected: (TopCategoryTab) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(TopCategoryTab.values()) { tab ->
            val isSelected = tab == selectedTab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    text = tab.label,
                    color = if (isSelected) Color.White else Color(0x99FFFFFF),
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .background(Color.White, RoundedCornerShape(1.dp))
                    )
                } else {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

/**
 * Hero Billboard Banner with Featured Floating Mini-Items
 */
@Composable
private fun MovieBoxHeroBillboard(
    heroItem: MediaItem,
    featuredMiniItems: List<MediaItem>,
    onPlayItem: (MediaItem) -> Unit,
    onSelectItem: (MediaItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        // Hero Background Artwork
        AsyncImage(
            model = heroItem.backdropUrl ?: heroItem.posterUrl,
            contentDescription = heroItem.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient Vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x66000000),
                            Color.Transparent,
                            Color(0xF00D0D12)
                        )
                    )
                )
        )

        // Title text in yellow/gold (like "NEAGLEY")
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 20.dp)
        ) {
            Text(
                text = "FROM THE WORLD OF REACHER",
                color = Color(0xCCFFFFFF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "NEAGLEY",
                color = Color(0xFFFFCC00),
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        // White Pagination Dot in center
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color(0xEEFFFFFF))
        )

        // Floating Horizontal Mini-Card Carousel right at bottom of hero
        LazyRow(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 8.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(featuredMiniItems) { item ->
                MovieBoxMiniHeroCard(
                    item = item,
                    onPlay = { onPlayItem(item) },
                    onClick = { onSelectItem(item) }
                )
            }
        }
    }
}

/**
 * Mini floating hero preview card (e.g. "Neagley [Hindi]", 📅 2026 | Action, bright green play button)
 */
@Composable
private fun MovieBoxMiniHeroCard(
    item: MediaItem,
    onPlay: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xCC161622),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
        modifier = Modifier
            .width(220.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Square poster
            AsyncImage(
                model = item.posterUrl ?: item.backdropUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "📅 ${item.releaseYear} | ${item.primaryGenre}",
                    color = Color(0xFFA0A0B2),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Bright Green Circular Play Button
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00D26A))
                    .clickable { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Banner 1: "Unlock Premium Benefits" with top Urdu script ribbon
 */
@Composable
private fun UnlockPremiumBanner(
    onUnlockClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        // Green top ribbon with Urdu script
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(Color(0xFF1B6B2F))
                .padding(vertical = 4.dp, horizontal = 12.dp)
        ) {
            Text(
                text = "فون جیتنے کے لیے پریمیم پر کلک کریں",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Gold gradient card body
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF4A3916), Color(0xFFC79E3A), Color(0xFFE5BE64))
                    ),
                    RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Gold shield icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x44000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "Premium",
                            tint = Color(0xFFFFE680),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Unlock Premium Benefits",
                            color = Color(0xFF1C1300),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "No ads · HD · Premium Contents",
                            color = Color(0xFF3B2E0F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Unlock Pill Button
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .clickable { onUnlockClick() }
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = "Unlock",
                        color = Color(0xFF221703),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Banner 2: "Watch on TV & Web" (Cyan/Teal gradient with Download TV button)
 */
@Composable
private fun WatchOnTvAndWebBanner(
    onDownloadClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        Text(
            text = "Watch on TV & Web",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF007499), Color(0xFF00A2C7), Color(0xFF59D4F2))
                    ),
                    RoundedCornerShape(10.dp)
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Megaphone / TV icon
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "TV",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "Watch on TV & Web",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Bigger screen, one premium account.",
                            color = Color(0xE6FFFFFF),
                            fontSize = 10.sp
                        )
                        Text(
                            text = "🌐 Official website: https://movieboxhd.net",
                            color = Color(0xCCFFFFFF),
                            fontSize = 9.sp
                        )
                    }
                }

                // Download TV Pill Button (Bright green)
                Surface(
                    color = Color(0xFF00D26A),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .clickable { onDownloadClick() }
                        .padding(start = 6.dp)
                ) {
                    Text(
                        text = "Download TV",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Filter Chips Row (e.g. TOP Movies, Cinema, New Punjabi, Bollywood)
 */
@Composable
private fun FilterChipsRow(
    chips: List<String>,
    selectedChip: String,
    onChipSelect: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chips) { chip ->
            val isSelected = chip == selectedChip
            Surface(
                color = if (isSelected) Color(0xFF282834) else Color(0xFF14141E),
                shape = RoundedCornerShape(14.dp),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFFFFF)) else null,
                modifier = Modifier.clickable { onChipSelect(chip) }
            ) {
                Text(
                    text = chip,
                    color = if (isSelected) Color.White else Color(0xAAFFFFFF),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Poster Card with VIP Crown, Hindi badge, and Giant Outline Rank Number
 */
@Composable
private fun MovieBoxRankPosterCard(
    item: MediaItem,
    rankNumber: Int,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable { onPlay() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B26))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Poster image
                AsyncImage(
                    model = item.posterUrl ?: item.backdropUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Bottom Right: Giant Stylized Rank Number (1, 2, 3...)
                Text(
                    text = "$rankNumber",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 4.dp, bottom = 0.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Title below
        Text(
            text = item.title,
            color = Color(0xFFE0E0E6),
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Standard Poster Card with VIP Crown and Hindi badge (without big number)
 */
@Composable
private fun MovieBoxPosterCardNoRank(
    item: MediaItem,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable { onPlay() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B26))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = item.posterUrl ?: item.backdropUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.title,
            color = Color(0xFFE0E0E6),
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Category Cards Row (All, All Movies, All Dramas, Punjabi, Korean, Anime)
 */
@Composable
private fun CategoryCardsRow(
    categories: List<String>,
    onSelectCategory: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { cat ->
            Surface(
                color = Color(0xFF1F1F2B),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
                modifier = Modifier.clickable { onSelectCategory(cat) }
            ) {
                Text(
                    text = cat,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * Must-Watch Collage Card (e.g. "TOP 20 South Asia On Screen")
 */
@Composable
private fun MustWatchCollageCard(
    title: String,
    imageUrl: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(110.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A26))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x33000000), Color(0xEE0D0D12))
                        )
                    )
            )

            // Title in center bottom with gold brackets
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⟨ $title ⟩",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Cricket Short 16:9 Card with Play overlay
 */
@Composable
private fun CricketShortCard(
    item: MediaItem,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.clickable { onPlay() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161622))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = item.backdropUrl ?: item.posterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Play icon in center
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color(0xAA000000), RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(text = item.duration, color = Color.White, fontSize = 9.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.title,
            color = Color(0xFFD5D5DE),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Section Header with "All >" clickable link on the right
 */
@Composable
private fun SectionTitleWithAllLink(
    title: String,
    onAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onAllClick() }
        ) {
            Text(
                text = "All",
                color = Color(0xAAFFFFFF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "All",
                tint = Color(0xAAFFFFFF),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Simple Section Header
 */
@Composable
private fun SectionHeaderSimple(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 14.dp)
    )
}
