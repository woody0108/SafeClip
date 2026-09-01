package com.glass.safeclip.data.recording

class RemoteEventDebouncer(
    private val debounceMs: Long
) {
    private var lastAcceptedElapsedMs: Long? = null

    init {
        require(debounceMs >= 0) { "Debounce duration cannot be negative." }
    }

    fun accept(elapsedMs: Long): Boolean {
        val previous = lastAcceptedElapsedMs
        if (previous != null && elapsedMs - previous < debounceMs) {
            return false
        }
        lastAcceptedElapsedMs = elapsedMs
        return true
    }

    fun reset() {
        lastAcceptedElapsedMs = null
    }
}
