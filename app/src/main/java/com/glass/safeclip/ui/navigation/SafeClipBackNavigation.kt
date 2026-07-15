package com.glass.safeclip.ui.navigation

object SafeClipBackNavigation {
    fun previousScreen(screen: SafeClipScreen): SafeClipScreen? {
        return when (screen) {
            SafeClipScreen.Start -> null
            SafeClipScreen.Connecting -> SafeClipScreen.Start
            SafeClipScreen.Home -> SafeClipScreen.Start
            SafeClipScreen.VideoBrowser -> SafeClipScreen.Home
            is SafeClipScreen.FolderManager -> SafeClipScreen.Home
            is SafeClipScreen.VideoPreview -> SafeClipScreen.VideoBrowser
            is SafeClipScreen.SubmissionForm -> SafeClipScreen.VideoPreview(screen.video)
            SafeClipScreen.SubmissionStatus -> SafeClipScreen.Home
            SafeClipScreen.Settings -> SafeClipScreen.Home
        }
    }
}
