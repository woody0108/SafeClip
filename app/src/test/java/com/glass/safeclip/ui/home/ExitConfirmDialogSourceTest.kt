package com.glass.safeclip.ui.home

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ExitConfirmDialogSourceTest {
    @Test
    fun secondBackPressConfirmsExitWithoutTreatingOutsideTapAsExit() {
        val source = dialogSource()

        assertTrue(
            "back dismissal must invoke the exit callback",
            source.contains("onDismissRequest = onConfirm")
        )
        assertTrue(
            "outside taps must not exit the app",
            source.contains("dismissOnClickOutside = false")
        )
    }

    @Test
    fun bothButtonsHaveExtraTopSpacing() {
        val source = dialogSource()

        assertTrue(
            "both button slots must use the shared lowered button modifier",
            source.count { it == '\n' } > 0 &&
                source.contains("modifier = loweredButtonModifier") &&
                source.contains("val loweredButtonModifier = Modifier.padding(top = 8.dp)")
        )
    }

    private fun dialogSource(): String =
        File("src/main/java/com/glass/safeclip/ui/home/ExitConfirmDialog.kt")
            .takeIf(File::isFile)
            ?.readText()
            .orEmpty()
}
