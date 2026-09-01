package com.glass.safeclip.data.media

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeClipMediaSaveLocationTest {
    @Test
    fun allGeneratedMediaUsesDcimSafeClip() {
        assertEquals("SafeClip", SafeClipMediaSaveLocation.albumName)
        assertEquals("DCIM/SafeClip", SafeClipMediaSaveLocation.relativePath)
        assertEquals(
            "DCIM/SafeClip/front_clip.mp4",
            SafeClipMediaSaveLocation.displayPath("front_clip.mp4")
        )
    }

    @Test
    fun publicDirectoryIsAlwaysSafeClipUnderDcim() {
        assertEquals(
            File("DCIM-root", "SafeClip"),
            SafeClipMediaSaveLocation.publicDirectory(File("DCIM-root"))
        )
    }

    @Test
    fun ensurePublicDirectoryCreatesAndReusesSafeClipFolder() {
        val dcimDirectory = Files.createTempDirectory("safeclip-dcim").toFile()

        assertTrue(SafeClipMediaSaveLocation.ensurePublicDirectory(dcimDirectory))
        assertTrue(File(dcimDirectory, "SafeClip").isDirectory)
        assertTrue(SafeClipMediaSaveLocation.ensurePublicDirectory(dcimDirectory))

        dcimDirectory.deleteRecursively()
    }
}
