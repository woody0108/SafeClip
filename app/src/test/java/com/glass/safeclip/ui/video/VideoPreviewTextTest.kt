package com.glass.safeclip.ui.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoPreviewTextTest {
    @Test
    fun captureSuccessMessageIncludesSavedPath() {
        val message = VideoPreviewText.captureSuccessMessage("captures/front.jpg")

        assertEquals("캡쳐 저장 완료: captures/front.jpg", message)
    }

    @Test
    fun captureFailureMessageExplainsFailure() {
        assertEquals("현재 장면을 캡쳐하지 못했습니다.", VideoPreviewText.captureFailureMessage(null))
        assertEquals(
            "현재 장면을 캡쳐하지 못했습니다. 프레임 없음",
            VideoPreviewText.captureFailureMessage("프레임 없음")
        )
    }

    @Test
    fun clipExportMessagesExplainResult() {
        assertEquals("클립 저장 완료: clips/front.mp4", VideoPreviewText.clipExportSuccessMessage("clips/front.mp4"))
        assertEquals("클립을 저장하지 못했습니다.", VideoPreviewText.clipExportFailureMessage(null))
        assertEquals(
            "클립을 저장하지 못했습니다. 구간 오류",
            VideoPreviewText.clipExportFailureMessage("구간 오류")
        )
    }
}
