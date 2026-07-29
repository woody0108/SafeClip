package com.glass.safeclip.ui.submission

data class SubmissionAttachmentRowActions(
    val representativeButtonSelected: Boolean,
    val showRemoveButton: Boolean
) {
    companion object {
        fun from(representative: Boolean): SubmissionAttachmentRowActions {
            return SubmissionAttachmentRowActions(
                representativeButtonSelected = representative,
                showRemoveButton = !representative
            )
        }
    }
}
