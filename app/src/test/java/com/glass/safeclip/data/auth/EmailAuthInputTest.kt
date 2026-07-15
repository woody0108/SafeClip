package com.glass.safeclip.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailAuthInputTest {
    @Test
    fun trimsEmailAndKeepsPassword() {
        val input = EmailAuthInput.create(" safeclip@example.com ", "123456")

        assertEquals("safeclip@example.com", input.email)
        assertEquals("123456", input.password)
    }

    @Test
    fun rejectsInvalidEmail() {
        val input = EmailAuthInput.create("safeclip", "123456")

        assertTrue(input.errorMessage!!.contains("이메일"))
    }

    @Test
    fun rejectsShortPassword() {
        val input = EmailAuthInput.create("safeclip@example.com", "123")

        assertTrue(input.errorMessage!!.contains("6자"))
    }
}
