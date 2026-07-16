package com.glass.safeclip.data.file

import org.junit.Assert.assertEquals
import org.junit.Test

class ManagedFolderFileDisplayTest {
    @Test
    fun `uses jpeg mime type for jpg and jpeg names`() {
        assertEquals("image/jpeg", ManagedFolderFileDisplay.mimeTypeForDisplayName("capture.jpg"))
        assertEquals("image/jpeg", ManagedFolderFileDisplay.mimeTypeForDisplayName("capture.JPEG"))
    }

    @Test
    fun `uses video mime type as fallback`() {
        assertEquals("video/mp4", ManagedFolderFileDisplay.mimeTypeForDisplayName("event.mp4"))
    }

    @Test
    fun `uses photo action text only for jpeg files`() {
        assertEquals(
            "사진 확인하기",
            ManagedFolderFileDisplay.submittedFileActionText("capture.jpg")
        )
        assertEquals(
            "영상 확인하기",
            ManagedFolderFileDisplay.submittedFileActionText("event.mp4")
        )
    }
}
