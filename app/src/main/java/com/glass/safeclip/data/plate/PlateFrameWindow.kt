package com.glass.safeclip.data.plate

data class PlateFrame(
    val elapsedMs: Long,
    val jpegBytes: ByteArray
)

class PlateFrameWindow(
    private val maxFrames: Int,
    private val retentionMs: Long
) {
    private val frames = ArrayDeque<PlateFrame>()

    @Synchronized
    fun add(frame: PlateFrame) {
        frames.addLast(frame)
        while (frames.size > maxFrames ||
            (frames.firstOrNull()?.elapsedMs ?: frame.elapsedMs) < frame.elapsedMs - retentionMs
        ) {
            frames.removeFirst()
        }
    }

    @Synchronized
    fun snapshotAround(triggerElapsedMs: Long, radiusMs: Long): List<PlateFrame> {
        return frames.filter { it.elapsedMs in (triggerElapsedMs - radiusMs)..(triggerElapsedMs + radiusMs) }
    }

    @Synchronized
    fun clear() = frames.clear()
}
