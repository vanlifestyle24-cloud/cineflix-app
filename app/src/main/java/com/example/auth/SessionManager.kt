package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class WatchHistoryEntry(
    val mediaId: String,
    val watchedTime: String = "45m",
    val totalTime: String = "2h 15m",
    val progressPercent: Float = 0.5f,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * SessionManager handles local mobile caching of user login credentials,
 * sessions, and watchlist state to prevent repetitive network requests
 * and protect against API rate limiting.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _watchlist = MutableStateFlow<Set<String>>(emptySet())
    val watchlist: StateFlow<Set<String>> = _watchlist.asStateFlow()

    private val _downloadedIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedIds: StateFlow<Set<String>> = _downloadedIds.asStateFlow()

    private val _activeDownloads = MutableStateFlow<Map<String, Float>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, Float>> = _activeDownloads.asStateFlow()

    private val _historyEntries = MutableStateFlow<List<WatchHistoryEntry>>(emptyList())
    val historyEntries: StateFlow<List<WatchHistoryEntry>> = _historyEntries.asStateFlow()

    private val _isVipActive = MutableStateFlow(prefs.getBoolean(KEY_IS_VIP, true)) // Default active VIP pass for demo
    val isVipActive: StateFlow<Boolean> = _isVipActive.asStateFlow()

    private val _vipPlan = MutableStateFlow(prefs.getString(KEY_VIP_PLAN, "Annual VIP 4K Pass") ?: "Annual VIP 4K Pass")
    val vipPlan: StateFlow<String> = _vipPlan.asStateFlow()

    private val _isAdminMode = MutableStateFlow(prefs.getBoolean(KEY_IS_ADMIN, false))
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    private val _cachedEmail = MutableStateFlow(prefs.getString(KEY_REMEMBERED_EMAIL, "") ?: "")
    val cachedEmail: StateFlow<String> = _cachedEmail.asStateFlow()

    private val _cachedPassword = MutableStateFlow(prefs.getString(KEY_REMEMBERED_PASSWORD, "") ?: "")
    val cachedPassword: StateFlow<String> = _cachedPassword.asStateFlow()

    private val _rememberMe = MutableStateFlow(prefs.getBoolean(KEY_REMEMBER_ME, true))
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()

    private val _carouselAutoRotate = MutableStateFlow(prefs.getBoolean(KEY_CAROUSEL_AUTO_ROTATE, true))
    val carouselAutoRotate: StateFlow<Boolean> = _carouselAutoRotate.asStateFlow()

    private val _carouselRotateSpeed = MutableStateFlow(prefs.getFloat(KEY_CAROUSEL_ROTATE_SPEED, 5f))
    val carouselRotateSpeed: StateFlow<Float> = _carouselRotateSpeed.asStateFlow()

    init {
        restoreSessionFromCache()
        restoreWatchlistFromCache()
        restoreDownloadsAndHistoryFromCache()
    }

    private fun restoreSessionFromCache() {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (isLoggedIn) {
            val id = prefs.getString(KEY_USER_ID, null) ?: "cached-user"
            val email = prefs.getString(KEY_USER_EMAIL, null) ?: "user@cineflix.com"
            val name = prefs.getString(KEY_USER_NAME, null) ?: email.substringBefore("@")
            val provider = prefs.getString(KEY_PROVIDER, "email") ?: "email"
            val token = prefs.getString(KEY_ACCESS_TOKEN, null)
            val loginTime = prefs.getLong(KEY_LOGIN_TIME, System.currentTimeMillis())

            _currentUser.value = UserProfile(
                id = id,
                email = email,
                name = name,
                provider = provider,
                accessToken = token,
                isCachedLocally = true,
                loginTime = loginTime
            )
        }
    }

    private fun restoreWatchlistFromCache() {
        val saved = prefs.getStringSet(KEY_WATCHLIST, setOf("hero-1", "row-1")) ?: setOf("hero-1", "row-1")
        _watchlist.value = saved
    }

    private fun restoreDownloadsAndHistoryFromCache() {
        // Read downloaded IDs
        val savedDownloads = prefs.getStringSet(KEY_DOWNLOADS, null)
        if (savedDownloads != null) {
            _downloadedIds.value = savedDownloads
        } else {
            _downloadedIds.value = emptySet()
            prefs.edit().putStringSet(KEY_DOWNLOADS, emptySet()).apply()
        }

        // Read watch history JSON array
        val savedHistoryJson = prefs.getString(KEY_HISTORY, null)
        if (savedHistoryJson != null) {
            try {
                val array = JSONArray(savedHistoryJson)
                val list = mutableListOf<WatchHistoryEntry>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        WatchHistoryEntry(
                            mediaId = obj.getString("mediaId"),
                            watchedTime = obj.optString("watchedTime", "0m"),
                            totalTime = obj.optString("totalTime", "0m"),
                            progressPercent = obj.optDouble("progressPercent", 0.0).toFloat(),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                _historyEntries.value = list
            } catch (e: Exception) {
                _historyEntries.value = emptyList()
            }
        } else {
            _historyEntries.value = emptyList()
            saveHistoryToPrefs(emptyList())
        }
    }

    fun addDownload(mediaId: String) {
        // Start simulation
        val currentActive = _activeDownloads.value.toMutableMap()
        currentActive[mediaId] = 0.05f
        _activeDownloads.value = currentActive

        // Normally we'd start a DownloadService here
        // For simulation, we just mark it as downloaded after a while
    }

    fun completeDownload(mediaId: String) {
        val currentActive = _activeDownloads.value.toMutableMap()
        currentActive.remove(mediaId)
        _activeDownloads.value = currentActive

        val updated = HashSet(_downloadedIds.value).apply { add(mediaId) }
        _downloadedIds.value = updated
        prefs.edit().putStringSet(KEY_DOWNLOADS, updated).apply()
    }

    fun updateDownloadProgress(mediaId: String, progress: Float) {
        val currentActive = _activeDownloads.value.toMutableMap()
        if (currentActive.containsKey(mediaId)) {
            currentActive[mediaId] = progress
            _activeDownloads.value = currentActive
        }
    }

    fun removeDownload(mediaId: String) {
        val updated = HashSet(_downloadedIds.value).apply { remove(mediaId) }
        _downloadedIds.value = updated
        prefs.edit().putStringSet(KEY_DOWNLOADS, updated).apply()
    }

    fun clearDownloads() {
        _downloadedIds.value = emptySet()
        prefs.edit().putStringSet(KEY_DOWNLOADS, HashSet<String>()).apply()
    }

    fun addWatchHistory(mediaId: String, watched: String = "45m", total: String = "2h 15m", progress: Float = 0.5f) {
        val current = _historyEntries.value.filter { it.mediaId != mediaId }.toMutableList()
        current.add(0, WatchHistoryEntry(mediaId, watched, total, progress, System.currentTimeMillis()))
        _historyEntries.value = current
        saveHistoryToPrefs(current)
    }

    fun removeWatchHistory(mediaId: String) {
        val updated = _historyEntries.value.filter { it.mediaId != mediaId }
        _historyEntries.value = updated
        saveHistoryToPrefs(updated)
    }

    fun clearWatchHistory() {
        _historyEntries.value = emptyList()
        saveHistoryToPrefs(emptyList())
    }

    private fun saveHistoryToPrefs(entries: List<WatchHistoryEntry>) {
        try {
            val array = JSONArray()
            for (entry in entries) {
                val obj = JSONObject().apply {
                    put("mediaId", entry.mediaId)
                    put("watchedTime", entry.watchedTime)
                    put("totalTime", entry.totalTime)
                    put("progressPercent", entry.progressPercent.toDouble())
                    put("timestamp", entry.timestamp)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveSession(
        user: UserProfile,
        plainPasswordToRemember: String? = null,
        rememberMe: Boolean = true
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, user.id)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_NAME, user.name)
            putString(KEY_PROVIDER, user.provider)
            putString(KEY_ACCESS_TOKEN, user.accessToken)
            putLong(KEY_LOGIN_TIME, user.loginTime)
            putBoolean(KEY_REMEMBER_ME, rememberMe)

            if (rememberMe && !plainPasswordToRemember.isNullOrBlank()) {
                putString(KEY_REMEMBERED_EMAIL, user.email)
                putString(KEY_REMEMBERED_PASSWORD, plainPasswordToRemember)
                _cachedEmail.value = user.email
                _cachedPassword.value = plainPasswordToRemember
            } else if (!rememberMe) {
                remove(KEY_REMEMBERED_PASSWORD)
                _cachedPassword.value = ""
            }
            apply()
        }
        _rememberMe.value = rememberMe
        _currentUser.value = user
    }

    fun clearSession() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            putBoolean(KEY_IS_ADMIN, false)
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_NAME)
            remove(KEY_PROVIDER)
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_LOGIN_TIME)
            // If remember me is false, also clear cached password
            if (!_rememberMe.value) {
                remove(KEY_REMEMBERED_EMAIL)
                remove(KEY_REMEMBERED_PASSWORD)
                _cachedEmail.value = ""
                _cachedPassword.value = ""
            }
            apply()
        }
        _isAdminMode.value = false
        _currentUser.value = null
    }

    fun toggleWatchlist(mediaId: String): Boolean {
        val current = _watchlist.value.toMutableSet()
        val isAdded: Boolean
        if (current.contains(mediaId)) {
            current.remove(mediaId)
            isAdded = false
        } else {
            current.add(mediaId)
            isAdded = true
        }
        _watchlist.value = current
        // Persist immediately in local mobile cache
        prefs.edit().putStringSet(KEY_WATCHLIST, HashSet(current)).apply()
        return isAdded
    }

    fun isBookmarked(mediaId: String): Boolean {
        return _watchlist.value.contains(mediaId)
    }

    fun activateVip(plan: String = "Annual VIP 4K Pass") {
        prefs.edit().apply {
            putBoolean(KEY_IS_VIP, true)
            putString(KEY_VIP_PLAN, plan)
            apply()
        }
        _isVipActive.value = true
        _vipPlan.value = plan
    }

    fun deactivateVip() {
        prefs.edit().apply {
            putBoolean(KEY_IS_VIP, false)
            putString(KEY_VIP_PLAN, "Free Tier")
            apply()
        }
        _isVipActive.value = false
        _vipPlan.value = "Free Tier"
    }

    fun redeemPromoCode(inputCode: String): Pair<Boolean, String> {
        val trimmed = inputCode.trim().uppercase()
        return when {
            trimmed in listOf("VIP2026", "FREE4K", "STUDENT100", "CINEFLIXVIP", "PREMIUM", "VIP") -> {
                activateVip("Annual VIP 4K Pass (Redeemed)")
                Pair(true, "🎉 Voucher '$trimmed' applied successfully! VIP 4K Pass unlocked.")
            }
            trimmed == "LIFETIME" -> {
                activateVip("Lifetime Master VIP")
                Pair(true, "🌟 Special code! Lifetime Master VIP unlocked.")
            }
            trimmed.isEmpty() -> {
                Pair(false, "Please enter a voucher or gift code.")
            }
            else -> {
                Pair(false, "Invalid voucher code. Try 'VIP2026' or 'FREE4K'.")
            }
        }
    }

    fun setAdminMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_ADMIN, enabled).apply()
        _isAdminMode.value = enabled
    }

    fun loginWithAnalysis(
        emailInput: String,
        passwordInput: String,
        displayNameInput: String = "",
        rememberMe: Boolean = true
    ): Pair<UserProfile, Boolean> {
        val cleanEmail = emailInput.trim().lowercase()
        val cleanPass = passwordInput.trim()

        val isAdmin = (cleanEmail == "admin@cineflix.com" ||
                cleanEmail == "admin" ||
                cleanEmail.startsWith("admin_") ||
                cleanEmail.startsWith("admin@")) &&
                (cleanPass in listOf("admin", "admin123", "master2026", "cineflix2026", "cineflix_live_master_98f4a21e7d0b3c65e8a11974ef") || cleanPass.length >= 4)

        if (isAdmin) {
            setAdminMode(true)
            val adminUser = UserProfile(
                id = "admin_master_1",
                email = if (cleanEmail.contains("@")) cleanEmail else "admin@cineflix.com",
                name = if (displayNameInput.isNotBlank()) displayNameInput else "Master Admin",
                provider = "admin_master",
                isCachedLocally = true
            )
            saveSession(adminUser, plainPasswordToRemember = cleanPass, rememberMe = rememberMe)
            return Pair(adminUser, true)
        } else {
            setAdminMode(false)
            val regularUser = UserProfile(
                id = "user_${System.currentTimeMillis() % 10000}",
                email = cleanEmail,
                name = if (displayNameInput.isNotBlank()) displayNameInput else cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                provider = "email",
                isCachedLocally = true
            )
            saveSession(regularUser, plainPasswordToRemember = cleanPass, rememberMe = rememberMe)
            return Pair(regularUser, false)
        }
    }

    fun loginAsAdmin(passwordOrPin: String, adminName: String = "Master Admin"): Pair<Boolean, String> {
        val clean = passwordOrPin.trim()
        val validPasscodes = listOf("admin", "admin123", "master2026", "cineflix2026", "1234", "0000", "cineflix_live_master_98f4a21e7d0b3c65e8a11974ef")
        return if (clean in validPasscodes || clean.length >= 4) {
            setAdminMode(true)
            val adminProfile = UserProfile(
                id = "admin_master_1",
                email = "admin@cineflix.com",
                name = adminName,
                provider = "admin_master",
                isCachedLocally = true
            )
            saveSession(adminProfile, rememberMe = true)
            Pair(true, "⚡ Master Admin Dashboard unlocked successfully!")
        } else {
            Pair(false, "Invalid Admin passcode. Default code: 'admin123' or 'master2026'")
        }
    }

    fun logoutAdmin() {
        setAdminMode(false)
    }

    fun setCarouselAutoRotate(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CAROUSEL_AUTO_ROTATE, enabled).apply()
        _carouselAutoRotate.value = enabled
    }

    fun setCarouselRotateSpeed(speedSeconds: Float) {
        val safeSpeed = if (speedSeconds.isNaN()) 5f else speedSeconds.coerceIn(3f, 15f)
        prefs.edit().putFloat(KEY_CAROUSEL_ROTATE_SPEED, safeSpeed).apply()
        _carouselRotateSpeed.value = safeSpeed
    }

    companion object {
        private const val PREF_NAME = "cineflix_user_cache"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_IS_ADMIN = "is_admin_mode"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_PROVIDER = "auth_provider"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_LOGIN_TIME = "login_time"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_REMEMBERED_EMAIL = "remembered_email"
        private const val KEY_REMEMBERED_PASSWORD = "remembered_password"
        private const val KEY_WATCHLIST = "cached_watchlist"
        private const val KEY_DOWNLOADS = "cached_downloads_set"
        private const val KEY_HISTORY = "cached_history_json"
        private const val KEY_IS_VIP = "is_vip_active"
        private const val KEY_VIP_PLAN = "vip_plan_name"
        private const val KEY_CAROUSEL_AUTO_ROTATE = "carousel_auto_rotate"
        private const val KEY_CAROUSEL_ROTATE_SPEED = "carousel_rotate_speed"
    }
}
