package com.glass.safeclip.ui.submission

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
}
