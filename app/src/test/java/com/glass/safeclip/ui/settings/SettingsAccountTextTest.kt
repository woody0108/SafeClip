package com.glass.safeclip.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsAccountTextTest {
    @Test
    fun guestAccountShowsGuestIdAndStatus() {
        val text = SettingsAccountText.from(
            guestId = "Guest-ABCD-1234",
            linkedEmail = null
        )

        assertEquals("ID : Guest-ABCD-1234", text.idLine)
        assertEquals("비회원으로 사용 중", text.statusLine)
        assertEquals(null, text.providerLine)
    }

    @Test
    fun linkedAccountUsesDisplayNameAsIdWhenAvailable() {
        val text = SettingsAccountText.from(
            guestId = "Guest-ABCD-1234",
            linkedEmail = "safeclip@example.com",
            linkedDisplayName = "베짱이들"
        )

        assertEquals("ID : 베짱이들", text.idLine)
        assertEquals("베짱이들 계정 연결됨", text.statusLine)
    }

    @Test
    fun linkedAccountUsesEmailAsIdWhenDisplayNameIsMissing() {
        val text = SettingsAccountText.from(
            guestId = "Guest-ABCD-1234",
            linkedEmail = "safeclip@example.com",
            linkedDisplayName = null
        )

        assertEquals("ID : safeclip@example.com", text.idLine)
        assertEquals("safeclip@example.com 계정 연결됨", text.statusLine)
    }

    @Test
    fun linkedAccountCanShowProvider() {
        val text = SettingsAccountText.from(
            guestId = "Guest-ABCD-1234",
            linkedEmail = "safeclip@example.com",
            linkedProvider = "google"
        )

        assertEquals("연동 방식 : google", text.providerLine)
    }
}
