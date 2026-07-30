package com.glass.safeclip.ui.navigation

object SafeClipBackNavigation {
    fun previousScreen(screen: SafeClipScreen): SafeClipScreen? {
        return when (screen) {
            SafeClipScreen.Boot -> null
            SafeClipScreen.Start -> null
            SafeClipScreen.Connecting -> SafeClipScreen.Start
            SafeClipScreen.Home -> SafeClipScreen.Start
            is SafeClipScreen.VideoBrowser -> SafeClipScreen.Home
            is SafeClipScreen.FolderManager -> SafeClipScreen.Home
            is SafeClipScreen.VideoPreview -> screen.returnScreen
            is SafeClipScreen.ImagePreview -> screen.returnScreen
            is SafeClipScreen.SubmissionForm -> screen.returnScreen
            SafeClipScreen.SubmissionStatus -> SafeClipScreen.Home
            SafeClipScreen.Settings -> SafeClipScreen.Home
            SafeClipScreen.Ask -> SafeClipScreen.Settings
        }
    }
}
