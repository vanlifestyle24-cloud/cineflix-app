package com.example.streaming

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.MockMediaRepository
import com.example.auth.SupabaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType

/**
 * Modern Supabase & Offline-First Media Catalog Manager.
 * Instantly loads existing cached data from local storage on launch,
 * then background fetches fresh data from Supabase to keep all users in sync.
 * Direct Admin edits sync directly to the Postgres database.
 */
class CineflixLocalCatalogManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_CATALOG, Context.MODE_PRIVATE)

    private val _catalog = MutableStateFlow<List<MediaItem>>(emptyList())
    val catalog: StateFlow<List<MediaItem>> = _catalog.asStateFlow()

    private val _heroItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val heroItems: StateFlow<List<MediaItem>> = _heroItems.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow("Catalog Live")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    init {
        // 1. Optimistic first-load from offline cache
        loadCatalogFromStorage()
        // 2. Fetch fresh global live data from Supabase
        fetchCatalogFromSupabase()
    }

    private fun loadCatalogFromStorage() {
        val jsonStr = prefs.getString(KEY_MOVIES_JSON, null)
        val loadedList = mutableListOf<MediaItem>()

        if (!jsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    loadedList.add(parseMediaFromJson(obj))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // If storage is empty, initialize with rich hardcoded catalog items
        if (loadedList.isEmpty()) {
            val initialCatalog = MockMediaRepository.allMediaItems.toMutableList()

            // Default fallback seeds
            val studentOfTheYear = MediaItem(
                id = "movie-soty-1",
                title = "Student of the Year",
                tagline = "The competition of a lifetime.",
                synopsis = "Several students at a prestigious high school compete for the coveted Student of the Year trophy, testing their friendships and loyalties.",
                type = MediaType.MOVIE,
                posterUrl = "https://m.media-amazon.com/images/M/MV5BMTQ4NTk1MzM0NV5BMl5BanBnXkFtZTcwOTY4MTAyOA@@._V1_SX300.jpg",
                backdropUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=1200&auto=format&fit=crop&q=80",
                primaryGenre = "Bollywood Romance",
                genres = listOf("Romance", "Drama", "Comedy", "Bollywood"),
                releaseYear = 2012,
                duration = "2h 57m",
                imdbRating = 5.3,
                matchScore = 95,
                qualityBadges = listOf("1080p FHD", "5.1 Audio", "Dolby Digital"),
                platformBadge = "RUMBLE DIRECT",
                isTop10 = true,
                top10Rank = 3,
                videoStreamUrl = "https://rumble.com/v7fpkry-gtrgtrgrtg.html"
            )

            val inceptionStream = MediaItem(
                id = "movie-inception-1",
                title = "Inception",
                tagline = "Your mind is the scene of the crime.",
                synopsis = "A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O.",
                type = MediaType.MOVIE,
                posterUrl = "https://m.media-amazon.com/images/M/MV5BMjAxMzY3NjcxNF5BMl5BanBnXkFtZTcwOTI5OTM0Mw@@._V1_SX300.jpg",
                backdropUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1200&auto=format&fit=crop&q=80",
                primaryGenre = "Sci-Fi Action",
                genres = listOf("Sci-Fi", "Action", "Thriller"),
                releaseYear = 2010,
                duration = "2h 28m",
                imdbRating = 8.8,
                matchScore = 99,
                qualityBadges = listOf("4K UHD", "Dolby Atmos", "IMAX"),
                platformBadge = "RUMBLE DIRECT",
                isTop10 = true,
                top10Rank = 1,
                videoStreamUrl = "https://rumble.com/v7fpkry-gtrgtrgrtg.html"
            )

            initialCatalog.add(0, studentOfTheYear)
            initialCatalog.add(1, inceptionStream)

            _catalog.value = initialCatalog
            saveCatalogToStorage(initialCatalog)
        } else {
            _catalog.value = loadedList
        }

        refreshHeroItems()
    }

    private fun refreshHeroItems() {
        val current = _catalog.value
        val top = current.filter { it.isTop10 || it.platformBadge.contains("ORIGINAL") }.take(5)
        _heroItems.value = if (top.isNotEmpty()) top else current.take(3)
    }

    private fun saveCatalogToStorage(items: List<MediaItem>) {
        try {
            val array = JSONArray()
            for (item in items) {
                array.put(mediaToJson(item))
            }
            prefs.edit().putString(KEY_MOVIES_JSON, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Fetch all items asynchronously from Supabase Database
     */
    fun fetchCatalogFromSupabase() {
        _isSyncing.value = true
        _syncMessage.value = "Connecting to Supabase..."
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("${SupabaseConfig.REST_ENDPOINT}/media_items?select=*&order=created_at.desc")
                    .header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                    .header("Authorization", "Bearer ${SupabaseConfig.PUBLISHABLE_KEY}")
                    .build()

                client.newCall(request).execute().use { response ->
                    _isSyncing.value = false
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (!responseBody.isNullOrBlank()) {
                            val array = JSONArray(responseBody)
                            val fetchedList = mutableListOf<MediaItem>()
                            for (i in 0 until array.length()) {
                                fetchedList.add(parseMediaFromJson(array.getJSONObject(i)))
                            }
                            
                            // Update flow and cache
                            _catalog.value = fetchedList
                            saveCatalogToStorage(fetchedList)
                            refreshHeroItems()
                            _syncMessage.value = "Synced: ${fetchedList.size} movies"
                            Log.d("CatalogManager", "Fetched ${fetchedList.size} items from Supabase successfully.")
                        } else {
                            _syncMessage.value = "Supabase empty response"
                        }
                    } else {
                        _syncMessage.value = "Sync Error: ${response.code}"
                        Log.e("CatalogManager", "Error response from Supabase REST API: ${response.code} ${response.message}")
                    }
                }
            } catch (e: Exception) {
                _isSyncing.value = false
                _syncMessage.value = "Offline cache active"
                Log.e("CatalogManager", "Network failure fetching from Supabase", e)
            }
        }
    }

    /**
     * Add new Movie or Series directly to Supabase Database
     */
    fun addMediaItem(item: MediaItem): Boolean {
        // 1. Optimistic UI update
        val current = _catalog.value.toMutableList()
        current.removeAll { it.id == item.id }
        current.add(0, item)
        _catalog.value = current
        saveCatalogToStorage(current)
        refreshHeroItems()

        // 2. Async Sync to Supabase
        _isSyncing.value = true
        _syncMessage.value = "Uploading ${item.title}..."
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val jsonPayload = mediaToJson(item).toString()
                
                val request = Request.Builder()
                    .url("${SupabaseConfig.REST_ENDPOINT}/media_items")
                    .header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                    .header("Authorization", "Bearer ${SupabaseConfig.PUBLISHABLE_KEY}")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(jsonPayload.toRequestBody(mediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    _isSyncing.value = false
                    if (response.isSuccessful) {
                        Log.d("CatalogManager", "Added/Upserted ${item.title} to Supabase database successfully.")
                        fetchCatalogFromSupabase()
                    } else {
                        _syncMessage.value = "Upload failed"
                        Log.e("CatalogManager", "Supabase Add Error: ${response.code} ${response.message} Payload: $jsonPayload")
                    }
                }
            } catch (e: Exception) {
                _isSyncing.value = false
                _syncMessage.value = "Upload exception"
                Log.e("CatalogManager", "Supabase Add Exception", e)
            }
        }
        return true
    }

    /**
     * Update existing media item in Supabase Database
     */
    fun updateMediaItem(item: MediaItem): Boolean {
        // 1. Optimistic UI update
        val current = _catalog.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            current[index] = item
        } else {
            current.add(0, item)
        }
        _catalog.value = current
        saveCatalogToStorage(current)
        refreshHeroItems()

        // 2. Async Sync to Supabase
        _isSyncing.value = true
        _syncMessage.value = "Saving ${item.title}..."
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val jsonPayload = mediaToJson(item).toString()

                val request = Request.Builder()
                    .url("${SupabaseConfig.REST_ENDPOINT}/media_items")
                    .header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                    .header("Authorization", "Bearer ${SupabaseConfig.PUBLISHABLE_KEY}")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(jsonPayload.toRequestBody(mediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    _isSyncing.value = false
                    if (response.isSuccessful) {
                        Log.d("CatalogManager", "Updated ${item.title} in Supabase database successfully.")
                        fetchCatalogFromSupabase()
                    } else {
                        _syncMessage.value = "Save failed"
                        Log.e("CatalogManager", "Supabase Update Error: ${response.code} ${response.message}")
                    }
                }
            } catch (e: Exception) {
                _isSyncing.value = false
                _syncMessage.value = "Save exception"
                Log.e("CatalogManager", "Supabase Update Exception", e)
            }
        }
        return true
    }

    /**
     * Delete media item by ID from Supabase Database
     */
    fun deleteMediaItem(id: String): Boolean {
        // 1. Optimistic UI update
        val current = _catalog.value.toMutableList()
        val removed = current.removeAll { it.id == id }
        if (removed) {
            _catalog.value = current
            saveCatalogToStorage(current)
            refreshHeroItems()

            // 2. Async Sync to Supabase
            _isSyncing.value = true
            _syncMessage.value = "Deleting..."
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url("${SupabaseConfig.REST_ENDPOINT}/media_items?id=eq.$id")
                        .header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                        .header("Authorization", "Bearer ${SupabaseConfig.PUBLISHABLE_KEY}")
                        .delete()
                        .build()

                    client.newCall(request).execute().use { response ->
                        _isSyncing.value = false
                        if (response.isSuccessful) {
                            Log.d("CatalogManager", "Deleted item $id from Supabase successfully.")
                            fetchCatalogFromSupabase()
                        } else {
                            _syncMessage.value = "Delete failed"
                            Log.e("CatalogManager", "Supabase Delete Error: ${response.code} ${response.message}")
                        }
                    }
                } catch (e: Exception) {
                    _isSyncing.value = false
                    _syncMessage.value = "Delete exception"
                    Log.e("CatalogManager", "Supabase Delete Exception", e)
                }
            }
        }
        return removed
    }

    /**
     * Reset catalog back to original hardcoded values on local storage
     */
    fun resetCatalog(): List<MediaItem> {
        prefs.edit().remove(KEY_MOVIES_JSON).apply()
        loadCatalogFromStorage()
        fetchCatalogFromSupabase()
        return _catalog.value
    }

    private fun mediaToJson(item: MediaItem): JSONObject {
        return JSONObject().apply {
            put("id", item.id)
            put("title", item.title)
            put("tagline", item.tagline)
            put("synopsis", item.synopsis)
            put("type", item.type.name)
            put("poster_url", item.posterUrl ?: "")
            put("backdrop_url", item.backdropUrl ?: "")
            put("primary_genre", item.primaryGenre)
            put("genres", JSONArray(item.genres))
            put("release_year", item.releaseYear)
            put("duration", item.duration)
            put("age_rating", item.ageRating)
            put("imdb_rating", item.imdbRating)
            put("match_score", item.matchScore)
            put("quality_badges", JSONArray(item.qualityBadges))
            put("platform_badge", item.platformBadge)
            put("is_top10", item.isTop10)
            put("top10_rank", item.top10Rank ?: JSONObject.NULL)
            put("video_stream_url", item.videoStreamUrl ?: "")
            put("youtube_video_id", item.youtubeVideoId ?: "")
            put("telegram_file_id", item.telegramFileId ?: "")
            put("creator", item.creator)
            put("audio_languages", JSONArray(item.audioLanguages))
            put("subtitle_languages", JSONArray(item.subtitleLanguages))
            put("cast_members", JSONArray(item.cast))
            put("parent_series_id", item.parentSeriesId ?: "")
            put("season_number", item.seasonNumber)
            put("episode_number", item.episodeNumber)
            put("skip_intro_start_sec", item.skipIntroStartSec ?: JSONObject.NULL)
            put("skip_intro_end_sec", item.skipIntroEndSec ?: JSONObject.NULL)
            put("skip_credits_start_sec", item.skipCreditsStartSec ?: JSONObject.NULL)
            put("skip_credits_end_sec", item.skipCreditsEndSec ?: JSONObject.NULL)

            val skipTimestampsObj = JSONObject()
            item.skipIntroStartSec?.let { skipTimestampsObj.put("intro_start", it) }
            item.skipIntroEndSec?.let { skipTimestampsObj.put("intro_end", it) }
            item.skipCreditsStartSec?.let { skipTimestampsObj.put("outro_start", it) }
            item.skipCreditsEndSec?.let { skipTimestampsObj.put("outro_end", it) }
            put("skip_timestamps", skipTimestampsObj)

            val langLinksObj = JSONObject()
            item.languageLinks.forEach { (lang, link) ->
                langLinksObj.put(lang, link)
            }
            put("language_links", langLinksObj)
        }
    }

    private fun parseMediaFromJson(obj: JSONObject): MediaItem {
        val genresList = mutableListOf<String>()
        val gArray = obj.optJSONArray("genres")
        if (gArray != null) {
            for (i in 0 until gArray.length()) {
                genresList.add(gArray.getString(i))
            }
        }
        if (genresList.isEmpty()) genresList.add(obj.optString("primary_genre", "Action"))

        val badgesList = mutableListOf<String>()
        val bArray = obj.optJSONArray("quality_badges")
        if (bArray != null) {
            for (i in 0 until bArray.length()) {
                badgesList.add(bArray.getString(i))
            }
        }
        if (badgesList.isEmpty()) badgesList.addAll(listOf("1080p FHD", "5.1 Audio"))

        val audioLangsList = mutableListOf<String>()
        val aArray = obj.optJSONArray("audio_languages")
        if (aArray != null) {
            for (i in 0 until aArray.length()) {
                audioLangsList.add(aArray.getString(i))
            }
        }
        if (audioLangsList.isEmpty()) audioLangsList.addAll(listOf("Hindi", "English [Original]"))

        val subLangsList = mutableListOf<String>()
        val sArray = obj.optJSONArray("subtitle_languages")
        if (sArray != null) {
            for (i in 0 until sArray.length()) {
                subLangsList.add(sArray.getString(i))
            }
        }
        if (subLangsList.isEmpty()) subLangsList.addAll(listOf("English [CC]", "Hindi"))

        val castList = mutableListOf<String>()
        val cArray = obj.optJSONArray("cast_members")
        if (cArray != null) {
            for (i in 0 until cArray.length()) {
                castList.add(cArray.getString(i))
            }
        }

        val languageLinksMap = mutableMapOf<String, String>()
        val langLinksObj = obj.optJSONObject("language_links")
        if (langLinksObj != null) {
            val keys = langLinksObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                languageLinksMap[key] = langLinksObj.getString(key)
            }
        }

        val typeStr = obj.optString("type", "MOVIE")
        val mediaType = try {
            MediaType.valueOf(typeStr)
        } catch (e: Exception) {
            MediaType.MOVIE
        }

        return MediaItem(
            id = obj.optString("id", "media_${System.currentTimeMillis()}"),
            title = obj.optString("title", "Untitled Title"),
            tagline = obj.optString("tagline", "Experience the ultimate entertainment."),
            synopsis = obj.optString("synopsis", "Streaming on Cineflix in high definition."),
            type = mediaType,
            posterUrl = obj.optString("poster_url", "").ifBlank { null },
            backdropUrl = obj.optString("backdrop_url", "").ifBlank { null },
            primaryGenre = obj.optString("primary_genre", "Entertainment"),
            genres = genresList,
            releaseYear = obj.optInt("release_year", 2026),
            duration = obj.optString("duration", "2h 15m"),
            imdbRating = obj.optDouble("imdb_rating", 8.0),
            matchScore = obj.optInt("match_score", 95),
            qualityBadges = badgesList,
            audioLanguages = audioLangsList,
            subtitleLanguages = subLangsList,
            cast = castList,
            creator = obj.optString("creator", "Admin Uploaded"),
            platformBadge = obj.optString("platform_badge", "ORACLE PROXY STREAM"),
            isTop10 = obj.optBoolean("is_top10", false),
            top10Rank = if (obj.isNull("top10_rank")) null else obj.optInt("top10_rank").let { if (it > 0) it else null },
            videoStreamUrl = obj.optString("video_stream_url", "").ifBlank { null },
            youtubeVideoId = obj.optString("youtube_video_id", "").ifBlank { null },
            telegramFileId = obj.optString("telegram_file_id", "").ifBlank { null },
            parentSeriesId = obj.optString("parent_series_id", "").ifBlank { null },
            seasonNumber = obj.optInt("season_number", 0),
            episodeNumber = obj.optInt("episode_number", 0),
            skipIntroStartSec = if (obj.isNull("skip_intro_start_sec")) null else obj.optInt("skip_intro_start_sec"),
            skipIntroEndSec = if (obj.isNull("skip_intro_end_sec")) null else obj.optInt("skip_intro_end_sec"),
            skipCreditsStartSec = if (obj.isNull("skip_credits_start_sec")) null else obj.optInt("skip_credits_start_sec"),
            skipCreditsEndSec = if (obj.isNull("skip_credits_end_sec")) null else obj.optInt("skip_credits_end_sec"),
            languageLinks = languageLinksMap
        )
    }

    companion object {
        private const val PREF_CATALOG = "cineflix_local_catalog_db"
        private const val KEY_MOVIES_JSON = "saved_movies_catalog_json"
    }
}
