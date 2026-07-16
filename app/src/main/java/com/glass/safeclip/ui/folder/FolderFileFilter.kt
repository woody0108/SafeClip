package com.glass.safeclip.ui.folder

import com.glass.safeclip.data.file.ManagedFolderFile
import com.glass.safeclip.data.file.ManagedFolderVideoCandidate

enum class FolderFileFilter(val label: String) {
    All("전체"),
    Video("영상"),
    Photo("사진");

    fun apply(files: List<ManagedFolderFile>): List<ManagedFolderFile> {
        return when (this) {
            All -> files
            Video -> files.filter { ManagedFolderVideoCandidate.canUseVideoActions(it) }
            Photo -> files.filter { ManagedFolderVideoCandidate.canPreviewImage(it) }
        }
    }
}
