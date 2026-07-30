package com.glass.safeclip.app

import com.glass.safeclip.data.file.ManagedFolderFile
import com.glass.safeclip.data.file.ManagedFolderFileDisplay
import com.glass.safeclip.data.file.ManagedFolderVideoCandidate
import com.glass.safeclip.ui.folder.FolderViewKind
import com.glass.safeclip.ui.navigation.SafeClipScreen
import com.glass.safeclip.ui.status.LocalSubmissionRecord
import java.net.URLEncoder

object SubmittedFilePreviewRoute {
    fun from(
        record: LocalSubmissionRecord,
        nasUploadUrl: String = ""
    ): SafeClipScreen {
        val previewUriString = previewUriString(
            record = record,
            nasUploadUrl = nasUploadUrl
        )
        val file = ManagedFolderFile(
            uriString = previewUriString,
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
                video = record.video.copy(uriString = previewUriString),
                returnScreen = SafeClipScreen.SubmissionStatus
            )
        }
    }

    private fun previewUriString(
        record: LocalSubmissionRecord,
        nasUploadUrl: String
    ): String {
        if (record.video.uriString.contains("://")) {
            return record.video.uriString
        }

        val baseUrl = reviewMediaBaseUrl(nasUploadUrl)
        if (baseUrl.isBlank()) {
            return record.video.uriString
        }

        return "$baseUrl?id=${record.id.urlEncode()}&file=0"
    }

    fun reviewMediaBaseUrl(nasUploadUrl: String): String {
        val cleanUrl = nasUploadUrl.trim()
        val uploadPath = "/api/nas-upload-api/public/upload.php"
        return if (cleanUrl.endsWith(uploadPath)) {
            cleanUrl.removeSuffix(uploadPath) + "/api/video.php"
        } else {
            ""
        }
    }

    private fun String.urlEncode(): String {
        return URLEncoder.encode(this, Charsets.UTF_8.name())
    }
}
