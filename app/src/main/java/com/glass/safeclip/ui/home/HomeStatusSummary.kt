package com.glass.safeclip.ui.home

data class HomeStatusSummary(
    val savedEventFolderName: String,
    val savedEventCountText: String,
    val currentFolderCountText: String,
    val submissionCountText: String
) {
    companion object {
        fun from(
            savedMediaItemCount: Int,
            currentFolderVideoCount: Int,
            submissionCount: Int = 0
        ): HomeStatusSummary {
            return HomeStatusSummary(
                savedEventFolderName = "SafeClip Captures",
                savedEventCountText = "${savedMediaItemCount}개",
                currentFolderCountText = "${currentFolderVideoCount}개",
                submissionCountText = "제출 ${submissionCount}개"
            )
        }
    }
}
