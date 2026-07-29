package com.glass.safeclip.ui.folder

import org.junit.Assert.assertEquals
import org.junit.Test

class FolderManagerTextTest {
    @Test
    fun titlesDescribeManagedFolderKind() {
        assertEquals("SafeClip 폴더", FolderManagerText.title(FolderViewKind.SafeClipSaved))
        assertEquals("블랙박스 폴더", FolderManagerText.title(FolderViewKind.CurrentFolder))
    }

    @Test
    fun emptyMessagesDescribeManagedFolderKind() {
        assertEquals("SafeClip 폴더에 저장된 캡쳐나 클립이 없습니다.", FolderManagerText.emptyMessage(FolderViewKind.SafeClipSaved))
        assertEquals("블랙박스 폴더에 표시할 파일이 없습니다.", FolderManagerText.emptyMessage(FolderViewKind.CurrentFolder))
    }

    @Test
    fun loadingMessageExplainsBackgroundRefresh() {
        assertEquals("파일 목록을 새로고침 중입니다.", FolderManagerText.loadingMessage())
    }
}
