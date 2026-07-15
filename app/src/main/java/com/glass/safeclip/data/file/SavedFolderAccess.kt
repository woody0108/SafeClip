package com.glass.safeclip.data.file

object SavedFolderAccess {
    fun canRestore(
        savedUriString: String?,
        persistedReadUriStrings: Set<String>,
        persistedWriteUriStrings: Set<String>
    ): Boolean {
        return !savedUriString.isNullOrBlank() &&
            savedUriString in persistedReadUriStrings &&
            savedUriString in persistedWriteUriStrings
    }
}
