package com.glass.safeclip.data.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class EmailAuthInputTest {
    @Test
    fun trimsDisplayNameAndEmailAndKeepsPassword() {
        val input = EmailAuthInput.create(
            displayName = " 베짱이들 ",
            email = " safeclip@example.com ",
            password = "123456",
            passwordConfirmation = "123456"
        )

        assertEquals("베짱이들", input.displayName)
        assertEquals("safeclip@example.com", input.email)
        assertEquals("123456", input.password)
    }

    @Test
    fun rejectsBlankDisplayName() {
        val input = EmailAuthInput.create("", "safeclip@example.com", "123456", "123456")

        assertEquals("이름을 입력해주세요.", input.errorMessage)
    }

    @Test
    fun rejectsInvalidEmail() {
        val input = EmailAuthInput.create("베짱이들", "safeclip", "123456", "123456")

        assertEquals("올바른 이메일을 입력해주세요.", input.errorMessage)
    }

    @Test
    fun rejectsShortPassword() {
        val input = EmailAuthInput.create("베짱이들", "safeclip@example.com", "123", "123")

        assertEquals("비밀번호는 6자 이상이어야 합니다.", input.errorMessage)
    }

    @Test
    fun rejectsMismatchedPasswordConfirmation() {
        val input = EmailAuthInput.create("베짱이들", "safeclip@example.com", "123456", "654321")

        assertEquals("비밀번호가 서로 다릅니다.", input.errorMessage)
    }
}
