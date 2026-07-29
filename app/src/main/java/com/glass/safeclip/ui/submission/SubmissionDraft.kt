package com.glass.safeclip.ui.submission

import com.glass.safeclip.data.submission.SubmissionAttachment

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
        get() = incidentDateTime.isNotBlank() &&
            locationText.isNotBlank() &&
            incidentType.isNotBlank() &&
            memo.isNotBlank() &&
            reviewConsent &&
            storageConsent &&
            dataUseConsent

    fun canSubmitWith(attachments: List<SubmissionAttachment>): Boolean {
        return isReadyToSubmit && attachments.isNotEmpty()
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
