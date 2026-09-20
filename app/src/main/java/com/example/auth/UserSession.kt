package com.example.auth

data class UserProfile(
    val id: String,
    val email: String,
    val name: String,
    val provider: String = "email", // "email" or "google"
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val isCachedLocally: Boolean = true,
    val loginTime: Long = System.currentTimeMillis()
)

sealed interface AuthResult {
    data class Success(val user: UserProfile, val message: String = "Login successful") : AuthResult
    data class NeedsEmailConfirmation(val email: String, val message: String) : AuthResult
    data class Error(val message: String) : AuthResult
}
