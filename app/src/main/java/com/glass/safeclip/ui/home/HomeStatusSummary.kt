package com.glass.safeclip.ui.home

data class HomeStatusSummary(
    val savedEventFolderName: String,
    val savedEventCountText: String,
    val currentFolderCountText: String,
    val submissionCountText: String
) {
    companion object {
        fun from(
            savedMediaPermissionGranted: Boolean,
            savedEventVideoCount: Int,
            savedEventPhotoCount: Int,
            currentFolderVideoCount: Int,
            currentFolderPhotoCount: Int,
            submissionCount: Int = 0
        ): HomeStatusSummary {
            return HomeStatusSummary(
                savedEventFolderName = "SafeClip",
                savedEventCountText = if (savedMediaPermissionGranted) {
                    "영상 ${savedEventVideoCount}개\n사진 ${savedEventPhotoCount}개"
                } else {
                    "폴더 권한을\n추가해주세요"
                },
                currentFolderCountText = "영상 ${currentFolderVideoCount}개\n사진 ${currentFolderPhotoCount}개",
                submissionCountText = "제출 ${submissionCount}개"
            )
        }
    }
}
