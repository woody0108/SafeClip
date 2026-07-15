package com.glass.safeclip.data.file

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedFolderAccessTest {
    @Test
    fun canRestoreFolderOnlyWhenSavedUriHasPersistedReadAndWritePermission() {
        val savedUri = "content://safeclip/tree/blackbox"
        val persistedReadUris = setOf("content://safeclip/tree/blackbox")
        val persistedWriteUris = setOf("content://safeclip/tree/blackbox")

        assertTrue(SavedFolderAccess.canRestore(savedUri, persistedReadUris, persistedWriteUris))
    }

    @Test
    fun cannotRestoreFolderWhenSavedUriIsMissing() {
        val persistedReadUris = setOf("content://safeclip/tree/blackbox")
        val persistedWriteUris = setOf("content://safeclip/tree/blackbox")

        assertFalse(SavedFolderAccess.canRestore(null, persistedReadUris, persistedWriteUris))
        assertFalse(SavedFolderAccess.canRestore("", persistedReadUris, persistedWriteUris))
    }

    @Test
    fun cannotRestoreFolderWhenSavedUriPermissionWasRevoked() {
        val savedUri = "content://safeclip/tree/blackbox"
        val persistedReadUris = setOf("content://safeclip/tree/other")
        val persistedWriteUris = setOf("content://safeclip/tree/blackbox")

        assertFalse(SavedFolderAccess.canRestore(savedUri, persistedReadUris, persistedWriteUris))
    }

    @Test
    fun cannotRestoreFolderWhenSavedUriHasOnlyReadPermission() {
        val savedUri = "content://safeclip/tree/blackbox"
        val persistedReadUris = setOf("content://safeclip/tree/blackbox")
        val persistedWriteUris = emptySet<String>()

        assertFalse(SavedFolderAccess.canRestore(savedUri, persistedReadUris, persistedWriteUris))
    }
}
