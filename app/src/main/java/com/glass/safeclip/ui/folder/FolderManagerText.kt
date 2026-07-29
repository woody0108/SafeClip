package com.glass.safeclip.ui.folder

object FolderManagerText {
    fun title(kind: FolderViewKind): String {
        return when (kind) {
            FolderViewKind.SafeClipSaved -> "SafeClip 폴더"
            FolderViewKind.CurrentFolder -> "블랙박스 폴더"
        }
    }

    fun emptyMessage(kind: FolderViewKind): String {
        return when (kind) {
            FolderViewKind.SafeClipSaved -> "SafeClip 폴더에 저장된 캡쳐나 클립이 없습니다."
            FolderViewKind.CurrentFolder -> "블랙박스 폴더에 표시할 파일이 없습니다."
        }
    }

    fun loadingMessage(): String {
        return "파일 목록을 새로고침 중입니다."
    }
}
