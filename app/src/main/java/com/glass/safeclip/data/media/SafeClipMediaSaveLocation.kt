package com.glass.safeclip.data.media

import java.io.File

object SafeClipMediaSaveLocation {
    const val albumName = "SafeClip"
    const val relativePath = "DCIM/SafeClip"

    val imageCaptureRelativePath: String
        get() = relativePath

    val videoClipRelativePath: String
        get() = relativePath

    fun publicDirectory(dcimDirectory: File): File = File(dcimDirectory, albumName)

    fun ensurePublicDirectory(dcimDirectory: File): Boolean {
        val directory = publicDirectory(dcimDirectory)
        return directory.isDirectory || (!directory.exists() && directory.mkdirs())
    }

    fun displayPath(fileName: String): String = "$relativePath/$fileName"

    fun videoClipDisplayPath(fileName: String): String {
        return displayPath(fileName)
    }

    fun eventFolderDisplayPath(fileName: String): String {
        return displayPath(fileName)
    }
}
