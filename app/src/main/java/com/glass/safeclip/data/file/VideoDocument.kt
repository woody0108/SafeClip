package com.glass.safeclip.data.file

data class VideoDocument(
    val displayName: String,
    val uriString: String,
    val isDirectory: Boolean,
    val sizeBytes: Long?,
    val lastModifiedMillis: Long?,
    val children: List<VideoDocument>
) {
    companion object {
        fun folder(displayName: String, uriString: String, children: List<VideoDocument>): VideoDocument {
            return VideoDocument(
                displayName = displayName,
                uriString = uriString,
                isDirectory = true,
                sizeBytes = null,
                lastModifiedMillis = null,
                children = children
            )
        }

        fun file(displayName: String, uriString: String, sizeBytes: Long?, lastModifiedMillis: Long?): VideoDocument {
            return VideoDocument(
                displayName = displayName,
                uriString = uriString,
                isDirectory = false,
                sizeBytes = sizeBytes,
                lastModifiedMillis = lastModifiedMillis,
                children = emptyList()
            )
        }
    }
}
