package com.glass.safeclip.data.recording

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteEventDebouncerTest {
    @Test
    fun remoteInputIsDebouncedForThreeSeconds() {
        val debouncer = RemoteEventDebouncer(debounceMs = 3_000)

        assertTrue(debouncer.accept(10_000))
        assertFalse(debouncer.accept(12_999))
        assertTrue(debouncer.accept(13_000))
    }

    @Test
    fun resetAllowsTheNextInputImmediately() {
        val debouncer = RemoteEventDebouncer(debounceMs = 3_000)
        debouncer.accept(10_000)

        debouncer.reset()

        assertTrue(debouncer.accept(10_001))
    }
}
