package com.glass.safeclip.data.media

import androidx.media3.common.Metadata
import androidx.media3.container.Mp4LocationData
import androidx.media3.container.Mp4OrientationData
import com.glass.safeclip.data.recording.EventLocation
import java.util.LinkedHashSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventLocationMetadataProviderTest {
    @Test
    fun buttonLocationReplacesAnyLocationCopiedFromInputSegments() {
        val entries = linkedSetOf<Metadata.Entry>(
            Mp4OrientationData(0),
            Mp4LocationData(37.0f, 127.0f)
        )
        val location = EventLocation(35.5396, 129.3114, 8.5f, 1_787_725_441_000)

        EventLocationMetadataProvider(location).updateMetadataEntries(entries)

        val locations = entries.filterIsInstance<Mp4LocationData>()
        assertEquals(1, locations.size)
        assertEquals(35.5396f, locations.single().latitude)
        assertEquals(129.3114f, locations.single().longitude)
        assertTrue(entries.any { it is Mp4OrientationData })
    }

    @Test
    fun missingButtonLocationRemovesStaleInputLocation() {
        val entries = LinkedHashSet<Metadata.Entry>()
        entries += Mp4LocationData(37.0f, 127.0f)

        EventLocationMetadataProvider(null).updateMetadataEntries(entries)

        assertTrue(entries.none { it is Mp4LocationData })
    }
}
