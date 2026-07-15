package com.glass.safeclip.data.media

object CaptureFileName {
    fun forVideo(displayName: String, positionMs: Long): String {
        val nameWithoutExtension = displayName.substringBeforeLast('.', missingDelimiterValue = displayName)
        val safeName = nameWithoutExtension
            .replace(Regex("[^A-Za-z0-9_-]+"), "_")
            .trim('_')
            .ifBlank { "capture" }
        return "${safeName}_${positionMs}ms.jpg"
    }
}
