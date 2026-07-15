package com.glass.safeclip.data.file

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object VideoDocumentAdapterRules {
    fun safeDisplayName(rawName: String?, fallback: String): String {
        return rawName?.takeIf { it.isNotBlank() } ?: fallback
    }
}

class AndroidDocumentTreeVideoSource(private val context: Context) {
    fun loadTree(treeUri: Uri): VideoDocument? {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        return root.toVideoDocument(fallbackName = "선택한 폴더")
    }

    private fun DocumentFile.toVideoDocument(fallbackName: String): VideoDocument {
        val name = VideoDocumentAdapterRules.safeDisplayName(this.name, fallbackName)
        return if (isDirectory) {
            VideoDocument.folder(
                displayName = name,
                uriString = uri.toString(),
                children = listFiles().map { child -> child.toVideoDocument(fallbackName = "이름 없는 항목") }
            )
        } else {
            VideoDocument.file(
                displayName = name,
                uriString = uri.toString(),
                sizeBytes = length().takeIf { it >= 0 },
                lastModifiedMillis = lastModified().takeIf { it > 0 }
            )
        }
    }
}
