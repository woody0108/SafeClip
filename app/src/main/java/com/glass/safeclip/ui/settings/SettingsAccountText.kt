package com.glass.safeclip.ui.settings

data class SettingsAccountText(
    val idLine: String,
    val statusLine: String
) {
    companion object {
        fun from(guestId: String, linkedEmail: String?): SettingsAccountText {
            return SettingsAccountText(
                idLine = "ID : $guestId",
                statusLine = linkedEmail?.takeIf { it.isNotBlank() }
                    ?.let { "$it 계정 연결됨" }
                    ?: "비회원으로 사용 중"
            )
        }
    }
}
