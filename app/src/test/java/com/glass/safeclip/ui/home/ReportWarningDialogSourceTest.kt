package com.glass.safeclip.ui.home

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportWarningDialogSourceTest {
    @Test
    fun dialogCannotBeDismissedByOutsideTapOrBackButton() {
        val source = dialogSource()

        assertTrue(
            "outside taps must not dismiss the warning",
            source.contains("dismissOnClickOutside = false")
        )
        assertTrue(
            "the back button must not dismiss the warning",
            source.contains("dismissOnBackPress = false")
        )
    }

    @Test
    fun noticeBodyScrollsInsideABoundedArea() {
        val source = dialogSource()

        assertTrue("the notice body must be scrollable", source.contains("verticalScroll("))
        assertTrue("the notice body must have a maximum height", source.contains("heightIn("))
    }

    @Test
    fun theWholeDoNotShowAgainRowTogglesTheCheckbox() {
        val source = dialogSource()

        assertTrue("the checkbox row must be clickable", source.contains("role = Role.Checkbox"))
        assertTrue(
            "the checkbox must delegate clicks to the full row",
            source.contains("onCheckedChange = null")
        )
    }

    @Test
    fun checkboxAndConfirmButtonShareTheFixedBottomRow() {
        val source = dialogSource()

        assertTrue(
            "the bottom actions must be kept in a dedicated row",
            source.contains("REPORT_WARNING_ACTION_ROW_TAG")
        )
    }

    private fun dialogSource(): String =
        File("src/main/java/com/glass/safeclip/ui/home/ReportWarningDialog.kt")
            .takeIf(File::isFile)
            ?.readText()
            .orEmpty()
}
