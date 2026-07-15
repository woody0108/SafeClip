package com.glass.safeclip.data.auth

enum class AuthProviderOption(
    val buttonText: String
) {
    Google("Google로 계속하기"),
    Email("이메일로 가입하기")
}

object SafeClipAuthProviders {
    val guestLinkingOptions: List<AuthProviderOption> = listOf(
        AuthProviderOption.Google,
        AuthProviderOption.Email
    )
}
