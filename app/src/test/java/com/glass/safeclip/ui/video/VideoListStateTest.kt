package com.glass.safeclip.ui.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoListStateTest {
    @Test
    fun fileSizeLabelFormatsUnknownKbAndMb() {
        assertEquals("크기 정보 없음", VideoListText.fileSizeLabel(null))
        assertEquals("512 KB", VideoListText.fileSizeLabel(512 * 1024))
        assertEquals("2.0 MB", VideoListText.fileSizeLabel(2 * 1024 * 1024))
    }
}
