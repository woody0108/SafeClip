package com.glass.safeclip.data.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class SafeClipAuthProviderTest {
    @Test
    fun guestLinkingStartsWithGoogleAndEmailOnly() {
        assertEquals(
            listOf(AuthProviderOption.Google, AuthProviderOption.Email),
            SafeClipAuthProviders.guestLinkingOptions
        )
    }

    @Test
    fun providerLabelsMatchSafeClipSignupCopy() {
        assertEquals("Google로 계속하기", AuthProviderOption.Google.buttonText)
        assertEquals("이메일로 가입하기", AuthProviderOption.Email.buttonText)
    }
}
