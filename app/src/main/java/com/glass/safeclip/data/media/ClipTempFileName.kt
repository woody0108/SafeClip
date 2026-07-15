package com.glass.safeclip.data.media

object ClipTempFileName {
    private val supportedExtensions = setOf("mp4", "mov", "avi", "ts")

    fun forSourceCopy(displayName: String): String {
        val nameWithoutExtension = displayName.substringBeforeLast('.', missingDelimiterValue = displayName)
        val extension = displayName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
            .takeIf { it in supportedExtensions }
            ?: "mp4"
        val safeName = nameWithoutExtension
            .replace(Regex("[^A-Za-z0-9_-]+"), "_")
            .trim('_')
            .ifBlank { "source" }
        return "${safeName}_source.$extension"
    }
}
