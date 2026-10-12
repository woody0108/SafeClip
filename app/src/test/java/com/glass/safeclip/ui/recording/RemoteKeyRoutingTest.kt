package com.glass.safeclip.ui.recording

import android.view.KeyEvent
import com.glass.safeclip.ui.navigation.SafeClipScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteKeyRoutingTest {
    @Test
    fun volumeKeyIsOfferedOnlyWhileRecordingScreenIsActive() {
        assertTrue(RemoteKeyRouting.shouldOffer(SafeClipScreen.LiveRecording, KeyEvent.KEYCODE_VOLUME_UP))
        assertFalse(RemoteKeyRouting.shouldOffer(SafeClipScreen.Home, KeyEvent.KEYCODE_VOLUME_UP))
    }

    @Test
    fun volumeKeysSelectDifferentEventWindows() {
        assertEquals(60_000L, RemoteKeyRouting.eventSideDurationMs(KeyEvent.KEYCODE_VOLUME_DOWN))
        assertEquals(300_000L, RemoteKeyRouting.eventSideDurationMs(KeyEvent.KEYCODE_VOLUME_UP))
        assertEquals(60_000L, RemoteKeyRouting.eventSideDurationMs(KeyEvent.KEYCODE_CAMERA))
    }
    @Test
    fun repeatedVolumeKeyIsConsumedWithoutOfferingAnotherEvent() {
        var offeredCount = 0

        assertTrue(RemoteKeyRouting.consumeKeyDown(KeyEvent.KEYCODE_VOLUME_UP, repeatCount = 0) {
            offeredCount += 1
        })
        assertTrue(RemoteKeyRouting.consumeKeyDown(KeyEvent.KEYCODE_VOLUME_UP, repeatCount = 1) {
            offeredCount += 1
        })

        assertEquals(1, offeredCount)
        assertFalse(RemoteKeyRouting.consumeKeyDown(KeyEvent.KEYCODE_BACK, repeatCount = 0) {
            offeredCount += 1
        })
    }
}
