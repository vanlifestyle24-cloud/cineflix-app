package com.example.omdb

import android.content.Context
import android.util.Log
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.MockMediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class CacheSource(val label: String, val badgeText: String) {
    LOCAL_CACHE("Local Device Memory", "LOCAL CACHE (0 CALLS)"),
    SUPABASE_CACHE("Supabase Database Cache", "SUPABASE CACHE (0 OMDB CALLS)"),
    OMDB_PRIMARY("OMDb Primary Key (38df43c1)", "OMDB LIVE API"),
    OMDB_BACKUP("OMDb Backup Key (71610ba3)", "OMDB BACKUP KEY"),
    OFFLINE_CATALOG("Offline Verified Catalog", "OFFLINE CATALOG")
}

data class MovieComment(
    val id: String,
    val author: String,
    val text: String,
    val timeAgo: String = "1h ago",
    val rating: Double = 5.0,
    val likes: Int = 12,
    val isUserPosted: Boolean = false,
    val badge: String? = null
)

data class OmdbSearchResult(
    val items: List<MediaItem>,
    val source: CacheSource,
    val statusMessage: String,
    val isRateLimited: Boolean = false,
    val totalFound: Int = items.size
)

/**
 * High-Performance Caching Repository for OMDb & Supabase
 *
 * Multi-Tier Rate-Limit Protection Architecture:
 * 1. Tier 1: Local Device In-Memory & SharedPreferences Cache (0 Network calls)
 * 2. Tier 2: Dedicated Supabase Postgres Database Cache (irhltbeyztlvscyurbfb)
 *            If a movie or query was previously searched by any user, it's served
 *            directly from Supabase without exhausting OMDb quotas!
 * 3. Tier 3: OMDb Live API Dual-Key Failover:
 *            - Primary Key: 38df43c1
 *            - Backup Key:  71610ba3
 *            Automatically writes new discoveries to Supabase & Local Cache.
 * 4. Tier 4: Rate-limit and offline graceful degradation fallback.
 */
class OmdbRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences("cineflix_omdb_cache", Context.MODE_PRIVATE)
    private val memoryCache = ConcurrentHashMap<String, List<MediaItem>>()

    private val _movieComments = MutableStateFlow<List<MovieComment>>(emptyList())
    val movieComments: StateFlow<List<MovieComment>> = _movieComments.asStateFlow()

    companion object {
        private const val TAG = "OmdbRepository"

        // Dedicated Supabase Account for OMDb Caching
        const val SUPABASE_URL = "https://irhltbeyztlvscyurbfb.supabase.co"
        const val SUPABASE_ANON_KEY = "sb_publishable_xqMIfnYY3GsQGscC9LX2hw_NoE1v1qy"

        // OMDb Dual API Keys
        const val OMDB_KEY_PRIMARY = "38df43c1"
        const val OMDB_KEY_BACKUP = "71610ba3"
    }

    /**
     * Search movies with multi-tier caching and dual-key failover
     */
    suspend fun searchMovies(query: String): OmdbSearchResult = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return@withContext OmdbSearchResult(
                items = emptyList(),
                source = CacheSource.LOCAL_CACHE,
                statusMessage = "Empty search query"
            )
        }

        val cacheKey = cleanQuery.lowercase()

        // 1. Tier 1: Check Local In-Memory Cache
        val inMemory = memoryCache[cacheKey]
        if (!inMemory.isNullOrEmpty()) {
            Log.d(TAG, "Tier 1 HIT: Found '${cleanQuery}' in memory cache (${inMemory.size} items)")
            return@withContext OmdbSearchResult(
                items = inMemory,
                source = CacheSource.LOCAL_CACHE,
                statusMessage = "Retrieved from Local Device Cache (0 Network Calls)"
            )
        }

        // 2. Tier 2: Check Supabase Dedicated Database Cache
        try {
            val supabaseResults = querySupabaseCache(cleanQuery)
            if (supabaseResults.isNotEmpty()) {
                Log.d(TAG, "Tier 2 HIT: Found '${cleanQuery}' in Supabase Cache (${supabaseResults.size} items)")
                memoryCache[cacheKey] = supabaseResults
                return@withContext OmdbSearchResult(
                    items = supabaseResults,
                    source = CacheSource.SUPABASE_CACHE,
                    statusMessage = "Served from Supabase DB Cache (0 OMDb Quota Used)"
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Supabase cache query error: ${e.message}")
        }

        // 3. Tier 3: Query OMDb Live API with Primary -> Backup Failover
        var omdbItems: List<MediaItem>? = null
        var usedSource: CacheSource = CacheSource.OMDB_PRIMARY
        var rateLimitHit = false

        // Try Primary Key (38df43c1)
        try {
            val primaryResponse = fetchFromOmdb(cleanQuery, OMDB_KEY_PRIMARY)
            if (primaryResponse.isRateLimited) {
                rateLimitHit = true
                Log.w(TAG, "Primary OMDb key rate limited! Attempting failover to backup key...")
            } else if (primaryResponse.items.isNotEmpty()) {
                omdbItems = primaryResponse.items
                usedSource = CacheSource.OMDB_PRIMARY
            }
        } catch (e: Exception) {
            Log.w(TAG, "Primary OMDb request failed: ${e.message}. Trying backup key...")
        }

        // Failover to Backup Key (71610ba3) if primary failed or rate-limited
        if (omdbItems == null) {
            try {
                val backupResponse = fetchFromOmdb(cleanQuery, OMDB_KEY_BACKUP)
                if (backupResponse.isRateLimited) {
                    rateLimitHit = true
                    Log.w(TAG, "Backup OMDb key is also rate limited!")
                } else if (backupResponse.items.isNotEmpty()) {
                    omdbItems = backupResponse.items
                    usedSource = CacheSource.OMDB_BACKUP
                }
            } catch (e: Exception) {
                Log.e(TAG, "Backup OMDb request failed: ${e.message}")
            }
        }

        // If OMDb returned items, save to Supabase and memory cache
        if (!omdbItems.isNullOrEmpty()) {
            memoryCache[cacheKey] = omdbItems
            // Asynchronously save in Supabase so future requests never hit OMDb
            try {
                saveToSupabaseCache(cleanQuery, omdbItems)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to persist items in Supabase: ${e.message}")
            }

            return@withContext OmdbSearchResult(
                items = omdbItems,
                source = usedSource,
                statusMessage = if (usedSource == CacheSource.OMDB_BACKUP)
                    "Fetched via OMDb Backup Key (Failover active) • Saved to Supabase"
                else
                    "Fetched via OMDb API • Persisted into Supabase Cache"
            )
        }

        // 4. Tier 4: Fallback to local catalog and graceful rate limit message
        val localFallback = MockMediaRepository.allMediaItems.filter {
            it.title.contains(cleanQuery, ignoreCase = true) ||
                    it.genres.any { g -> g.contains(cleanQuery, ignoreCase = true) } ||
                    it.synopsis.contains(cleanQuery, ignoreCase = true)
        }

        val message = if (rateLimitHit) {
            "OMDb Rate Limit reached. Showing offline & cached collection."
        } else {
            "No direct OMDb matches. Showing recommended titles."
        }

        return@withContext OmdbSearchResult(
            items = localFallback,
            source = CacheSource.OFFLINE_CATALOG,
            statusMessage = message,
            isRateLimited = rateLimitHit
        )
    }

    /**
     * Query Supabase omdb_cache table via REST
     */
    private suspend fun querySupabaseCache(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "$SUPABASE_URL/rest/v1/omdb_cache?or=(search_query.ilike.*$encoded*,title.ilike.*$encoded*)&select=*&limit=20"

        val request = Request.Builder()
            .url(url)
            .get()
            .header("apikey", SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val bodyString = response.body?.string() ?: return@withContext emptyList()
            val jsonArray = JSONArray(bodyString)
            val list = mutableListOf<MediaItem>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(mapJsonToMediaItem(obj))
            }
            list
        }
    }

    /**
     * Save discovered movies to Supabase with upsert (resolution=merge-duplicates)
     */
    private suspend fun saveToSupabaseCache(query: String, items: List<MediaItem>) = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext

        val jsonArray = JSONArray()
        items.take(10).forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("year", item.releaseYear.toString())
                put("type", if (item.type == MediaType.TV_SERIES) "series" else "movie")
                put("poster", item.posterUrl ?: item.backdropUrl ?: "")
                put("plot", item.synopsis)
                put("genre", item.primaryGenre)
                put("imdb_rating", item.imdbRating.toString())
                put("runtime", item.duration)
                put("search_query", query.lowercase().trim())
            }
            jsonArray.put(obj)
        }

        val requestBody = jsonArray.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/omdb_cache")
            .post(requestBody)
            .header("apikey", SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .header("Prefer", "resolution=merge-duplicates")
            .build()

        client.newCall(request).execute().use { response ->
            Log.d(TAG, "Saved ${items.size} movies to Supabase cache. Response: ${response.code}")
        }
    }

    private data class OmdbCallResult(
        val items: List<MediaItem>,
        val isRateLimited: Boolean
    )

    /**
     * Fetch from OMDb API directly with error / rate limit detection
     */
    private suspend fun fetchFromOmdb(query: String, apiKey: String): OmdbCallResult = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://www.omdbapi.com/?s=$encoded&apikey=$apiKey"

        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (response.code == 429) {
                return@withContext OmdbCallResult(emptyList(), isRateLimited = true)
            }
            val body = response.body?.string() ?: return@withContext OmdbCallResult(emptyList(), false)
            val json = JSONObject(body)

            val isResponse = json.optString("Response", "False")
            if (isResponse.equals("False", ignoreCase = true)) {
                val errorMsg = json.optString("Error", "")
                val isLimit = errorMsg.contains("limit", ignoreCase = true) || errorMsg.contains("exceeded", ignoreCase = true)
                return@withContext OmdbCallResult(emptyList(), isRateLimited = isLimit)
            }

            val searchArray = json.optJSONArray("Search") ?: return@withContext OmdbCallResult(emptyList(), false)
            val mediaList = mutableListOf<MediaItem>()

            for (i in 0 until searchArray.length()) {
                val itemObj = searchArray.getJSONObject(i)
                val id = itemObj.optString("imdbID", "tt_${System.currentTimeMillis()}_$i")
                val title = itemObj.optString("Title", "Unknown Title")
                val yearStr = itemObj.optString("Year", "2024")
                val year = yearStr.filter { it.isDigit() }.take(4).toIntOrNull() ?: 2024
                val typeStr = itemObj.optString("Type", "movie")
                val poster = itemObj.optString("Poster", "")
                val posterUrl = if (poster.startsWith("http")) poster else null

                mediaList.add(
                    MediaItem(
                        id = id,
                        title = title,
                        tagline = "OMDb Blockbuster",
                        synopsis = "Critically acclaimed title discovered via OMDb and cached on Supabase.",
                        type = if (typeStr.contains("series", ignoreCase = true)) MediaType.TV_SERIES else MediaType.MOVIE,
                        posterUrl = posterUrl,
                        backdropUrl = posterUrl,
                        primaryGenre = if (typeStr.contains("series", ignoreCase = true)) "TV Series" else "Cinema",
                        genres = listOf("OMDb", if (typeStr.contains("series", ignoreCase = true)) "Series" else "Movie"),
                        releaseYear = year,
                        duration = "Feature",
                        imdbRating = 8.4,
                        matchScore = 95,
                        qualityBadges = listOf("4K UHD", "Dolby Atmos", "OMDb")
                    )
                )
            }

            OmdbCallResult(mediaList, false)
        }
    }

    /**
     * Fetch complete movie details and generate comments
     */
    suspend fun fetchCommentsForMovie(title: String) = withContext(Dispatchers.IO) {
        val details = fetchMovieDetailByTitle(title)
        // Create a dummy MediaItem for the comment generator context
        val media = MediaItem(
            id = "temp",
            title = title,
            tagline = "",
            synopsis = "",
            primaryGenre = "Cinema",
            genres = emptyList(),
            releaseYear = 2024,
            duration = ""
        )
        val comments = generateCommentsForMovie(media, details)
        _movieComments.value = comments
    }

    private fun mapJsonToMediaItem(obj: JSONObject): MediaItem {
        val id = obj.optString("id", "omdb_${System.currentTimeMillis()}")
        val title = obj.optString("title", "Untitled")
        val year = obj.optString("year", "2024").filter { it.isDigit() }.take(4).toIntOrNull() ?: 2024
        val typeStr = obj.optString("type", "movie")
        val poster = obj.optString("poster", "")
        val plot = obj.optString("plot", "Cached in Supabase OTT Platform.")
        val genre = obj.optString("genre", "Action")
        val rating = obj.optString("imdb_rating", "8.5").toDoubleOrNull() ?: 8.5
        val runtime = obj.optString("runtime", "2h 00m")

        return MediaItem(
            id = id,
            title = title,
            tagline = "Supabase Cached Title",
            synopsis = plot.ifBlank { "High-speed streaming title from Supabase local database cache." },
            type = if (typeStr.contains("series", ignoreCase = true)) MediaType.TV_SERIES else MediaType.MOVIE,
            posterUrl = poster.ifBlank { null },
            backdropUrl = poster.ifBlank { null },
            primaryGenre = genre.split(",").firstOrNull()?.trim() ?: "Cinema",
            genres = genre.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("Drama") },
            releaseYear = year,
            duration = runtime.ifBlank { "2h" },
            imdbRating = rating,
            matchScore = 96,
            qualityBadges = listOf("4K UHD", "Supabase Cached")
        )
    }

    /**
     * Fetch complete movie details from OMDb by title with dual-key fallback
     */
    suspend fun fetchMovieDetailByTitle(title: String): JSONObject? = withContext(Dispatchers.IO) {
        val cleanTitle = title.replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("\\(.*?\\)"), "")
            .trim()
        if (cleanTitle.isBlank()) return@withContext null

        val encoded = URLEncoder.encode(cleanTitle, "UTF-8")
        val keys = listOf(OMDB_KEY_PRIMARY, OMDB_KEY_BACKUP)
        for (apiKey in keys) {
            try {
                val url = "https://www.omdbapi.com/?t=$encoded&plot=full&apikey=$apiKey"
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val json = JSONObject(body)
                        if (json.optString("Response", "False").equals("True", ignoreCase = true)) {
                            return@withContext json
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "OMDb detail fetch failed with key $apiKey: ${e.message}")
            }
        }
        null
    }

    /**
     * Generates or extracts rich comments & reviews from OMDb data
     */
    fun generateCommentsForMovie(media: MediaItem, omdbData: JSONObject?): List<MovieComment> {
        val comments = mutableListOf<MovieComment>()
        val cleanTitle = media.title.replace(Regex("\\[.*?\\]"), "").trim()

        if (omdbData != null) {
            val director = omdbData.optString("Director", "").takeIf { it.isNotBlank() && it != "N/A" }
            val actors = omdbData.optString("Actors", "").takeIf { it.isNotBlank() && it != "N/A" }
            val plot = omdbData.optString("Plot", "")
            val awards = omdbData.optString("Awards", "").takeIf { it.isNotBlank() && it != "N/A" }
            val language = omdbData.optString("Language", "Hindi")
            val ratingsArray = omdbData.optJSONArray("Ratings")

            // 1. Rotten Tomatoes or Critic Review
            var rtScore: String? = null
            var imdbScore: String? = null
            if (ratingsArray != null) {
                for (i in 0 until ratingsArray.length()) {
                    val r = ratingsArray.getJSONObject(i)
                    if (r.optString("Source").contains("Rotten", ignoreCase = true)) {
                        rtScore = r.optString("Value")
                    }
                    if (r.optString("Source").contains("Internet Movie", ignoreCase = true)) {
                        imdbScore = r.optString("Value")
                    }
                }
            }

            if (rtScore != null) {
                comments.add(
                    MovieComment(
                        id = "c_rt",
                        author = "Rotten Tomatoes Critics",
                        text = "Certified Fresh with $rtScore critical rating! Gripping narrative and high-octane visual execution.",
                        timeAgo = "2h ago",
                        rating = 4.8,
                        likes = 34,
                        badge = "Critic Consensus"
                    )
                )
            }

            if (imdbScore != null || omdbData.has("imdbRating")) {
                val score = imdbScore ?: "${omdbData.optString("imdbRating", "8.5")}/10"
                comments.add(
                    MovieComment(
                        id = "c_imdb",
                        author = "IMDb Community Verified",
                        text = "Rated $score worldwide. An absolute cinematic triumph, best streamed with headphones or home theatre.",
                        timeAgo = "4h ago",
                        rating = 5.0,
                        likes = 28,
                        badge = "Verified Rating"
                    )
                )
            }

            // 2. Cast & Acting focus
            if (actors != null) {
                val leadActor = actors.split(",").firstOrNull()?.trim() ?: "The cast"
                comments.add(
                    MovieComment(
                        id = "c_cast",
                        author = "Rahul Sharma",
                        text = "Stunning performance by $leadActor! Especially the intense dialogues and climax scenes. 10/10 ⭐⭐⭐⭐⭐",
                        timeAgo = "6h ago",
                        rating = 5.0,
                        likes = 19
                    )
                )
            }

            // 3. Direction & Atmosphere
            if (director != null) {
                comments.add(
                    MovieComment(
                        id = "c_dir",
                        author = "Ananya Patel",
                        text = "Direction by $director is pure artistry. The camera angles and background score build pure tension!",
                        timeAgo = "1d ago",
                        rating = 4.9,
                        likes = 15
                    )
                )
            }

            // 4. Dubbing / Audio Experience
            val primeLang = language.split(",").firstOrNull()?.trim() ?: "Hindi"
            comments.add(
                MovieComment(
                    id = "c_audio",
                    author = "Hassan Ali",
                    text = "Dubbing in $primeLang is crisp and natural! Streaming in Full HD on Cineflix player without buffering. 👏🔥",
                    timeAgo = "1d ago",
                    rating = 5.0,
                    likes = 22
                )
            )

            // 5. Awards or Plot note
            if (awards != null) {
                comments.add(
                    MovieComment(
                        id = "c_awards",
                        author = "Devendra Rao",
                        text = "$awards. Deserves every single praise it received. Don't skip the post-credits!",
                        timeAgo = "2d ago",
                        rating = 4.7,
                        likes = 9
                    )
                )
            }
        }

        // Fallback realistic comments if less than 3
        if (comments.size < 3) {
            comments.addAll(
                listOf(
                    MovieComment(
                        id = "c_fb_1",
                        author = "Aarav Kumar",
                        text = "Spectacular watch! $cleanTitle delivers incredible thrills and emotional depth throughout.",
                        timeAgo = "3h ago",
                        rating = 5.0,
                        likes = 14
                    ),
                    MovieComment(
                        id = "c_fb_2",
                        author = "Pooja Verma",
                        text = "The Hindi dub and original soundtrack sound amazing on this OTT server. Must watch! ⭐⭐⭐⭐⭐",
                        timeAgo = "8h ago",
                        rating = 4.8,
                        likes = 11
                    ),
                    MovieComment(
                        id = "c_fb_3",
                        author = "Zaid Khan",
                        text = "Flawless 1080p playback. Cineflix player controls and skip intro feature make it super smooth.",
                        timeAgo = "1d ago",
                        rating = 5.0,
                        likes = 8
                    )
                )
            )
        }

        return comments
    }
}
