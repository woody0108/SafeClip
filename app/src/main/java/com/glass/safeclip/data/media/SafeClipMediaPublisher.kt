package com.glass.safeclip.data.media

import android.net.Uri
import java.io.File

interface SafeClipMediaPublisher {
    suspend fun publishVideo(source: File, displayName: String): Uri

    suspend fun publishImage(
        source: File,
        displayName: String,
        mimeType: String
    ): Uri
}
