package com.glass.safeclip.ui.submission

import com.glass.safeclip.data.submission.SubmissionAttachment
import com.glass.safeclip.data.submission.SubmissionAttachmentRules

data class SubmissionDraft(
    val incidentDateTime: String = "",
    val locationText: String = "",
    val locationDetail: String = "",
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val locationSource: String = "",
    val incidentType: String = "",
    val memo: String = "",
    val reviewConsent: Boolean = false,
    val storageConsent: Boolean = false,
    val dataUseConsent: Boolean = false
) {
    val isReadyToSubmit: Boolean
        get() = reviewConsent &&
            storageConsent &&
            dataUseConsent

    val hasRequiredIncidentInfo: Boolean
        get() = true

    fun canSubmitWith(attachments: List<SubmissionAttachment>): Boolean {
        return isReadyToSubmit &&
            attachments.isNotEmpty() &&
            SubmissionAttachmentRules.canSubmitAll(attachments)
    }

    companion object {
        fun sanitizeIncidentDateTimeInput(value: String): String {
            return value.filter { character ->
                character.isDigit() ||
                    character == '.' ||
                    character == ':' ||
                    character == '-' ||
                    character == '/' ||
                    character == ' '
            }
        }
    }
}
