package com.glass.safeclip.app

import com.glass.safeclip.data.submission.SubmissionLookupKey
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmissionLookupSelectorTest {
    @Test
    fun `uses owner uid when signed in`() {
        val key = SubmissionLookupSelector.from(ownerUid = "uid-123", guestId = "Guest-ABCD")

        assertEquals(SubmissionLookupKey.OwnerUid("uid-123"), key)
    }

    @Test
    fun `uses guest id when not signed in`() {
        val key = SubmissionLookupSelector.from(ownerUid = null, guestId = "Guest-ABCD")

        assertEquals(SubmissionLookupKey.GuestId("Guest-ABCD"), key)
    }
}
