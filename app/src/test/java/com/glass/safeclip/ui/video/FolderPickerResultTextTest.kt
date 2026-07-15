package com.glass.safeclip.ui.video

import org.junit.Assert.assertEquals
import org.junit.Test

class FolderPickerResultTextTest {
    @Test
    fun messageForVideoCountExplainsEmptyAndNonEmptyResults() {
        assertEquals("선택한 폴더에서 지원 영상 파일을 찾지 못했습니다.", FolderPickerResultText.messageForVideoCount(0))
        assertEquals("영상 후보 3개를 찾았습니다.", FolderPickerResultText.messageForVideoCount(3))
    }
}
