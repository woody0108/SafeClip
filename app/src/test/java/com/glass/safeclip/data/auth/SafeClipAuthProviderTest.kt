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

    @Test
    fun providerLabelsCanUseSignupAndLoginCopySeparately() {
        assertEquals("Google로 가입하기", AuthProviderOption.Google.signUpText)
        assertEquals("이메일로 가입하기", AuthProviderOption.Email.signUpText)
        assertEquals("Google로 로그인하기", AuthProviderOption.Google.loginText)
        assertEquals("이메일로 로그인하기", AuthProviderOption.Email.loginText)
    }
}
