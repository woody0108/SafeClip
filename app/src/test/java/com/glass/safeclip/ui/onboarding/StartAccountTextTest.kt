package com.glass.safeclip.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartAccountTextTest {
    @Test
    fun guestAccountShowsGuestIdAndAllowsSignup() {
        val text = StartAccountText.from(
            guestId = "Guest-C2D0-3702",
            displayName = null,
            email = null
        )

        assertEquals("ID : Guest-C2D0-3702", text.idLine)
        assertEquals("회원가입", text.actionText)
        assertTrue(text.signupEnabled)
    }

    @Test
    fun linkedAccountShowsDisplayNameAndDisablesSignup() {
        val text = StartAccountText.from(
            guestId = "Guest-C2D0-3702",
            displayName = "베짱이들",
            email = "woody08431@gmail.com"
        )

        assertEquals("ID : 베짱이들", text.idLine)
        assertEquals("연동완료", text.actionText)
        assertFalse(text.signupEnabled)
    }

    @Test
    fun linkedAccountFallsBackToEmailWhenDisplayNameIsBlank() {
        val text = StartAccountText.from(
            guestId = "Guest-C2D0-3702",
            displayName = "",
            email = "safeclip@example.com"
        )

        assertEquals("ID : safeclip@example.com", text.idLine)
        assertFalse(text.signupEnabled)
    }
}
