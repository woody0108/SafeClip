package com.glass.safeclip.data.auth

sealed interface AuthConnectionResult {
    data class SignedIn(
        val uid: String,
        val displayName: String?,
        val email: String?,
        val provider: String
    ) : AuthConnectionResult

    data class NeedsFirebaseSetup(
        val message: String
    ) : AuthConnectionResult

    data class Failed(
        val message: String
    ) : AuthConnectionResult
}
