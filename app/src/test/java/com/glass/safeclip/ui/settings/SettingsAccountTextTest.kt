package com.glass.safeclip.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsAccountTextTest {
    @Test
    fun guestAccountShowsGuestStatus() {
        val text = SettingsAccountText.from(
            guestId = "Guest-ABCD-1234",
            linkedEmail = null
        )

        assertEquals("ID : Guest-ABCD-1234", text.idLine)
        assertEquals("비회원으로 사용 중", text.statusLine)
    }

    @Test
    fun linkedAccountShowsEmailStatus() {
        val text = SettingsAccountText.from(
            guestId = "Guest-ABCD-1234",
            linkedEmail = "safeclip@example.com"
        )

        assertEquals("ID : Guest-ABCD-1234", text.idLine)
        assertEquals("safeclip@example.com 계정 연결됨", text.statusLine)
    }
}
