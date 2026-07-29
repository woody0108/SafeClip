package com.glass.safeclip.ui.submission

import org.junit.Assert.assertEquals
import org.junit.Test

class SubmissionIncidentDateTimeFieldsTest {
    @Test
    fun dateInputKeepsOnlyDateCharacters() {
        assertEquals(
            "2026.07.29",
            SubmissionIncidentDateTimeFields.sanitizeDate("2026년.07월.29일 abc")
        )
    }

    @Test
    fun timeInputKeepsOnlyTimeCharacters() {
        assertEquals(
            "15:30",
            SubmissionIncidentDateTimeFields.sanitizeTime("오후 15:30분 abc")
        )
    }

    @Test
    fun combinesDateAndTimeForExistingSubmissionField() {
        val fields = SubmissionIncidentDateTimeFields(
            date = "2026.07.29",
            time = "15:30"
        )

        assertEquals("2026.07.29 15:30", fields.combined())
    }

    @Test
    fun splitsExistingCombinedDateTime() {
        val fields = SubmissionIncidentDateTimeFields.fromCombined("2026.07.29 15:30")

        assertEquals("2026.07.29", fields.date)
        assertEquals("15:30", fields.time)
    }
}
