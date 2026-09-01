package com.glass.safeclip.data.recording

data class RollingSegment(
    val id: String,
    val uriString: String,
    val startElapsedMs: Long,
    val endElapsedMs: Long,
    val isProtected: Boolean
)
