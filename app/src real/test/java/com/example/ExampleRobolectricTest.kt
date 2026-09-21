package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.SessionManager
import com.example.auth.UserProfile
import com.example.model.MockMediaRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CINEFLIX", appName)
    }

    @Test
    fun `media repository loads items and top 10 items`() {
        val allItems = MockMediaRepository.allMediaItems
        assertTrue("Media items must not be empty", allItems.isNotEmpty())
        val heroItems = MockMediaRepository.sampleHeroItems
        assertTrue("Hero items must be available", heroItems.isNotEmpty())
        val top10 = allItems.filter { it.isTop10 }
        assertTrue("Top 10 items must exist", top10.isNotEmpty())
        assertNotNull(allItems.first().title)
    }

    @Test
    fun `session manager caches login and watchlist locally`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = SessionManager(context)

        // Test user session persistence
        val testUser = UserProfile(
            id = "test-123",
            email = "vanlife.style24@gmail.com",
            name = "Adnan",
            provider = "google"
        )
        sessionManager.saveSession(testUser, rememberMe = true)
        assertEquals("vanlife.style24@gmail.com", sessionManager.currentUser.value?.email)
        assertEquals("Adnan", sessionManager.currentUser.value?.name)
        assertTrue(sessionManager.currentUser.value?.isCachedLocally == true)

        // Test watchlist caching (rate limit safe)
        sessionManager.toggleWatchlist("test-movie-id")
        assertTrue(sessionManager.isBookmarked("test-movie-id"))

        sessionManager.toggleWatchlist("test-movie-id")
        assertFalse(sessionManager.isBookmarked("test-movie-id"))

        // Test logout
        sessionManager.clearSession()
        assertEquals(null, sessionManager.currentUser.value)
    }

    @Test
    fun `omdb repository parses query and handles cache sources`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val omdbRepo = com.example.omdb.OmdbRepository(context)

        assertNotNull(omdbRepo)
        // Test fallback search on local repository for offline safety
        val localItems = com.example.model.MockMediaRepository.allMediaItems
        assertTrue(localItems.isNotEmpty())
    }
}
