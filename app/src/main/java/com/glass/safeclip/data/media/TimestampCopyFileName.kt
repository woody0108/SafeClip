package com.glass.safeclip.data.media

object TimestampCopyFileName {
    fun forVideo(originalName: String): String {
        return "${originalName.substringBeforeLast('.', originalName)}_TIMESTAMP.mp4"
    }
}
