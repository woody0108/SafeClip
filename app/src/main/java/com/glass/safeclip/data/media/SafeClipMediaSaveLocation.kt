package com.glass.safeclip.data.media

object SafeClipMediaSaveLocation {
    const val albumName = "SafeClip Captures"
    private const val picturesDirectory = "Pictures"
    private const val moviesDirectory = "Movies"

    val imageCaptureRelativePath: String
        get() = "$picturesDirectory/$albumName"

    val videoClipRelativePath: String
        get() = "$moviesDirectory/$albumName"

    fun videoClipDisplayPath(fileName: String): String {
        return "$videoClipRelativePath/$fileName"
    }

    fun eventFolderDisplayPath(fileName: String): String {
        return "$albumName/$fileName"
    }
}
