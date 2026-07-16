package com.glass.safeclip.data.file

import com.glass.safeclip.domain.model.VideoCandidate

object ManagedFolderFileList {
    fun fromVideoCandidates(videos: List<VideoCandidate>): List<ManagedFolderFile> {
        return videos.map { video ->
            ManagedFolderFile(
                uriString = video.uriString,
                displayName = video.displayName,
                mimeType = "video/mp4",
                sizeBytes = video.sizeBytes
            )
        }
    }

    fun preferScannedFiles(
        scannedFiles: List<ManagedFolderFile>,
        fallbackVideos: List<VideoCandidate>
    ): List<ManagedFolderFile> {
        return scannedFiles.ifEmpty { fromVideoCandidates(fallbackVideos) }
    }
}
