package com.glass.safeclip.app

import com.glass.safeclip.data.file.ManagedFolderFile
import com.glass.safeclip.data.file.ManagedFolderFileList
import com.glass.safeclip.domain.model.VideoCandidate
import com.glass.safeclip.ui.folder.FolderViewKind

object FolderManagerFileSelector {
    fun select(
        kind: FolderViewKind,
        eventFolderFiles: List<ManagedFolderFile>,
        currentFolderFiles: List<ManagedFolderFile>,
        fallbackVideos: List<VideoCandidate>
    ): List<ManagedFolderFile> {
        return when (kind) {
            FolderViewKind.SafeClipSaved -> eventFolderFiles
            FolderViewKind.CurrentFolder -> ManagedFolderFileList.preferScannedFiles(
                scannedFiles = currentFolderFiles,
                fallbackVideos = fallbackVideos
            )
        }
    }
}
