package com.glass.safeclip.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmailLoginDialogMessageTest {
    @Test
    fun showsLoginFailureMessageInsideDialog() {
        assertEquals(
            "로그인에 실패했습니다.",
            EmailLoginDialogMessage.from("로그인에 실패했습니다.")
        )
    }

    @Test
    fun hidesSubmissionRefreshMessageInsideDialog() {
        assertNull(EmailLoginDialogMessage.from("제출내역 0개를 불러왔습니다."))
    }

    @Test
    fun normalizesFirebaseCredentialError() {
        assertEquals(
            "로그인에 실패했습니다.",
            EmailLoginDialogMessage.from("The supplied auth credential is incorrect, malformed or has expired.")
        )
    }
}
