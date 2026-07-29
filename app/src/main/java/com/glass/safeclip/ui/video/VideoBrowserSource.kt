package com.glass.safeclip.ui.video

import com.glass.safeclip.data.file.ManagedFolderFile

enum class VideoBrowserSource(val label: String) {
    All("전체"),
    Blackbox("블랙박스"),
    SafeClip("SafeClip");

    fun select(
        blackboxFiles: List<ManagedFolderFile>,
        safeClipFiles: List<ManagedFolderFile>
    ): List<ManagedFolderFile> {
        return when (this) {
            All -> blackboxFiles + safeClipFiles
            Blackbox -> blackboxFiles
            SafeClip -> safeClipFiles
        }
    }
}
