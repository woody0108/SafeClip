package com.glass.safeclip.data.auth

data class EmailAuthInput(
    val email: String,
    val password: String,
    val errorMessage: String?
) {
    val isValid: Boolean = errorMessage == null

    companion object {
        fun create(email: String, password: String): EmailAuthInput {
            val trimmedEmail = email.trim()
            val error = when {
                !trimmedEmail.contains("@") || !trimmedEmail.contains(".") -> "올바른 이메일을 입력해주세요."
                password.length < 6 -> "비밀번호는 6자 이상이어야 합니다."
                else -> null
            }
            return EmailAuthInput(
                email = trimmedEmail,
                password = password,
                errorMessage = error
            )
        }
    }
}
