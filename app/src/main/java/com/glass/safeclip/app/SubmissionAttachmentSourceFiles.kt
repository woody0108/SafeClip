package com.glass.safeclip.app

import com.glass.safeclip.data.file.ManagedFolderFile
import com.glass.safeclip.data.file.ManagedFolderFileList
import com.glass.safeclip.domain.model.VideoCandidate

data class SubmissionAttachmentSourceFiles(
    val selectedFolderFiles: List<ManagedFolderFile>,
    val eventFolderFiles: List<ManagedFolderFile>
) {
    companion object {
        fun from(
            currentFolderFiles: List<ManagedFolderFile>,
            eventFolderFiles: List<ManagedFolderFile>,
            fallbackVideos: List<VideoCandidate>
        ): SubmissionAttachmentSourceFiles {
            return SubmissionAttachmentSourceFiles(
                selectedFolderFiles = ManagedFolderFileList.preferScannedFiles(
                    scannedFiles = currentFolderFiles,
                    fallbackVideos = fallbackVideos
                ),
                eventFolderFiles = eventFolderFiles
            )
        }
    }
}
