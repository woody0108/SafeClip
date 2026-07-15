package com.glass.safeclip.ui.onboarding

data class StartAccountText(
    val idLine: String,
    val actionText: String,
    val signupEnabled: Boolean
) {
    companion object {
        fun from(
            guestId: String,
            displayName: String?,
            email: String?
        ): StartAccountText {
            val linkedName = displayName?.takeIf { it.isNotBlank() }
                ?: email?.takeIf { it.isNotBlank() }

            return if (linkedName == null) {
                StartAccountText(
                    idLine = "ID : $guestId",
                    actionText = "회원가입",
                    signupEnabled = true
                )
            } else {
                StartAccountText(
                    idLine = "ID : $linkedName",
                    actionText = "연동완료",
                    signupEnabled = false
                )
            }
        }
    }
}
