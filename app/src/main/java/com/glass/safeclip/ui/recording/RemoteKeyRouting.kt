package com.glass.safeclip.ui.recording

import android.view.KeyEvent
import com.glass.safeclip.ui.navigation.SafeClipScreen

object RemoteKeyRouting {
    const val SHORT_EVENT_SIDE_DURATION_MS = 60_000L
    const val LONG_EVENT_SIDE_DURATION_MS = 5L * 60L * 1_000L

    fun shouldOffer(screen: SafeClipScreen, keyCode: Int): Boolean {
        if (screen != SafeClipScreen.LiveRecording) return false
        return eventSideDurationMs(keyCode) != null
    }

    fun eventSideDurationMs(keyCode: Int): Long? = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> LONG_EVENT_SIDE_DURATION_MS
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_CAMERA -> SHORT_EVENT_SIDE_DURATION_MS
        else -> null
    }

    fun consumeKeyDown(keyCode: Int, repeatCount: Int, offerEvent: () -> Unit): Boolean {
        if (eventSideDurationMs(keyCode) == null) return false
        if (repeatCount == 0) offerEvent()
        return true
    }
}
