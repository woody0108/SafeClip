package com.glass.safeclip.data.file

data class ManagedFolderFileSummary(
    val videoCount: Int,
    val photoCount: Int
) {
    val mediaCount: Int = videoCount + photoCount

    companion object {
        fun from(files: List<ManagedFolderFile>): ManagedFolderFileSummary {
            return ManagedFolderFileSummary(
                videoCount = files.count(ManagedFolderVideoCandidate::canUseVideoActions),
                photoCount = files.count(ManagedFolderVideoCandidate::canPreviewImage)
            )
        }
    }
}
