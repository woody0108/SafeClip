package com.glass.safeclip.data.auth

enum class AuthProviderOption(
    val buttonText: String,
    val signUpText: String,
    val loginText: String
) {
    Google(
        buttonText = "Google로 계속하기",
        signUpText = "Google로 가입하기",
        loginText = "Google로 로그인하기"
    ),
    Email(
        buttonText = "이메일로 가입하기",
        signUpText = "이메일로 가입하기",
        loginText = "이메일로 로그인하기"
    )
}

object SafeClipAuthProviders {
    val guestLinkingOptions: List<AuthProviderOption> = listOf(
        AuthProviderOption.Google,
        AuthProviderOption.Email
    )
}
