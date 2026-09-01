package com.glass.safeclip.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartAccountTextTest {
    @Test
    fun guestAccountShowsGuestIdAndAllowsSignupAndLogin() {
        val text = StartAccountText.from(
            guestId = "Guest-C2D0-3702",
            displayName = null,
            email = null
        )

        assertEquals("ID : Guest-C2D0-3702", text.idLine)
        assertEquals("회원가입", text.actionText)
        assertTrue(text.signupEnabled)
        assertTrue(text.loginEnabled)
    }

    @Test
    fun linkedAccountShowsDisplayNameAndDisablesSignupAndLogin() {
        val text = StartAccountText.from(
            guestId = "Guest-C2D0-3702",
            displayName = "베짱이들",
            email = "woody08431@gmail.com"
        )

        assertEquals("ID : 베짱이들", text.idLine)
        assertEquals("연동완료", text.actionText)
        assertFalse(text.signupEnabled)
        assertFalse(text.loginEnabled)
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
        assertFalse(text.loginEnabled)
    }

    @Test
    fun failureMessageShowsFailureBadgeBesideId() {
        val text = StartAccountText.from(
            guestId = "Guest-C2D0-3702",
            displayName = null,
            email = null,
            authMessage = "이메일 로그인에 실패했습니다."
        )

        assertEquals("실패", text.statusBadge)
    }

    @Test
    fun successMessageDoesNotShowFailureBadgeBesideId() {
        val text = StartAccountText.from(
            guestId = "Guest-C2D0-3702",
            displayName = "베짱이들",
            email = "woody08431@gmail.com",
            authMessage = "베짱이들 계정으로 연결되었습니다."
        )

        assertEquals(null, text.statusBadge)
    }
}
