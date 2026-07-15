package com.glass.safeclip.data.file

data class ManagedFolderFile(
    val uriString: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long?
)
