package com.glass.safeclip.data.submission

import com.glass.safeclip.ui.submission.SubmissionDraft

data class SubmissionFileMetadata(
    val incidentDateTimeText: String? = null,
    val locationText: String? = null,
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val locationSource: String = ""
)

data class SubmissionMetadataAutofillResult(
    val draft: SubmissionDraft,
    val filledDateTime: Boolean,
    val filledLocation: Boolean
) {
    val filledAny: Boolean
        get() = filledDateTime || filledLocation
}

object SubmissionMetadataAutofill {
    fun apply(
        draft: SubmissionDraft,
        metadata: SubmissionFileMetadata?,
        replaceDateTime: Boolean = false,
        replaceLocation: Boolean = false
    ): SubmissionMetadataAutofillResult {
        if (metadata == null) {
            return SubmissionMetadataAutofillResult(draft, filledDateTime = false, filledLocation = false)
        }

        val shouldFillDateTime = draft.incidentDateTime.isBlank() || replaceDateTime
        val shouldFillLocation = draft.locationText.isBlank() || replaceLocation
        val nextDateTime = if (shouldFillDateTime) {
            metadata.incidentDateTimeText.orEmpty()
        } else {
            draft.incidentDateTime
        }
        val nextLocation = if (shouldFillLocation) {
            metadata.locationText.orEmpty()
        } else {
            draft.locationText
        }
        val nextLatitude = if (shouldFillLocation) {
            metadata.locationLatitude
        } else {
            draft.locationLatitude
        }
        val nextLongitude = if (shouldFillLocation) {
            metadata.locationLongitude
        } else {
            draft.locationLongitude
        }
        val nextSource = if (shouldFillLocation) {
            metadata.locationSource
        } else {
            draft.locationSource
        }
        val nextDraft = draft.copy(
            incidentDateTime = nextDateTime,
            locationText = nextLocation,
            locationLatitude = nextLatitude,
            locationLongitude = nextLongitude,
            locationSource = nextSource
        )

        return SubmissionMetadataAutofillResult(
            draft = nextDraft,
            filledDateTime = nextDraft.incidentDateTime != draft.incidentDateTime,
            filledLocation = nextDraft.locationText != draft.locationText
        )
    }
}
