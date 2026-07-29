package com.glass.safeclip.ui.submission

import com.glass.safeclip.data.submission.SubmissionAttachment

data class SubmissionAttachmentPreviewSelection(
    val selected: SubmissionAttachment,
    val title: String
) {
    fun select(attachment: SubmissionAttachment): SubmissionAttachmentPreviewSelection {
        return copy(selected = attachment, title = "미리보기")
    }

    fun selectCandidate(attachment: SubmissionAttachment): SubmissionAttachmentPreviewSelection {
        return copy(selected = attachment, title = "추가 전 미리보기")
    }

    companion object {
        fun initial(representative: SubmissionAttachment): SubmissionAttachmentPreviewSelection {
            return SubmissionAttachmentPreviewSelection(
                selected = representative,
                title = "미리보기"
            )
        }
    }
}
