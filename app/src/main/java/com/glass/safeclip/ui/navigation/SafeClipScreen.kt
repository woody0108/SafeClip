package com.glass.safeclip.ui.navigation

import com.glass.safeclip.data.media.VideoClipExportResult
import com.glass.safeclip.data.file.ManagedFolderFile
import com.glass.safeclip.data.submission.SubmissionAttachment
import com.glass.safeclip.domain.model.VideoCandidate
import com.glass.safeclip.ui.folder.FolderViewKind
import com.glass.safeclip.ui.video.VideoBrowserSource

sealed interface SafeClipScreen {
    data object Boot : SafeClipScreen
    data object Start : SafeClipScreen
    data object Connecting : SafeClipScreen
    data object Home : SafeClipScreen
    data object LiveRecording : SafeClipScreen
    data class VideoBrowser(val initialSource: VideoBrowserSource = VideoBrowserSource.Blackbox) : SafeClipScreen
    data class FolderManager(val kind: FolderViewKind) : SafeClipScreen
    data class VideoPreview(
        val video: VideoCandidate,
        val returnScreen: SafeClipScreen = VideoBrowser()
    ) : SafeClipScreen
    data class ImagePreview(
        val file: ManagedFolderFile,
        val sourceKind: FolderViewKind,
        val returnScreen: SafeClipScreen = FolderManager(sourceKind)
    ) : SafeClipScreen
    data class SubmissionForm(
        val video: VideoCandidate,
        val clip: VideoClipExportResult?,
        val initialAttachment: SubmissionAttachment,
        val availableFiles: List<ManagedFolderFile>,
        val availableFolderPath: String,
        val eventFiles: List<ManagedFolderFile>,
        val returnScreen: SafeClipScreen = VideoPreview(video)
    ) : SafeClipScreen
    data object SubmissionStatus : SafeClipScreen
    data object MyPage : SafeClipScreen
    data class Settings(val returnScreen: SafeClipScreen = Home) : SafeClipScreen
    data object Ask : SafeClipScreen

    companion object {
        fun first(): SafeClipScreen = Boot
    }
}
