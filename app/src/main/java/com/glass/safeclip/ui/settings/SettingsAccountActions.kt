package com.glass.safeclip.ui.settings

data class SettingsAccountActions(
    val signOutEnabled: Boolean,
    val signOutText: String
) {
    companion object {
        fun from(
            linkedEmail: String?,
            linkedDisplayName: String?,
            linkedProvider: String?
        ): SettingsAccountActions {
            val isLinked = !linkedEmail.isNullOrBlank() ||
                !linkedDisplayName.isNullOrBlank() ||
                !linkedProvider.isNullOrBlank()
            return SettingsAccountActions(
                signOutEnabled = isLinked,
                signOutText = "로그아웃"
            )
        }
    }
}
