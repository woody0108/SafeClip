package com.glass.safeclip.ui.navigation

import com.glass.safeclip.data.media.VideoClipExportResult
import com.glass.safeclip.domain.model.VideoCandidate
import com.glass.safeclip.ui.folder.FolderViewKind

sealed interface SafeClipScreen {
    data object Start : SafeClipScreen
    data object Connecting : SafeClipScreen
    data object Home : SafeClipScreen
    data object VideoBrowser : SafeClipScreen
    data class FolderManager(val kind: FolderViewKind) : SafeClipScreen
    data class VideoPreview(val video: VideoCandidate) : SafeClipScreen
    data class SubmissionForm(
        val video: VideoCandidate,
        val clip: VideoClipExportResult?
    ) : SafeClipScreen
    data object SubmissionStatus : SafeClipScreen
    data object Settings : SafeClipScreen

    companion object {
        fun first(): SafeClipScreen = Start
    }
}
