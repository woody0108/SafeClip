package com.glass.safeclip.data.file

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

class AndroidManagedFileOperator(private val context: Context) {
    fun delete(file: ManagedFolderFile): Boolean {
        val uri = Uri.parse(file.uriString)
        val documentDeleted = DocumentFile.fromSingleUri(context, uri)?.delete() == true
        if (documentDeleted) return true

        return runCatching {
            context.contentResolver.delete(uri, null, null) > 0
        }.getOrDefault(false)
    }

    fun copyToFolder(file: ManagedFolderFile, destinationTreeUri: Uri): Boolean {
        val destinationFolder = DocumentFile.fromTreeUri(context, destinationTreeUri) ?: return false
        val target = destinationFolder.createFile(file.mimeType, file.displayName) ?: return false
        return copyContent(fileUri = Uri.parse(file.uriString), targetUri = target.uri)
    }

    fun moveToFolder(file: ManagedFolderFile, destinationTreeUri: Uri): Boolean {
        val copied = copyToFolder(file, destinationTreeUri)
        if (!copied) return false
        return delete(file)
    }

    private fun copyContent(fileUri: Uri, targetUri: Uri): Boolean {
        return runCatching {
            context.contentResolver.openInputStream(fileUri)?.use { input ->
                context.contentResolver.openOutputStream(targetUri)?.use { output ->
                    input.copyTo(output)
                } ?: return false
            } ?: return false
            true
        }.getOrDefault(false)
    }
}
