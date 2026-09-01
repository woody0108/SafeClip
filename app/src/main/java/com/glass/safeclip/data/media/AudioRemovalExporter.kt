package com.glass.safeclip.data.media

import android.net.Uri
import kotlinx.coroutines.flow.Flow

sealed interface MediaExportProgress {
    data class Running(val percent: Int) : MediaExportProgress
    data class Completed(val uri: Uri) : MediaExportProgress
    data class Failed(val message: String) : MediaExportProgress
}

interface AudioRemovalExporter {
    fun export(sourceUri: Uri, displayName: String): Flow<MediaExportProgress>
}
