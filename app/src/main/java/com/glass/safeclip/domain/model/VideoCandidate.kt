package com.glass.safeclip.domain.model

data class VideoCandidate(
    val uriString: String,
    val displayName: String,
    val sizeBytes: Long?,
    val lastModifiedMillis: Long?,
    val folderPath: String
)
