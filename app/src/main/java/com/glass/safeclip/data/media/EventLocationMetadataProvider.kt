package com.glass.safeclip.data.media

import androidx.annotation.OptIn
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.container.Mp4LocationData
import androidx.media3.transformer.InAppMp4Muxer
import com.glass.safeclip.data.recording.EventLocation

@OptIn(UnstableApi::class)
class EventLocationMetadataProvider(
    private val location: EventLocation?
) : InAppMp4Muxer.MetadataProvider {
    override fun updateMetadataEntries(metadataEntries: MutableSet<Metadata.Entry>) {
        metadataEntries.removeAll { it is Mp4LocationData }
        location?.let {
            metadataEntries += Mp4LocationData(it.latitude.toFloat(), it.longitude.toFloat())
        }
    }
}
