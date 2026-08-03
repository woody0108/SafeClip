package com.glass.safeclip.ui.submission

import java.time.LocalDate

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
            val digits = value.filter(Char::isDigit).take(8)
            val year = digits.take(4)
            val month = digits.drop(4).take(2).validatedMonth()
            val day = if (year.length == 4 && month.length == 2) {
                digits.drop(6).take(2).validatedDay(year, month)
            } else {
                ""
            }
            return listOf(year, month, day)
                .filter { it.isNotBlank() }
                .joinToString(".")
        }

        fun warningMessage(date: String, today: LocalDate = LocalDate.now()): String? {
            val parsedDate = parseCompleteDate(date) ?: return null
            return if (parsedDate.isAfter(today) || parsedDate.isBefore(today.minusDays(3))) {
                "날짜를 다시 확인해주세요"
            } else {
                null
            }
        }

        fun selectableStartDate(today: LocalDate = LocalDate.now()): LocalDate {
            return today.minusDays(10)
        }

        fun selectableEndDate(today: LocalDate = LocalDate.now()): LocalDate {
            return today
        }

        fun sanitizeTime(value: String): String {
            val digits = value.filter(Char::isDigit).take(4)
            val hour = digits.take(2).validatedHour()
            val minute = if (hour.length == 2) {
                digits.drop(2).take(2).validatedMinute()
            } else {
                ""
            }
            return listOf(hour, minute)
                .filter { it.isNotBlank() }
                .joinToString(":")
        }

        fun formatDate(year: Int, month: Int, day: Int): String {
            return "%04d.%02d.%02d".format(year, month, day)
        }

        fun formatTime(hour: Int, minute: Int): String {
            return "%02d:%02d".format(hour, minute)
        }

        private fun String.validatedMonth(): String {
            if (length < 2) return this
            val value = toIntOrNull() ?: return dropLast(1)
            return if (value in 1..12) this else dropLast(1)
        }

        private fun String.validatedDay(yearText: String, monthText: String): String {
            if (length < 2) return this
            val day = toIntOrNull() ?: return dropLast(1)
            if (day !in 1..31) return dropLast(1)
            if (yearText.length == 4 && monthText.length == 2) {
                val year = yearText.toIntOrNull() ?: return this
                val month = monthText.toIntOrNull() ?: return this
                runCatching { LocalDate.of(year, month, day) }.getOrNull()
                    ?: return dropLast(1)
            }
            return this
        }

        private fun parseCompleteDate(date: String): LocalDate? {
            val match = Regex("""^(\d{4})\.(\d{2})\.(\d{2})$""").matchEntire(date) ?: return null
            val year = match.groupValues[1].toIntOrNull() ?: return null
            val month = match.groupValues[2].toIntOrNull() ?: return null
            val day = match.groupValues[3].toIntOrNull() ?: return null
            return runCatching { LocalDate.of(year, month, day) }.getOrNull()
        }

        private fun String.validatedHour(): String {
            if (length < 2) return this
            val value = toIntOrNull() ?: return dropLast(1)
            return if (value in 0..23) this else dropLast(1)
        }

        private fun String.validatedMinute(): String {
            if (length < 2) return this
            val value = toIntOrNull() ?: return dropLast(1)
            return if (value in 0..59) this else dropLast(1)
        }
    }
}
