package com.glass.safeclip.ui.submission

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmissionDraftTest {
    @Test
    fun draftIsNotReadyWhenRequiredFieldsAreMissing() {
        assertFalse(SubmissionDraft().isReadyToSubmit)
    }

    @Test
    fun draftIsReadyWhenFieldsAndConsentAreComplete() {
        val draft = SubmissionDraft(
            incidentDateTime = "2026.07.14 10:30",
            locationText = "서울 강남구 테헤란로",
            incidentType = "추돌 사고",
            memo = "급정거 후 추돌",
            reviewConsent = true,
            storageConsent = true,
            dataUseConsent = true
        )

        assertTrue(draft.isReadyToSubmit)
    }
}
