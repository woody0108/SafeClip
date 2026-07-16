package com.glass.safeclip.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderPermissionSnapshotTest {
    @Test
    fun `allows restore only when saved uri has read and write permissions`() {
        val snapshot = FolderPermissionSnapshot(
            persistedReadUriStrings = setOf("content://safeclip/tree/current"),
            persistedWriteUriStrings = setOf("content://safeclip/tree/current")
        )

        assertTrue(snapshot.canRestore("content://safeclip/tree/current"))
    }

    @Test
    fun `denies restore when uri is missing or write permission is missing`() {
        val snapshot = FolderPermissionSnapshot(
            persistedReadUriStrings = setOf("content://safeclip/tree/current"),
            persistedWriteUriStrings = emptySet()
        )

        assertFalse(snapshot.canRestore(null))
        assertFalse(snapshot.canRestore("content://safeclip/tree/current"))
    }
}
