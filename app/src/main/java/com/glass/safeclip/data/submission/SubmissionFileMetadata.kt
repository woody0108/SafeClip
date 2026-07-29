package com.glass.safeclip.data.submission

import com.glass.safeclip.ui.submission.SubmissionDraft

data class SubmissionFileMetadata(
    val incidentDateTimeText: String? = null,
    val locationText: String? = null
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
        val nextDraft = draft.copy(
            incidentDateTime = nextDateTime,
            locationText = nextLocation
        )

        return SubmissionMetadataAutofillResult(
            draft = nextDraft,
            filledDateTime = nextDraft.incidentDateTime != draft.incidentDateTime,
            filledLocation = nextDraft.locationText != draft.locationText
        )
    }
}
