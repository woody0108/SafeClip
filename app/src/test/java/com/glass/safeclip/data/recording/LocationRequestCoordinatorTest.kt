package com.glass.safeclip.data.recording

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationRequestCoordinatorTest {
    @Test
    fun preservesFastLocationWhenAnotherProviderTimesOut() = runTest {
        val results = LocationRequestCoordinator.collectAvailable(
            timeoutMs = 50L,
            requests = listOf(
                {
                    delay(100L)
                    "gps"
                },
                {
                    delay(10L)
                    "network"
                }
            )
        )

        assertEquals(listOf("network"), results)
    }

    @Test
    fun returnsEmptyWhenEveryProviderTimesOut() = runTest {
        val results = LocationRequestCoordinator.collectAvailable(
            timeoutMs = 50L,
            requests = listOf(
                {
                    delay(100L)
                    "gps"
                }
            )
        )

        assertEquals(emptyList<String>(), results)
    }
}
