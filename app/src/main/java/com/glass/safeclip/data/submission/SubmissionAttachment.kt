package com.glass.safeclip.data.submission

import com.glass.safeclip.data.file.ManagedFolderFile
import com.glass.safeclip.data.file.VideoCandidateRules
import com.glass.safeclip.domain.model.VideoCandidate

data class SubmissionAttachment(
    val uriString: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long?,
    val folderPath: String,
    val kind: SubmissionAttachmentKind,
    val nasStoredName: String? = null,
    val nasRelativePath: String? = null,
    val uploadedSizeBytes: Long? = null
) {
    fun toFirestoreFields(): Map<String, Any?> {
        return mapOf(
            "uriString" to uriString,
            "displayName" to displayName,
            "mimeType" to mimeType,
            "sizeBytes" to sizeBytes,
            "folderPath" to folderPath,
            "kind" to kind.firestoreValue,
            "nasStoredName" to nasStoredName,
            "nasRelativePath" to nasRelativePath,
            "uploadedSizeBytes" to uploadedSizeBytes
        )
    }

    fun toRepresentativeVideoCandidate(): VideoCandidate {
        return VideoCandidate(
            uriString = uriString,
            displayName = displayName,
            sizeBytes = sizeBytes,
            lastModifiedMillis = null,
            folderPath = folderPath
        )
    }

    fun withUploadResponse(response: NasSubmissionUploadResponse): SubmissionAttachment {
        return copy(
            nasStoredName = response.storedName,
            nasRelativePath = response.relativePath,
            uploadedSizeBytes = response.sizeBytes
        )
    }

    companion object {
        fun fromManagedFile(file: ManagedFolderFile, folderPath: String): SubmissionAttachment? {
            val kind = when {
                file.isVideoLikeFile() -> SubmissionAttachmentKind.Video
                file.isJpegImage() -> SubmissionAttachmentKind.Photo
                else -> null
            } ?: return null

            return SubmissionAttachment(
                uriString = file.uriString,
                displayName = file.displayName,
                mimeType = file.mimeType,
                sizeBytes = file.sizeBytes,
                folderPath = folderPath,
                kind = kind
            )
        }

        fun fromVideoCandidate(video: VideoCandidate): SubmissionAttachment {
            return SubmissionAttachment(
                uriString = video.uriString,
                displayName = video.displayName,
                mimeType = "video/mp4",
                sizeBytes = video.sizeBytes,
                folderPath = video.folderPath,
                kind = SubmissionAttachmentKind.Video
            )
        }

        private fun ManagedFolderFile.isVideoLikeFile(): Boolean {
            return mimeType.startsWith("video/") || VideoCandidateRules.isSupportedVideoFile(displayName)
        }

        private fun ManagedFolderFile.isJpegImage(): Boolean {
            val lowerName = displayName.lowercase()
            return mimeType == "image/jpeg" || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")
        }
    }
}

enum class SubmissionAttachmentKind(val firestoreValue: String) {
    Video("video"),
    Photo("photo")
}

enum class SubmissionAttachmentFilter(val label: String) {
    All("전체"),
    Photos("사진"),
    Videos("영상");

    fun apply(attachments: List<SubmissionAttachment>): List<SubmissionAttachment> {
        return when (this) {
            All -> attachments
            Photos -> attachments.filter { it.kind == SubmissionAttachmentKind.Photo }
            Videos -> attachments.filter { it.kind == SubmissionAttachmentKind.Video }
        }
    }
}

object SubmissionAttachmentCandidateDisplay {
    const val PreviewLimit = 6

    fun visible(
        candidates: List<SubmissionAttachment>,
        showAll: Boolean
    ): List<SubmissionAttachment> {
        return if (showAll) candidates else candidates.take(PreviewLimit)
    }
}

sealed interface AttachmentSelectionResult {
    data class Accepted(val attachments: List<SubmissionAttachment>) : AttachmentSelectionResult
    data class Rejected(val message: String) : AttachmentSelectionResult
}

object SubmissionAttachmentRules {
    const val MaxVideos = 2
    const val MaxPhotos = 5
    const val MaxVideoBytes = 500L * 1024L * 1024L
    const val MaxPhotoBytes = 50L * 1024L * 1024L

    fun add(
        current: List<SubmissionAttachment>,
        next: SubmissionAttachment
    ): AttachmentSelectionResult {
        if (current.any { it.uriString == next.uriString }) {
            return AttachmentSelectionResult.Rejected("이미 첨부한 파일입니다.")
        }
        if (next.kind == SubmissionAttachmentKind.Video && !isUnderSizeLimit(next, MaxVideoBytes)) {
            return AttachmentSelectionResult.Rejected("영상은 500MB 미만 파일만 첨부할 수 있습니다.")
        }
        if (next.kind == SubmissionAttachmentKind.Photo && !isUnderSizeLimit(next, MaxPhotoBytes)) {
            return AttachmentSelectionResult.Rejected("사진은 50MB 미만 파일만 첨부할 수 있습니다.")
        }

        val videos = current.count { it.kind == SubmissionAttachmentKind.Video }
        val photos = current.count { it.kind == SubmissionAttachmentKind.Photo }
        if (next.kind == SubmissionAttachmentKind.Video && videos >= MaxVideos) {
            return AttachmentSelectionResult.Rejected("영상은 최대 2개까지 첨부할 수 있습니다.")
        }
        if (next.kind == SubmissionAttachmentKind.Photo && photos >= MaxPhotos) {
            return AttachmentSelectionResult.Rejected("사진은 최대 5개까지 첨부할 수 있습니다.")
        }

        return AttachmentSelectionResult.Accepted(current + next)
    }

    private fun isUnderSizeLimit(attachment: SubmissionAttachment, maxBytes: Long): Boolean {
        return (attachment.sizeBytes ?: 0L) in 1 until maxBytes
    }
}
