package com.glass.safeclip.ui.home

data class HomeImportActions(
    val folderButtonText: String,
    val folderButtonIsPrimary: Boolean,
    val recentButtonText: String,
    val recentButtonIsPrimary: Boolean,
    val recentButtonEnabled: Boolean,
    val eventFolderEnabled: Boolean,
    val selectedFolderText: String?
) {
    companion object {
        fun from(
            selectedFolderName: String?,
            folderPermissionGranted: Boolean,
            cameraPermissionGranted: Boolean
        ): HomeImportActions {
            val ready = folderPermissionGranted && cameraPermissionGranted
            return HomeImportActions(
                folderButtonText = "폴더 선택하기",
                folderButtonIsPrimary = !folderPermissionGranted,
                recentButtonText = "최근 이벤트 보기",
                recentButtonIsPrimary = ready,
                recentButtonEnabled = ready,
                eventFolderEnabled = ready,
                selectedFolderText = selectedFolderName?.let { "선택된 폴더: $it" }
            )
        }
    }
}
