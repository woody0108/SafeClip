package com.glass.safeclip.ui.recording

import android.view.KeyEvent
import com.glass.safeclip.ui.navigation.SafeClipScreen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteKeyRoutingTest {
    @Test
    fun volumeKeyIsOfferedOnlyWhileRecordingScreenIsActive() {
        assertTrue(RemoteKeyRouting.shouldOffer(SafeClipScreen.LiveRecording, KeyEvent.KEYCODE_VOLUME_UP))
        assertFalse(RemoteKeyRouting.shouldOffer(SafeClipScreen.Home, KeyEvent.KEYCODE_VOLUME_UP))
    }
}
