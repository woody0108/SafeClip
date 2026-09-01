package com.glass.safeclip.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.glass.safeclip.ui.theme.SafeClipTheme
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReportWarningDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tappingTheLabelTogglesDoNotShowAgain() {
        showDialog()

        composeRule.onNodeWithTag(REPORT_WARNING_CHECK_ROW_TAG).assertIsOff()
        composeRule.onNodeWithText("다시 표시 안함").performClick()
        composeRule.onNodeWithTag(REPORT_WARNING_CHECK_ROW_TAG).assertIsOn()
    }

    @Test
    fun noticeBodyIsScrollableWhileBottomActionsStayVisible() {
        showDialog()

        composeRule.onNodeWithTag(REPORT_WARNING_BODY_TAG).assert(hasScrollAction())
        composeRule.onNodeWithText("다시 표시 안함").assertIsDisplayed()
        composeRule.onNodeWithText("확인").assertIsDisplayed()
    }

    @Test
    fun checkboxAndConfirmButtonAreOnTheSameBottomLine() {
        showDialog()

        val checkboxBounds = composeRule
            .onNodeWithTag(REPORT_WARNING_CHECK_ROW_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
        val confirmBounds = composeRule
            .onNodeWithText("확인")
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(
            "checkbox and confirm button must share the same action line",
            abs(checkboxBounds.center.y - confirmBounds.center.y) <= 8f
        )
    }

    private fun showDialog() {
        composeRule.setContent {
            SafeClipTheme {
                var checked by remember { mutableStateOf(false) }
                ReportWarningDialog(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    onConfirm = {}
                )
            }
        }
    }
}
