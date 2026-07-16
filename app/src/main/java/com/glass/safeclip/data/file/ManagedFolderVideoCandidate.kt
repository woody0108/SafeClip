package com.glass.safeclip.data.file

import com.glass.safeclip.domain.model.VideoCandidate

object ManagedFolderVideoCandidate {
    fun from(file: ManagedFolderFile, folderPath: String): VideoCandidate? {
        if (!canUseVideoActions(file)) return null
        return toCandidate(file, folderPath)
    }

    fun fromSubmittableFile(file: ManagedFolderFile, folderPath: String): VideoCandidate? {
        if (!canSubmitFile(file)) return null
        return toCandidate(file, folderPath)
    }

    private fun toCandidate(file: ManagedFolderFile, folderPath: String): VideoCandidate {
        return VideoCandidate(
            uriString = file.uriString,
            displayName = file.displayName,
            sizeBytes = file.sizeBytes,
            lastModifiedMillis = null,
            folderPath = folderPath
        )
    }

    fun canUseVideoActions(file: ManagedFolderFile): Boolean {
        return file.isVideoLikeFile()
    }

    fun canSubmitFile(file: ManagedFolderFile): Boolean {
        return file.isVideoLikeFile() || file.isJpegImage()
    }

    fun canPreviewImage(file: ManagedFolderFile): Boolean {
        return file.isJpegImage()
    }

    fun primaryPreviewActionText(file: ManagedFolderFile): String {
        return if (canPreviewImage(file)) "사진 미리보기" else "영상재생하기"
    }

    private fun ManagedFolderFile.isVideoLikeFile(): Boolean {
        return mimeType.startsWith("video/") || VideoCandidateRules.isSupportedVideoFile(displayName)
    }

    private fun ManagedFolderFile.isJpegImage(): Boolean {
        val lowerName = displayName.lowercase()
        return mimeType == "image/jpeg" || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")
    }
}
