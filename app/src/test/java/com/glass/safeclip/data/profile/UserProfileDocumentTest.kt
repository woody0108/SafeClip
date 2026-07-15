package com.glass.safeclip.data.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileDocumentTest {
    @Test
    fun createsUserDocumentPathFromUid() {
        val profile = UserProfile(
            uid = "uid-123",
            guestId = "Guest-ABCD-1234",
            email = "safeclip@example.com",
            displayName = "SafeClip",
            provider = "google"
        )

        assertEquals("users/uid-123", profile.documentPath)
    }

    @Test
    fun firstSaveIncludesCreatedAtAndLastLoginAt() {
        val profile = UserProfile("uid-123", "Guest-ABCD-1234", "safeclip@example.com", "SafeClip", "google")

        val fields = UserProfileDocument.createFields(profile)

        assertTrue(fields.containsKey("createdAt"))
        assertTrue(fields.containsKey("lastLoginAt"))
        assertEquals("Guest-ABCD-1234", fields["guestId"])
    }

    @Test
    fun loginUpdateOnlyRefreshesLastLoginAtAndProfileFields() {
        val profile = UserProfile("uid-123", "Guest-ABCD-1234", "safeclip@example.com", "SafeClip", "google")

        val fields = UserProfileDocument.loginUpdateFields(profile)

        assertFalse(fields.containsKey("createdAt"))
        assertTrue(fields.containsKey("lastLoginAt"))
        assertEquals("google", fields["provider"])
    }
}
