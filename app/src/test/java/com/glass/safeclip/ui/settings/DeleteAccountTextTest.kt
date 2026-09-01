package com.glass.safeclip.ui.settings

import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteAccountTextTest {
    @Test
    fun warningMentionsSubmissionDeletion() {
        assertTrue(DeleteAccountText.warning.contains("제출내역"))
        assertTrue(DeleteAccountText.warning.contains("삭제"))
    }
}
