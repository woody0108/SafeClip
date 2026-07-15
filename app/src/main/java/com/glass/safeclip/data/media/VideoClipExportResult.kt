package com.glass.safeclip.data.media

import com.glass.safeclip.domain.model.VideoCandidate
import java.io.File

data class VideoClipExportResult(
    val outputFile: File,
    val outputUriString: String?,
    val savedDisplayPath: String,
    val originalUriString: String,
    val originalDisplayName: String,
    val originalSizeBytes: Long?,
    val originalLastModifiedMillis: Long?,
    val originalFolderPath: String,
    val startMs: Long,
    val endMs: Long
) {
    companion object {
        fun from(
            outputFile: File,
            outputUriString: String?,
            savedDisplayPath: String,
            originalVideo: VideoCandidate,
            selection: VideoClipSelection
        ): VideoClipExportResult {
            require(selection.isComplete) { "저장할 시작/끝 구간을 먼저 지정해주세요." }
            return VideoClipExportResult(
                outputFile = outputFile,
                outputUriString = outputUriString,
                savedDisplayPath = savedDisplayPath,
                originalUriString = originalVideo.uriString,
                originalDisplayName = originalVideo.displayName,
                originalSizeBytes = originalVideo.sizeBytes,
                originalLastModifiedMillis = originalVideo.lastModifiedMillis,
                originalFolderPath = originalVideo.folderPath,
                startMs = selection.startMs!!,
                endMs = selection.endMs!!
            )
        }
    }
}
