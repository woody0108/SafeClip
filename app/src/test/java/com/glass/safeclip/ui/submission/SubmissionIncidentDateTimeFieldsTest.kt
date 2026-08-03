package com.glass.safeclip.ui.submission

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

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
    fun dateInputStopsAfterValidDateLength() {
        assertEquals(
            "2026.08.09",
            SubmissionIncidentDateTimeFields.sanitizeDate("2026.08.0999")
        )
    }

    @Test
    fun dateInputRejectsImpossibleDay() {
        assertEquals(
            "2026.08.3",
            SubmissionIncidentDateTimeFields.sanitizeDate("2026.08.35")
        )
    }

    @Test
    fun dateInputKeepsFutureDateForWarning() {
        assertEquals(
            "2026.08.04",
            SubmissionIncidentDateTimeFields.sanitizeDate("2026.08.04")
        )
    }

    @Test
    fun dateInputKeepsOldDateForWarning() {
        assertEquals(
            "2026.07.23",
            SubmissionIncidentDateTimeFields.sanitizeDate("2026.07.23")
        )
    }

    @Test
    fun dateWarningShowsForFutureDate() {
        assertEquals(
            "날짜를 다시 확인해주세요",
            SubmissionIncidentDateTimeFields.warningMessage(
                date = "2026.08.04",
                today = LocalDate.of(2026, 8, 3)
            )
        )
    }

    @Test
    fun dateWarningShowsForDateOlderThanThreeDays() {
        assertEquals(
            "날짜를 다시 확인해주세요",
            SubmissionIncidentDateTimeFields.warningMessage(
                date = "2026.07.30",
                today = LocalDate.of(2026, 8, 3)
            )
        )
    }

    @Test
    fun dateWarningIsEmptyForRecentDate() {
        assertEquals(
            null,
            SubmissionIncidentDateTimeFields.warningMessage(
                date = "2026.08.01",
                today = LocalDate.of(2026, 8, 3)
            )
        )
    }

    @Test
    fun datePickerSelectableStartDateIsTenDaysBeforeToday() {
        assertEquals(
            LocalDate.of(2026, 7, 24),
            SubmissionIncidentDateTimeFields.selectableStartDate(LocalDate.of(2026, 8, 3))
        )
    }

    @Test
    fun datePickerSelectableEndDateIsToday() {
        assertEquals(
            LocalDate.of(2026, 8, 3),
            SubmissionIncidentDateTimeFields.selectableEndDate(LocalDate.of(2026, 8, 3))
        )
    }

    @Test
    fun timeInputStopsAfterValidTimeLength() {
        assertEquals(
            "22:00",
            SubmissionIncidentDateTimeFields.sanitizeTime("22:0099")
        )
    }

    @Test
    fun timeInputRejectsImpossibleHour() {
        assertEquals(
            "2",
            SubmissionIncidentDateTimeFields.sanitizeTime("25:00")
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
