package com.glass.safeclip.app

import com.glass.safeclip.data.file.ManagedFolderFile
import com.glass.safeclip.data.file.ManagedFolderFileScanner
import com.glass.safeclip.data.file.VideoDocument
import com.glass.safeclip.data.file.VideoScanner
import com.glass.safeclip.ui.video.FolderPickerResultText
import com.glass.safeclip.ui.video.VideoListState

data class FolderLoadResult(
    val state: VideoListState,
    val files: List<ManagedFolderFile>
)

class FolderLoadCoordinator(
    private val videoScanner: VideoScanner,
    private val managedFileScanner: ManagedFolderFileScanner
) {
    fun fromRoot(
        root: VideoDocument?,
        selectedUriString: String,
        fallbackName: String,
        savedMediaItemCount: Int
    ): FolderLoadResult {
        val videos = root?.let(videoScanner::scan).orEmpty()
        val files = root?.let(managedFileScanner::scan).orEmpty()
        return FolderLoadResult(
            state = VideoListState(
                selectedFolderName = root?.displayName ?: fallbackName,
                selectedFolderUriString = selectedUriString,
                isLoading = false,
                videos = videos,
                savedMediaItemCount = savedMediaItemCount,
                errorMessage = FolderPickerResultText.messageForVideoCount(videos.size)
                    .takeIf { videos.isEmpty() }
            ),
            files = files
        )
    }
}
