package com.glass.safeclip.app

enum class SavedFolderRestorePlan {
    Restore,
    PermissionLost,
    NoSavedFolder
}

object SavedFolderRestorePlanner {
    fun plan(
        savedUriString: String?,
        permissionSnapshot: FolderPermissionSnapshot
    ): SavedFolderRestorePlan {
        return when {
            savedUriString.isNullOrBlank() -> SavedFolderRestorePlan.NoSavedFolder
            permissionSnapshot.canRestore(savedUriString) -> SavedFolderRestorePlan.Restore
            else -> SavedFolderRestorePlan.PermissionLost
        }
    }
}
