package com.glass.safeclip.ui.home

data class HomeStatusSummary(
    val savedEventFolderName: String,
    val savedEventCountText: String,
    val currentFolderCountText: String
) {
    companion object {
        fun from(savedMediaItemCount: Int, currentFolderVideoCount: Int): HomeStatusSummary {
            return HomeStatusSummary(
                savedEventFolderName = "SafeClip Captures",
                savedEventCountText = "${savedMediaItemCount}개",
                currentFolderCountText = "${currentFolderVideoCount}개"
            )
        }
    }
}
