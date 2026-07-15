package com.glass.safeclip.data.media

object ClipFileName {
    fun forVideo(displayName: String, startMs: Long, endMs: Long): String {
        val nameWithoutExtension = displayName.substringBeforeLast('.', missingDelimiterValue = displayName)
        val safeName = nameWithoutExtension
            .replace(Regex("[^A-Za-z0-9_-]+"), "_")
            .trim('_')
            .ifBlank { "clip" }
        return "${safeName}_clip_${startMs}ms_${endMs}ms.mp4"
    }
}
