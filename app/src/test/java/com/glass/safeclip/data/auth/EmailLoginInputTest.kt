package com.glass.safeclip.data.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class EmailLoginInputTest {
    @Test
    fun trimsEmailAndKeepsPassword() {
        val input = EmailLoginInput.create(" safeclip@example.com ", "123456")

        assertEquals("safeclip@example.com", input.email)
        assertEquals("123456", input.password)
    }

    @Test
    fun rejectsInvalidEmail() {
        val input = EmailLoginInput.create("safeclip", "123456")

        assertEquals("올바른 이메일을 입력해주세요.", input.errorMessage)
    }

    @Test
    fun rejectsBlankPassword() {
        val input = EmailLoginInput.create("safeclip@example.com", "")

        assertEquals("비밀번호를 입력해주세요.", input.errorMessage)
    }
}
