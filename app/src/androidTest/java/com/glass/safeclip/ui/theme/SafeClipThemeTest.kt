package com.glass.safeclip.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SafeClipThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lightSystemModeStillUsesReadableSafeClipColors() {
        var background = Color.Unspecified
        var text = Color.Unspecified

        composeRule.setContent {
            SafeClipTheme(darkTheme = false) {
                background = MaterialTheme.colorScheme.background
                text = MaterialTheme.colorScheme.onBackground
            }
        }

        composeRule.runOnIdle {
            assertEquals(SafeClipBackground, background)
            assertEquals(SafeClipTextPrimary, text)
        }
    }
}
