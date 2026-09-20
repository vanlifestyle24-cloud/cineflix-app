package com.example.model

import androidx.annotation.DrawableRes
import com.example.R

enum class MediaType {
    MOVIE, TV_SERIES, LIVE_SPECIAL
}

enum class StreamingTier(val label: String, val priceMonthly: String, val priceAnnual: String, val resolution: String, val devices: Int) {
    MOBILE("Mobile", "₹149/mo", "₹1,490/yr", "720p HD", 1),
    STANDARD("Super Standard", "₹499/mo", "₹4,990/yr", "1080p FHD", 2),
    PREMIUM_4K("Ultra Premium 4K", "₹799/mo", "₹7,990/yr", "4K UHD + Dolby Atmos", 4)
}

data class MediaItem(
    val id: String,
    val title: String,
    val tagline: String,
    val synopsis: String,
    val type: MediaType = MediaType.MOVIE,
    @DrawableRes val backdropRes: Int? = null,
    val backdropUrl: String? = null,
    val posterUrl: String? = null,
    val primaryGenre: String,
    val genres: List<String>,
    val releaseYear: Int,
    val duration: String,
    val ageRating: String = "U/A 16+",
    val imdbRating: Double = 8.6,
    val matchScore: Int = 97,
    val qualityBadges: List<String> = listOf("4K UHD", "HDR10+", "Dolby Atmos", "5.1"),
    val audioLanguages: List<String> = listOf("English [Original]", "Hindi", "Tamil", "Telugu", "Spanish"),
    val subtitleLanguages: List<String> = listOf("English [CC]", "Hindi", "Spanish", "French"),
    val cast: List<String> = listOf("Alex Rivera", "Elena Vance", "Marcus Vance"),
    val creator: String = "Christopher Nolan",
    val platformBadge: String = "CINEFLIX ORIGINAL",
    val isTop10: Boolean = false,
    val top10Rank: Int? = null,
    val watchProgressPercent: Float? = null,
    val isBookmarked: Boolean = false,
    val videoStreamUrl: String? = null,
    val youtubeVideoId: String? = null,
    val telegramFileId: String? = null,
    val parentSeriesId: String? = null,
    val skipIntroStartSec: Int? = null,
    val skipIntroEndSec: Int? = null,
    val languageLinks: Map<String, String> = emptyMap() // Map of Language Label -> Stream URL
)

object MockMediaRepository {
    val sampleHeroItems = listOf(
        MediaItem(
            id = "hero-1",
            title = "ASTRAL HORIZON",
            tagline = "Beyond the edge of human comprehension lies our destiny.",
            synopsis = "When an unexplained gravitational anomaly opens on the outer edge of Saturn, an elite multinational crew undertakes humanity's most perilous deep-space voyage to prevent the collapse of planetary magnetic fields.",
            type = MediaType.MOVIE,
            backdropRes = R.drawable.img_hero_scifi,
            primaryGenre = "Sci-Fi Epic",
            genres = listOf("Sci-Fi", "Adventure", "Drama", "IMAX"),
            releaseYear = 2026,
            duration = "2h 48m",
            ageRating = "U/A 16+",
            imdbRating = 9.2,
            matchScore = 99,
            qualityBadges = listOf("4K UHD", "HDR10+", "Dolby Atmos", "IMAX Enhanced"),
            platformBadge = "CINEFLIX ORIGINAL",
            isTop10 = true,
            top10Rank = 1
        ),
        MediaItem(
            id = "hero-2",
            title = "NEON SYNDICATE: TOKYO 2088",
            tagline = "In a city without shadows, secrets kill faster than bullets.",
            synopsis = "A rogue cybernetic investigator uncovers a clandestine digital syndicate threatening to overwrite human consciousness across Neo-Shibuya's megacity grid.",
            type = MediaType.TV_SERIES,
            backdropRes = R.drawable.img_hero_action,
            primaryGenre = "Cyberpunk Thriller",
            genres = listOf("Action", "Cyberpunk", "Neo-Noir", "Crime"),
            releaseYear = 2026,
            duration = "Season 1 • 8 Episodes",
            ageRating = "18+",
            imdbRating = 8.8,
            matchScore = 96,
            qualityBadges = listOf("4K UHD", "Dolby Vision", "Spatial Audio"),
            platformBadge = "HOTSTAR SPECIAL",
            isTop10 = true,
            top10Rank = 2
        )
    )

