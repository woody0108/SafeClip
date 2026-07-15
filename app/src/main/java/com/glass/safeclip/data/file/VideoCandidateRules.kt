package com.glass.safeclip.data.file

import com.glass.safeclip.domain.model.VideoCandidate
import java.util.Locale

object VideoCandidateRules {
    private val supportedExtensions = setOf("mp4", "mov", "avi", "ts")

    fun isSupportedVideoFile(displayName: String): Boolean {
        val extension = displayName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.ROOT)
        return extension in supportedExtensions
    }

    fun sortRecentFirst(candidates: List<VideoCandidate>): List<VideoCandidate> {
        return candidates.sortedWith(
            compareByDescending<VideoCandidate> { it.lastModifiedMillis ?: Long.MIN_VALUE }
                .thenByDescending { it.displayName }
        )
    }
}
