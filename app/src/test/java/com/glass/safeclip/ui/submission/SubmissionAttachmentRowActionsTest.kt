package com.glass.safeclip.ui.submission

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmissionAttachmentRowActionsTest {
    @Test
    fun representativeAttachmentShowsOnlySelectedRepresentativeButton() {
        val actions = SubmissionAttachmentRowActions.from(representative = true)

        assertTrue(actions.representativeButtonSelected)
        assertFalse(actions.showRemoveButton)
    }

    @Test
    fun nonRepresentativeAttachmentShowsDefaultRepresentativeAndRemoveButtons() {
        val actions = SubmissionAttachmentRowActions.from(representative = false)

        assertFalse(actions.representativeButtonSelected)
        assertTrue(actions.showRemoveButton)
    }
}
