package com.glass.safeclip.data.auth

data class EmailLoginInput(
    val email: String,
    val password: String,
    val errorMessage: String?
) {
    val isValid: Boolean = errorMessage == null

    companion object {
        fun create(email: String, password: String): EmailLoginInput {
            val trimmedEmail = email.trim()
            val error = when {
                !trimmedEmail.matches(Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")) -> "올바른 이메일을 입력해주세요."
                password.isBlank() -> "비밀번호를 입력해주세요."
                else -> null
            }
            return EmailLoginInput(
                email = trimmedEmail,
                password = password,
                errorMessage = error
            )
        }
    }
}
