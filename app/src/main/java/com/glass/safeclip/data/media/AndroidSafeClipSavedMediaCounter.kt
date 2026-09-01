package com.glass.safeclip.data.media

import android.content.Context
import com.glass.safeclip.data.file.LastSelectedEventFolderStore

class AndroidSafeClipSavedMediaCounter(
    private val context: Context,
    private val eventFolderStore: LastSelectedEventFolderStore? = null
) {
    fun countSavedItems(): Int {
        return AndroidSafeClipSavedMediaRepository(context, eventFolderStore)
            .listSavedItems()
            .size
    }
}
