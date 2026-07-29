package com.glass.safeclip.ui.home

data class HomeImportActions(
    val folderButtonText: String,
    val folderButtonIsPrimary: Boolean,
    val recentButtonText: String,
    val recentButtonIsPrimary: Boolean,
    val recentButtonEnabled: Boolean,
    val showRecentButton: Boolean,
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
                folderButtonText = "블랙박스 폴더 선택",
                folderButtonIsPrimary = !folderPermissionGranted,
                recentButtonText = "블랙박스 파일보기",
                recentButtonIsPrimary = ready,
                recentButtonEnabled = ready,
                showRecentButton = false,
                eventFolderEnabled = ready,
                selectedFolderText = selectedFolderName?.let { "선택된 폴더: $it" }
            )
        }
    }
}
