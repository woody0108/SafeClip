package com.glass.safeclip.data.recording

enum class RecordingQuality {
    FHD,
    HD,
    SD;

    companion object {
        fun select(
            requested: RecordingQuality,
            supported: Set<RecordingQuality>
        ): RecordingQuality {
            require(supported.isNotEmpty()) { "At least one recording quality must be supported." }

            return entries
                .drop(requested.ordinal)
                .firstOrNull(supported::contains)
                ?: throw IllegalArgumentException("No supported quality at or below $requested.")
        }
    }
}
