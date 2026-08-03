package com.glass.safeclip.data.submission

import java.time.LocalDate

object SubmissionFileNameMetadata {
    private val blackboxDateTimePattern =
        Regex("""(?:EVT|REC)_(\d{4})_(\d{2})_(\d{2})_(\d{2})_(\d{2})(?:_(\d{2}))?""")

    fun dateTimeFromFileName(fileName: String): String? {
        val match = blackboxDateTimePattern.find(fileName) ?: return null
        val year = match.groupValues[1].toIntOrNull() ?: return null
        val month = match.groupValues[2].toIntOrNull() ?: return null
        val day = match.groupValues[3].toIntOrNull() ?: return null
        val hour = match.groupValues[4].toIntOrNull() ?: return null
        val minute = match.groupValues[5].toIntOrNull() ?: return null
        val validDate = runCatching { LocalDate.of(year, month, day) }.isSuccess
        if (!validDate || hour !in 0..23 || minute !in 0..59) return null
        return "%04d.%02d.%02d %02d:%02d".format(year, month, day, hour, minute)
    }
}
