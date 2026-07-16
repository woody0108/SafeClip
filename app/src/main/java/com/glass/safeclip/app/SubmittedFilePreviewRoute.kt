package com.glass.safeclip.app

import com.glass.safeclip.data.file.ManagedFolderFile
import com.glass.safeclip.data.file.ManagedFolderFileDisplay
import com.glass.safeclip.data.file.ManagedFolderVideoCandidate
import com.glass.safeclip.ui.folder.FolderViewKind
import com.glass.safeclip.ui.navigation.SafeClipScreen
import com.glass.safeclip.ui.status.LocalSubmissionRecord

object SubmittedFilePreviewRoute {
    fun from(record: LocalSubmissionRecord): SafeClipScreen {
        val file = ManagedFolderFile(
            uriString = record.video.uriString,
            displayName = record.video.displayName,
            mimeType = ManagedFolderFileDisplay.mimeTypeForDisplayName(record.video.displayName),
            sizeBytes = record.video.sizeBytes
        )
        return if (ManagedFolderVideoCandidate.canPreviewImage(file)) {
            SafeClipScreen.ImagePreview(
                file = file,
                sourceKind = FolderViewKind.CurrentFolder,
                returnScreen = SafeClipScreen.SubmissionStatus
            )
        } else {
            SafeClipScreen.VideoPreview(
                video = record.video,
                returnScreen = SafeClipScreen.SubmissionStatus
            )
        }
    }
}
