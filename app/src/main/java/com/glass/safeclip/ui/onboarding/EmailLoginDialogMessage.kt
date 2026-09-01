package com.glass.safeclip.ui.onboarding

object EmailLoginDialogMessage {
    fun from(authMessage: String?): String? {
        val message = authMessage?.trim().orEmpty()
        if (message.isBlank()) return null
        val isLoginFailure = listOf("로그인", "가입된 계정").any { message.contains(it) } &&
            listOf("실패", "찾지 못", "없습니다", "잘못").any { message.contains(it) }
        if (isLoginFailure) return message

        val lowerMessage = message.lowercase()
        val isFirebaseLoginFailure = listOf(
            "auth credential",
            "credential",
            "password",
            "no user",
            "invalid",
            "incorrect",
            "expired"
        ).any { lowerMessage.contains(it) }
        return if (isFirebaseLoginFailure) "로그인에 실패했습니다." else null
    }
}
