package com.glass.safeclip.ui.recording

import android.view.KeyEvent
import com.glass.safeclip.ui.navigation.SafeClipScreen

object RemoteKeyRouting {
    fun shouldOffer(screen: SafeClipScreen, keyCode: Int): Boolean {
        if (screen != SafeClipScreen.LiveRecording) return false
        return keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_CAMERA
    }
}