    val allMediaItems = listOf(
        sampleHeroItems[0],
        sampleHeroItems[1],
        MediaItem(
            id = "row-1",
            title = "THE QUANTUM HEIST",
            tagline = "Steal from time itself.",
            synopsis = "An elite squad of physicists and rogue operatives attempt to infiltrate an unbreachable subterranean vault that exists across two parallel timeline realities simultaneously.",
            type = MediaType.MOVIE,
            backdropRes = R.drawable.img_hero_scifi,
            primaryGenre = "Action Sci-Fi",
            genres = listOf("Sci-Fi", "Action", "Suspense"),
            releaseYear = 2025,
            duration = "2h 15m",
            ageRating = "13+",
            imdbRating = 8.4,
            matchScore = 94,
            qualityBadges = listOf("4K UHD", "HDR"),
            platformBadge = "PRIME EXCLUSIVE",
            isTop10 = true,
            top10Rank = 3,
            watchProgressPercent = 0.65f
        ),
        MediaItem(
            id = "row-2",
            title = "MIDNIGHT EXPRESS: BERLIN",
            tagline = "One train. Seven assassins. Zero stops.",
            synopsis = "A high-speed bullet train crossing central Europe becomes a lethal battleground when multiple rival operatives discover they're all targeting the same passenger.",
            type = MediaType.MOVIE,
            backdropRes = R.drawable.img_hero_action,
            primaryGenre = "Action Thriller",
            genres = listOf("Action", "Thriller", "Mystery"),
            releaseYear = 2025,
            duration = "1h 56m",
            ageRating = "16+",
            imdbRating = 8.1,
            matchScore = 91,
            qualityBadges = listOf("Full HD", "5.1 Surround"),
            platformBadge = "JIOCINEMA MAX",
            isTop10 = true,
            top10Rank = 4,
            watchProgressPercent = 0.35f
        ),
        MediaItem(
            id = "row-3",
            title = "SHADOW PROTOCOL",
            tagline = "Trust is a fatal luxury.",
            synopsis = "Deep-cover intelligence operative finds her entire agency compromised after an encrypted satellite network goes dark over Eastern Europe.",
            type = MediaType.TV_SERIES,
            backdropRes = R.drawable.img_hero_action,
            primaryGenre = "Espionage",
            genres = listOf("Espionage", "Drama", "Action"),
            releaseYear = 2026,
            duration = "Season 2 • 10 Episodes",
            ageRating = "18+",
            imdbRating = 8.9,
            matchScore = 98,
            qualityBadges = listOf("4K UHD", "Dolby Atmos"),
            platformBadge = "CINEFLIX ORIGINAL",
            isTop10 = true,
            top10Rank = 5
        ),
        MediaItem(
            id = "row-4",
            title = "ECLIPSE OF THE GODS",
            tagline = "Ancient prophecies awake in the modern era.",
            synopsis = "Archaeologists unearth a subterranean temple beneath the Mediterranean Sea, triggering cosmic phenomena that threaten the solar equilibrium.",
            type = MediaType.MOVIE,
            backdropRes = R.drawable.img_hero_scifi,
            primaryGenre = "Fantasy Adventure",
            genres = listOf("Fantasy", "Adventure", "Mythology"),
            releaseYear = 2025,
            duration = "2h 32m",
            ageRating = "U/A 13+",
            imdbRating = 8.5,
            matchScore = 93,
            qualityBadges = listOf("4K UHD", "Dolby Vision"),
            platformBadge = "HOTSTAR SPECIAL",
            isTop10 = true,
            top10Rank = 6
        ),
        MediaItem(
            id = "row-5",
            title = "VALLEY OF ECHOES",
            tagline = "Some mysteries refuse to stay buried.",
            synopsis = "A secluded alpine village encounters inexplicable acoustic phenomena echoing from glacial caverns, unlocking forgotten local folklore.",
            type = MediaType.TV_SERIES,
            backdropRes = R.drawable.img_hero_scifi,
            primaryGenre = "Psychological Mystery",
            genres = listOf("Mystery", "Drama", "Horror"),
            releaseYear = 2025,
            duration = "Mini-Series • 6 Episodes",
            ageRating = "16+",
            imdbRating = 8.7,
            matchScore = 95,
            qualityBadges = listOf("4K UHD", "HDR10"),
            platformBadge = "PRIME EXCLUSIVE"
        ),
        MediaItem(
            id = "row-6",
            title = "CHRONO RACER: ZERO",
            tagline = "Speed is nothing without timing.",
            synopsis = "Underground street racers weaponize localized time dilation fields in an adrenaline-charged championship across neon megacities.",
            type = MediaType.MOVIE,
            backdropRes = R.drawable.img_hero_action,
            primaryGenre = "Action Sci-Fi",
            genres = listOf("Action", "Sci-Fi", "Racing"),
            releaseYear = 2026,
            duration = "1h 48m",
            ageRating = "U/A 16+",
            imdbRating = 7.9,
            matchScore = 89,
            qualityBadges = listOf("4K UHD", "Dolby Atmos"),
            platformBadge = "JIOCINEMA MAX"
        )
    )

    val genresList = listOf(
        "All Genres", "Trending", "Sci-Fi", "Action Thriller", "Cyberpunk",
        "Originals", "Top 10", "4K UHD", "Family", "Drama"
    )
}
