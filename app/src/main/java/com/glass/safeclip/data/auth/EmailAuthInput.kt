package com.glass.safeclip.data.auth

data class EmailAuthInput(
    val displayName: String,
    val email: String,
    val password: String,
    val errorMessage: String?
) {
    val isValid: Boolean = errorMessage == null

    companion object {
        fun create(
            displayName: String,
            email: String,
            password: String,
            passwordConfirmation: String
        ): EmailAuthInput {
            val trimmedDisplayName = displayName.trim()
            val trimmedEmail = email.trim()
            val error = when {
                trimmedDisplayName.isBlank() -> "이름을 입력해주세요."
                !trimmedEmail.matches(Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")) -> "올바른 이메일을 입력해주세요."
                password.length < 6 -> "비밀번호는 6자 이상이어야 합니다."
                password != passwordConfirmation -> "비밀번호가 서로 다릅니다."
                else -> null
            }
            return EmailAuthInput(
                displayName = trimmedDisplayName,
                email = trimmedEmail,
                password = password,
                errorMessage = error
            )
        }
    }
}
