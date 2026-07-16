package com.glass.safeclip.data.file

class ManagedFolderFileScanner {
    fun scan(root: VideoDocument): List<ManagedFolderFile> {
        return buildList {
            scanInto(root)
        }
    }

    private fun MutableList<ManagedFolderFile>.scanInto(document: VideoDocument) {
        if (document.isDirectory) {
            document.children.forEach { scanInto(it) }
            return
        }

        val mimeType = mimeTypeFor(document.displayName) ?: return
        add(
            ManagedFolderFile(
                uriString = document.uriString,
                displayName = document.displayName,
                mimeType = mimeType,
                sizeBytes = document.sizeBytes
            )
        )
    }

    private fun mimeTypeFor(fileName: String): String? {
        val lowerName = fileName.lowercase()
        return when {
            VideoCandidateRules.isSupportedVideoFile(fileName) -> "video/mp4"
            lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") -> "image/jpeg"
            else -> null
        }
    }
}
