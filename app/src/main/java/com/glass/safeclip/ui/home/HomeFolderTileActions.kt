package com.glass.safeclip.ui.home

data class HomeFolderTileActions(
    val blackboxFolderViewIsPrimary: Boolean,
    val safeClipFolderViewIsPrimary: Boolean
) {
    companion object {
        fun from(
            folderPermissionGranted: Boolean,
            safeClipVideoCount: Int,
            safeClipPhotoCount: Int
        ): HomeFolderTileActions {
            return HomeFolderTileActions(
                blackboxFolderViewIsPrimary = folderPermissionGranted,
                safeClipFolderViewIsPrimary = safeClipVideoCount + safeClipPhotoCount > 0
            )
        }
    }
}
