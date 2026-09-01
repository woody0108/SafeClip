package com.glass.safeclip.ui.theme

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class TypographySourceTest {
    @Test
    fun bundledNotoSansKrFontsExistForEveryUsedWeight() {
        val fonts = listOf(
            "noto_sans_kr_regular.otf",
            "noto_sans_kr_medium.otf",
            "noto_sans_kr_bold.otf"
        ).map { name -> File("src/main/res/font/$name") }

        fonts.forEach { font ->
            assertTrue(font.isFile)
            assertTrue(font.length() > 100_000L)
        }
    }

    @Test
    fun everyMaterialTypographyStyleUsesSafeClipFont() {
        val source = File("src/main/java/com/glass/safeclip/ui/theme/Type.kt").readText()
        val styles = listOf(
            "displayLarge",
            "displayMedium",
            "displaySmall",
            "headlineLarge",
            "headlineMedium",
            "headlineSmall",
            "titleLarge",
            "titleMedium",
            "titleSmall",
            "bodyLarge",
            "bodyMedium",
            "bodySmall",
            "labelLarge",
            "labelMedium",
            "labelSmall"
        )

        assertTrue(source.contains("Font(R.font.noto_sans_kr_regular, FontWeight.Normal)"))
        assertTrue(source.contains("Font(R.font.noto_sans_kr_medium, FontWeight.Medium)"))
        assertTrue(source.contains("Font(R.font.noto_sans_kr_medium, FontWeight.SemiBold)"))
        assertTrue(source.contains("Font(R.font.noto_sans_kr_bold, FontWeight.Bold)"))
        styles.forEach { style ->
            assertTrue(source.contains("$style = BaseTypography.$style.withSafeClipFont()"))
        }
    }
}
