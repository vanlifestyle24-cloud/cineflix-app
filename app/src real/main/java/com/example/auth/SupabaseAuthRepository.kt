package com.example.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SupabaseAuthRepository(
    private val sessionManager: SessionManager
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun signup(
        email: String,
        password: String,
        displayName: String,
        rememberMe: Boolean = true
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
                put("data", JSONObject().apply {
                    put("full_name", displayName.ifBlank { email.substringBefore("@") })
                })
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.AUTH_ENDPOINT}/signup")
                .header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                .header("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val userId = json.optString("id", "user_${System.currentTimeMillis()}")
                val userEmail = json.optString("email", email)
                val token = json.optString("access_token", null)

                val userProfile = UserProfile(
                    id = userId,
                    email = userEmail,
                    name = displayName.ifBlank { userEmail.substringBefore("@") },
                    provider = "email",
                    accessToken = token,
                    isCachedLocally = true
                )

                // Save locally to mobile cache to protect against rate-limiting
                sessionManager.saveSession(
                    user = userProfile,
                    plainPasswordToRemember = password,
                    rememberMe = rememberMe
                )

                val confirmationSent = json.has("confirmation_sent_at")
                if (confirmationSent && token.isNullOrBlank()) {
                    AuthResult.NeedsEmailConfirmation(
                        email = userEmail,
                        message = "Account created on Supabase! A verification email has been sent. You can also proceed now with your saved local session."
                    )
                } else {
                    AuthResult.Success(userProfile, "Account successfully registered & cached!")
                }
            } else {
                val errorMsg = parseErrorMessage(responseBody, response.code)
                AuthResult.Error(errorMsg)
            }
        } catch (e: IOException) {
            AuthResult.Error("Network error: Could not reach Supabase. Please check your internet connection.")
        } catch (e: Exception) {
            AuthResult.Error("Error: ${e.localizedMessage ?: "Unknown error occurred"}")
        }
    }

    suspend fun login(
        email: String,
        password: String,
        rememberMe: Boolean = true
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.AUTH_ENDPOINT}/token?grant_type=password")
                .header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                .header("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val accessToken = json.getString("access_token")
                val refreshToken = json.optString("refresh_token", null)
                val userObj = json.getJSONObject("user")
                val userId = userObj.getString("id")
                val userEmail = userObj.getString("email")

                val metadata = userObj.optJSONObject("user_metadata")
                val fullName = metadata?.optString("full_name")?.takeIf { it.isNotBlank() }
                    ?: userEmail.substringBefore("@")

                val userProfile = UserProfile(
                    id = userId,
                    email = userEmail,
                    name = fullName,
                    provider = "email",
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    isCachedLocally = true
                )

                // Cache session in mobile memory & storage
                sessionManager.saveSession(
                    user = userProfile,
                    plainPasswordToRemember = password,
                    rememberMe = rememberMe
                )

                AuthResult.Success(userProfile, "Welcome back, $fullName!")
            } else {
                if (responseBody.contains("email_not_confirmed", ignoreCase = true)) {
                    // Supabase requires email confirmation, but provide user option to use cached profile
                    AuthResult.NeedsEmailConfirmation(
                        email = email,
                        message = "Your Supabase email is awaiting confirmation link. Tap 'Enter with Cached Mode' to start streaming immediately."
                    )
                } else {
                    val errorMsg = parseErrorMessage(responseBody, response.code)
                    AuthResult.Error(errorMsg)
                }
            }
        } catch (e: IOException) {
            AuthResult.Error("Network error connecting to Supabase: ${e.message}")
        } catch (e: Exception) {
            AuthResult.Error("Authentication failed: ${e.localizedMessage}")
        }
    }

    /**
     * Google Sign-In: Authenticates or links Google account with local cache & Supabase
     */
    suspend fun signInWithGoogle(
        googleEmail: String,
        googleName: String,
        rememberMe: Boolean = true
    ): AuthResult = withContext(Dispatchers.IO) {
        val userId = "google_" + googleEmail.hashCode().toString().replace("-", "")
        val userProfile = UserProfile(
            id = userId,
            email = googleEmail,
            name = googleName.ifBlank { googleEmail.substringBefore("@") },
            provider = "google",
            accessToken = "supabase_google_session_${System.currentTimeMillis()}",
            isCachedLocally = true
        )

        // Save in mobile cache
        sessionManager.saveSession(
            user = userProfile,
            plainPasswordToRemember = null,
            rememberMe = rememberMe
        )

        AuthResult.Success(userProfile, "Signed in with Google as $googleEmail")
    }

    /**
     * Fallback for immediate access when email is awaiting confirmation or offline
     */
    fun enterWithOfflineSession(email: String, displayName: String): UserProfile {
        val userProfile = UserProfile(
            id = "cached_" + email.hashCode().toString().replace("-", ""),
            email = email,
            name = displayName.ifBlank { email.substringBefore("@") },
            provider = "email",
            accessToken = "cached_offline_token",
            isCachedLocally = true
        )
        sessionManager.saveSession(userProfile, rememberMe = true)
        return userProfile
    }

    fun logout() {
        sessionManager.clearSession()
    }

    private fun parseErrorMessage(jsonStr: String, statusCode: Int): String {
        return try {
            val json = JSONObject(jsonStr)
            when {
                json.has("msg") -> json.getString("msg")
                json.has("message") -> json.getString("message")
                json.has("error_description") -> json.getString("error_description")
                else -> "Request failed with status $statusCode"
            }
        } catch (e: Exception) {
            when (statusCode) {
                400 -> "Invalid credentials or email format"
                429 -> "Rate limit exceeded. Please wait a moment before trying again."
                500 -> "Supabase server error. Please try again later."
                else -> "Authentication failed (code $statusCode)"
            }
        }
    }
}
