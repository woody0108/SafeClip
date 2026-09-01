package com.glass.safeclip.ui.recording

import android.content.pm.ActivityInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveRecordingOrientationTest {
    @Test
    fun recordingUsesLandscapeAndReturnsToPortrait() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            LiveRecordingOrientation.recording
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            LiveRecordingOrientation.afterRecording
        )
    }
}
