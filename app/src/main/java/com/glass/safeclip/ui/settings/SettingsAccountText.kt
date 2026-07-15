package com.glass.safeclip.ui.settings

data class SettingsAccountText(
    val idLine: String,
    val statusLine: String,
    val providerLine: String?
) {
    companion object {
        fun from(
            guestId: String,
            linkedEmail: String?,
            linkedDisplayName: String? = null,
            linkedProvider: String? = null
        ): SettingsAccountText {
            val accountName = linkedDisplayName?.takeIf { it.isNotBlank() }
                ?: linkedEmail?.takeIf { it.isNotBlank() }

            return SettingsAccountText(
                idLine = "ID : ${accountName ?: guestId}",
                statusLine = accountName?.let { "$it 계정 연결됨" } ?: "비회원으로 사용 중",
                providerLine = linkedProvider?.takeIf { it.isNotBlank() }?.let { "연동 방식 : $it" }
            )
        }
    }
}
