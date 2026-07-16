package com.glass.safeclip.app

import com.glass.safeclip.data.file.SavedFolderAccess

data class FolderPermissionSnapshot(
    val persistedReadUriStrings: Set<String>,
    val persistedWriteUriStrings: Set<String>
) {
    fun canRestore(savedUriString: String?): Boolean {
        return SavedFolderAccess.canRestore(
            savedUriString = savedUriString,
            persistedReadUriStrings = persistedReadUriStrings,
            persistedWriteUriStrings = persistedWriteUriStrings
        )
    }
}
