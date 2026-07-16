package com.glass.safeclip.app

import com.glass.safeclip.data.auth.AuthConnectionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthUserProfileFactoryTest {
    @Test
    fun `creates firestore profile from signed in auth result`() {
        val result = AuthConnectionResult.SignedIn(
            uid = "uid-123",
            displayName = "Safe User",
            email = "safe@example.com",
            provider = "google"
        )

        val profile = AuthUserProfileFactory.from(result, guestId = "Guest-ABCD")

        assertEquals("uid-123", profile?.uid)
        assertEquals("Guest-ABCD", profile?.guestId)
        assertEquals("Safe User", profile?.displayName)
        assertEquals("safe@example.com", profile?.email)
        assertEquals("google", profile?.provider)
    }

    @Test
    fun `returns null for failed auth result`() {
        val profile = AuthUserProfileFactory.from(
            AuthConnectionResult.Failed("failed"),
            guestId = "Guest-ABCD"
        )

        assertNull(profile)
    }
}
