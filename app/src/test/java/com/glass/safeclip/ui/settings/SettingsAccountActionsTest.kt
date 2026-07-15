package com.glass.safeclip.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsAccountActionsTest {
    @Test
    fun guestAccountCannotSignOut() {
        val actions = SettingsAccountActions.from(
            linkedEmail = null,
            linkedDisplayName = null,
            linkedProvider = null
        )

        assertFalse(actions.signOutEnabled)
    }

    @Test
    fun linkedAccountCanSignOut() {
        val actions = SettingsAccountActions.from(
            linkedEmail = "safeclip@example.com",
            linkedDisplayName = null,
            linkedProvider = "google"
        )

        assertTrue(actions.signOutEnabled)
    }
}
