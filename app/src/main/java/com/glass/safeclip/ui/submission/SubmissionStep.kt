package com.glass.safeclip.ui.submission

import com.glass.safeclip.data.submission.SubmissionAttachment
import com.glass.safeclip.data.submission.SubmissionAttachmentRules

enum class SubmissionStep {
    Files,
    Details;

    fun canContinue(draft: SubmissionDraft, attachments: List<SubmissionAttachment>): Boolean {
        return when (this) {
            Files -> attachments.isNotEmpty() && SubmissionAttachmentRules.canSubmitAll(attachments)
            Details -> attachments.isNotEmpty() &&
                SubmissionAttachmentRules.canSubmitAll(attachments)
        }
    }
}
