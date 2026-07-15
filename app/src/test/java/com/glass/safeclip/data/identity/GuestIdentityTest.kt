package com.glass.safeclip.data.identity

import org.junit.Assert.assertEquals
import org.junit.Test

class GuestIdentityTest {
    @Test
    fun createsReadableGuestIdFromSeed() {
        val guestId = GuestIdGenerator.fromSeed("a1b2c3d4e5f67890")

        assertEquals("Guest-A1B2-C3D4", guestId)
    }

    @Test
    fun keepsExistingGuestIdWhenItIsAlreadySaved() {
        val existing = "Guest-ABCD-1234"

        val guestId = GuestIdentity.create(existing) { "Guest-FFFF-0000" }

        assertEquals(existing, guestId)
    }

    @Test
    fun createsNewGuestIdWhenSavedValueIsBlank() {
        val guestId = GuestIdentity.create(" ") { "Guest-FFFF-0000" }

        assertEquals("Guest-FFFF-0000", guestId)
    }
}
