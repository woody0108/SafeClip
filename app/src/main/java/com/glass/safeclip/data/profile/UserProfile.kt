package com.glass.safeclip.data.profile

data class UserProfile(
    val uid: String,
    val guestId: String,
    val email: String?,
    val displayName: String?,
    val provider: String
) {
    val documentPath: String = "users/$uid"
}

sealed interface UserProfileSyncResult {
    data class Success(
        val profile: UserProfile,
        val message: String
    ) : UserProfileSyncResult

    data class Failed(
        val message: String
    ) : UserProfileSyncResult
}
