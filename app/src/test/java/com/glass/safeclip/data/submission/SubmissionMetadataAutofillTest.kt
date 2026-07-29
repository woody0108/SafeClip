package com.glass.safeclip.data.submission

import com.glass.safeclip.ui.submission.SubmissionDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmissionMetadataAutofillTest {
    @Test
    fun fillsBlankDateTimeAndLocationFromRepresentativeMetadata() {
        val metadata = SubmissionFileMetadata(
            incidentDateTimeText = "2026.07.29 14:30",
            locationText = "서울특별시 강남구 테헤란로 인근"
        )

        val result = SubmissionMetadataAutofill.apply(SubmissionDraft(), metadata)

        assertTrue(result.filledAny)
        assertEquals("2026.07.29 14:30", result.draft.incidentDateTime)
        assertEquals("서울특별시 강남구 테헤란로 인근", result.draft.locationText)
    }

    @Test
    fun keepsUserEnteredDateTimeAndLocation() {
        val draft = SubmissionDraft(
            incidentDateTime = "직접 입력한 일시",
            locationText = "직접 입력한 위치"
        )
        val metadata = SubmissionFileMetadata(
            incidentDateTimeText = "2026.07.29 14:30",
            locationText = "서울특별시 강남구 테헤란로 인근"
        )

        val result = SubmissionMetadataAutofill.apply(draft, metadata)

        assertFalse(result.filledAny)
        assertEquals("직접 입력한 일시", result.draft.incidentDateTime)
        assertEquals("직접 입력한 위치", result.draft.locationText)
    }

    @Test
    fun replacesPreviousAutofilledValuesWhenRepresentativeChanges() {
        val draft = SubmissionDraft(
            incidentDateTime = "2026.07.29 14:30",
            locationText = "서울특별시 강남구 테헤란로 인근"
        )
        val metadata = SubmissionFileMetadata(
            incidentDateTimeText = "2026.07.29 15:10",
            locationText = "서울특별시 서초구 반포대로 인근"
        )

        val result = SubmissionMetadataAutofill.apply(
            draft = draft,
            metadata = metadata,
            replaceDateTime = true,
            replaceLocation = true
        )

        assertTrue(result.filledAny)
        assertEquals("2026.07.29 15:10", result.draft.incidentDateTime)
        assertEquals("서울특별시 서초구 반포대로 인근", result.draft.locationText)
    }

    @Test
    fun fillsOnlyMissingField() {
        val draft = SubmissionDraft(incidentDateTime = "직접 입력한 일시")
        val metadata = SubmissionFileMetadata(
            incidentDateTimeText = "2026.07.29 14:30",
            locationText = "서울특별시 강남구 테헤란로 인근"
        )

        val result = SubmissionMetadataAutofill.apply(draft, metadata)

        assertTrue(result.filledAny)
        assertEquals("직접 입력한 일시", result.draft.incidentDateTime)
        assertEquals("서울특별시 강남구 테헤란로 인근", result.draft.locationText)
    }
}
