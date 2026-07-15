package com.glass.safeclip.ui.icon

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeClipLauncherIconResourceTest {
    @Test
    fun adaptiveIconsUseSafeClipForeground() {
        val resRoot = resourceRoot()
        val launcher = resRoot.resolve("mipmap-anydpi-v26/ic_launcher.xml").readText()
        val roundLauncher = resRoot.resolve("mipmap-anydpi-v26/ic_launcher_round.xml").readText()

        assertTrue(launcher.contains("@drawable/ic_safeclip_launcher_foreground"))
        assertTrue(roundLauncher.contains("@drawable/ic_safeclip_launcher_foreground"))
    }

    @Test
    fun safeClipForegroundContainsShieldLensAndRecordDot() {
        val foreground = resourceRoot().resolve("drawable/ic_safeclip_launcher_foreground.xml")

        assertTrue(Files.exists(foreground))

        val xml = foreground.readText()
        assertTrue(xml.contains("#FF5A1F"))
        assertTrue(xml.contains("M54,24"))
        assertTrue(xml.contains("android:strokeColor=\"#68C8FF\""))
    }

    @Test
    fun safeClipForegroundKeepsOuterBracketsInsideMaskSafeArea() {
        val foreground = resourceRoot().resolve("drawable/ic_safeclip_launcher_foreground.xml")
        val xml = foreground.readText()

        assertTrue(xml.contains("M25,25"))
        assertTrue(xml.contains("M83,25"))
        assertTrue(xml.contains("M25,83"))
        assertTrue(xml.contains("M83,83"))
    }

    private fun resourceRoot(): Path {
        val moduleRoot = Path.of("src/main/res")
        return if (moduleRoot.exists()) moduleRoot else Path.of("app/src/main/res")
    }
}
