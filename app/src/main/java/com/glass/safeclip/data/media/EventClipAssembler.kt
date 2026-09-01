package com.glass.safeclip.data.media

import android.net.Uri
import com.glass.safeclip.data.recording.EventClipPlan
import com.glass.safeclip.data.recording.EventLocation

interface EventClipAssembler {
    suspend fun assemble(
        plan: EventClipPlan,
        displayName: String,
        location: EventLocation? = null
    ): Result<Uri>
}
