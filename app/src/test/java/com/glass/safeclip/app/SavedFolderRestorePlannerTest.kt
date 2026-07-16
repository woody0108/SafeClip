package com.glass.safeclip.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedFolderRestorePlannerTest {
    @Test
    fun `plans restore when saved folder has persisted read and write permissions`() {
        val snapshot = FolderPermissionSnapshot(
            persistedReadUriStrings = setOf("content://safeclip/tree/current"),
            persistedWriteUriStrings = setOf("content://safeclip/tree/current")
        )

        val plan = SavedFolderRestorePlanner.plan(
            savedUriString = "content://safeclip/tree/current",
            permissionSnapshot = snapshot
        )

        assertEquals(SavedFolderRestorePlan.Restore, plan)
    }

    @Test
    fun `plans no saved folder when uri is empty`() {
        val snapshot = FolderPermissionSnapshot(
            persistedReadUriStrings = setOf("content://safeclip/tree/current"),
            persistedWriteUriStrings = setOf("content://safeclip/tree/current")
        )

        val plan = SavedFolderRestorePlanner.plan(
            savedUriString = null,
            permissionSnapshot = snapshot
        )

        assertEquals(SavedFolderRestorePlan.NoSavedFolder, plan)
    }

    @Test
    fun `plans permission lost when saved folder exists but permission is missing`() {
        val snapshot = FolderPermissionSnapshot(
            persistedReadUriStrings = setOf("content://safeclip/tree/current"),
            persistedWriteUriStrings = emptySet()
        )

        val plan = SavedFolderRestorePlanner.plan(
            savedUriString = "content://safeclip/tree/current",
            permissionSnapshot = snapshot
        )

        assertEquals(SavedFolderRestorePlan.PermissionLost, plan)
    }
}
