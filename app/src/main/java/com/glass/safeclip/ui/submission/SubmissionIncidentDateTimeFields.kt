package com.glass.safeclip.ui.submission

data class SubmissionIncidentDateTimeFields(
    val date: String = "",
    val time: String = ""
) {
    fun combined(): String {
        return listOf(date, time)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    companion object {
        fun fromCombined(value: String): SubmissionIncidentDateTimeFields {
            val parts = value.trim().split(Regex("\\s+"), limit = 2)
            return SubmissionIncidentDateTimeFields(
                date = parts.getOrNull(0).orEmpty(),
                time = parts.getOrNull(1).orEmpty()
            )
        }

        fun sanitizeDate(value: String): String {
            return value.filter { character ->
                character.isDigit() || character == '.' || character == '-' || character == '/'
            }
        }

        fun sanitizeTime(value: String): String {
            return value.filter { character ->
                character.isDigit() || character == ':'
            }
        }

        fun formatDate(year: Int, month: Int, day: Int): String {
            return "%04d.%02d.%02d".format(year, month, day)
        }

        fun formatTime(hour: Int, minute: Int): String {
            return "%02d:%02d".format(hour, minute)
        }
    }
}
