package com.glass.safeclip.data.file

object ManagedFolderFileDisplay {
    fun mimeTypeForDisplayName(displayName: String): String {
        return if (isJpegDisplayName(displayName)) "image/jpeg" else "video/mp4"
    }

    fun submittedFileActionText(displayName: String): String {
        return if (isJpegDisplayName(displayName)) "사진 확인하기" else "영상 확인하기"
    }

    private fun isJpegDisplayName(displayName: String): Boolean {
        val lowerName = displayName.lowercase()
        return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")
    }
}
