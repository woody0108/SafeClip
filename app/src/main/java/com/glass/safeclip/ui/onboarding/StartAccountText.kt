package com.glass.safeclip.ui.onboarding

data class StartAccountText(
    val idLine: String,
    val actionText: String,
    val signupEnabled: Boolean,
    val loginEnabled: Boolean,
    val statusBadge: String?
) {
    companion object {
        fun from(
            guestId: String,
            displayName: String?,
            email: String?,
            authMessage: String? = null
        ): StartAccountText {
            val linkedName = displayName?.takeIf { it.isNotBlank() }
                ?: email?.takeIf { it.isNotBlank() }
            val failureBadge = if (authMessage.isAuthFailureMessage()) "실패" else null

            return if (linkedName == null) {
                StartAccountText(
                    idLine = "ID : $guestId",
                    actionText = "회원가입",
                    signupEnabled = true,
                    loginEnabled = true,
                    statusBadge = failureBadge
                )
            } else {
                StartAccountText(
                    idLine = "ID : $linkedName",
                    actionText = "연동완료",
                    signupEnabled = false,
                    loginEnabled = false,
                    statusBadge = failureBadge
                )
            }
        }

        private fun String?.isAuthFailureMessage(): Boolean {
            val message = this?.trim().orEmpty()
            if (message.isBlank()) return false
            return listOf("실패", "취소", "읽지 못", "찾지 못", "없습니다", "잘못").any { keyword ->
                message.contains(keyword)
            }
        }
    }
}
