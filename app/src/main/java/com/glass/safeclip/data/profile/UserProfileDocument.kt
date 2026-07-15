package com.glass.safeclip.data.profile

import com.google.firebase.firestore.FieldValue

object UserProfileDocument {
    fun createFields(profile: UserProfile): Map<String, Any?> {
        return baseFields(profile) + mapOf(
            "createdAt" to FieldValue.serverTimestamp(),
            "lastLoginAt" to FieldValue.serverTimestamp()
        )
    }

    fun loginUpdateFields(profile: UserProfile): Map<String, Any?> {
        return baseFields(profile) + mapOf(
            "lastLoginAt" to FieldValue.serverTimestamp()
        )
    }

    fun fromFirestore(uid: String, data: Map<String, Any?>): UserProfile {
        return UserProfile(
            uid = uid,
            guestId = data["guestId"] as? String ?: "",
            email = data["email"] as? String,
            displayName = data["displayName"] as? String,
            provider = data["provider"] as? String ?: "unknown"
        )
    }

    private fun baseFields(profile: UserProfile): Map<String, Any?> {
        return mapOf(
            "uid" to profile.uid,
            "guestId" to profile.guestId,
            "email" to profile.email,
            "displayName" to profile.displayName,
            "provider" to profile.provider
        )
    }
}
