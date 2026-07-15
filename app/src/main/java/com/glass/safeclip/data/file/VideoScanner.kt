package com.glass.safeclip.data.file

import com.glass.safeclip.domain.model.VideoCandidate

class VideoScanner {
    fun scan(root: VideoDocument): List<VideoCandidate> {
        val candidates = mutableListOf<VideoCandidate>()
        scanInto(document = root, folderPath = root.displayName, candidates = candidates)
        return VideoCandidateRules.sortRecentFirst(candidates)
    }

    private fun scanInto(
        document: VideoDocument,
        folderPath: String,
        candidates: MutableList<VideoCandidate>
    ) {
        if (document.isDirectory) {
            document.children.forEach { child ->
                val childPath = if (child.isDirectory) "$folderPath/${child.displayName}" else folderPath
                scanInto(child, childPath, candidates)
            }
            return
        }

        if (VideoCandidateRules.isSupportedVideoFile(document.displayName)) {
            candidates += VideoCandidate(
                uriString = document.uriString,
                displayName = document.displayName,
                sizeBytes = document.sizeBytes,
                lastModifiedMillis = document.lastModifiedMillis,
                folderPath = folderPath
            )
        }
    }
}
