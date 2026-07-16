package com.glass.safeclip.ui.folder

import org.junit.Assert.assertEquals
import org.junit.Test

class FolderManagerTextTest {
    @Test
    fun titlesDescribeManagedFolderKind() {
        assertEquals("SafeClip 저장함", FolderManagerText.title(FolderViewKind.SafeClipSaved))
        assertEquals("현재 폴더", FolderManagerText.title(FolderViewKind.CurrentFolder))
    }

    @Test
    fun emptyMessagesDescribeManagedFolderKind() {
        assertEquals("저장된 캡쳐나 클립이 없습니다.", FolderManagerText.emptyMessage(FolderViewKind.SafeClipSaved))
        assertEquals("현재 폴더에 표시할 영상이 없습니다.", FolderManagerText.emptyMessage(FolderViewKind.CurrentFolder))
    }

    @Test
    fun loadingMessageExplainsBackgroundRefresh() {
        assertEquals("파일 목록을 새로고침 중입니다.", FolderManagerText.loadingMessage())
    }
}
