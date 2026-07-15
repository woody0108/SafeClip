package com.glass.safeclip.data.media

data class PlaybackSpeedOption(
    val label: String,
    val speed: Float
)

object PlaybackSpeedOptions {
    val supported = listOf(
        PlaybackSpeedOption(label = "0.25x", speed = 0.25f),
        PlaybackSpeedOption(label = "0.5x", speed = 0.5f),
        PlaybackSpeedOption(label = "1.0x", speed = 1.0f),
        PlaybackSpeedOption(label = "1.5x", speed = 1.5f),
        PlaybackSpeedOption(label = "2.0x", speed = 2.0f)
    )
}
