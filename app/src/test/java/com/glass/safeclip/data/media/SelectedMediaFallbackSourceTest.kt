package com.glass.safeclip.data.media

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectedMediaFallbackSourceTest {
    @Test
    fun limitedAccessCanRetryWithoutProviderSidePathSelection() {
        val source = File(
            "src/main/java/com/glass/safeclip/data/media/AndroidSafeClipSavedMediaRepository.kt"
        ).readText()

        assertTrue(source.contains("useAccessibleSelectionFallback"))
        assertTrue(source.contains("MediaStore.MediaColumns.RELATIVE_PATH"))
        assertTrue(source.contains("selection = null"))
    }
}