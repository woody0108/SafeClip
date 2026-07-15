package com.glass.safeclip.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeTileTextStyleTest {
    @Test
    fun folderPermissionAndShortcutTilesShareTextSizes() {
        assertEquals(18, HomeTileTextStyle.TITLE_FONT_SIZE_SP)
        assertEquals(13, HomeTileTextStyle.SUBTITLE_FONT_SIZE_SP)
    }
}
