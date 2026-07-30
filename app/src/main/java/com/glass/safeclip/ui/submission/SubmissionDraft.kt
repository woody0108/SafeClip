package com.glass.safeclip.ui.submission

import com.glass.safeclip.data.submission.SubmissionAttachment
import com.glass.safeclip.data.submission.SubmissionAttachmentRules

data class SubmissionDraft(
    val incidentDateTime: String = "",
    val locationText: String = "",
    val incidentType: String = "",
    val memo: String = "",
    val reviewConsent: Boolean = false,
    val storageConsent: Boolean = false,
    val dataUseConsent: Boolean = false
) {
    val isReadyToSubmit: Boolean
        get() = hasRequiredIncidentInfo &&
            reviewConsent &&
            storageConsent &&
            dataUseConsent

    val hasRequiredIncidentInfo: Boolean
        get() = incidentDateTime.isNotBlank() &&
            locationText.isNotBlank() &&
            incidentType.isNotBlank() &&
            memo.isNotBlank()

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
