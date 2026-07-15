package com.glass.safeclip.ui.video

import com.glass.safeclip.domain.model.VideoCandidate
import java.util.Locale

data class VideoListState(
    val selectedFolderName: String? = null,
    val selectedFolderUriString: String? = null,
    val isLoading: Boolean = false,
    val videos: List<VideoCandidate> = emptyList(),
    val savedMediaItemCount: Int = 0,
    val errorMessage: String? = null
)

object VideoListText {
    fun fileSizeLabel(sizeBytes: Long?): String {
        if (sizeBytes == null) return "크기 정보 없음"
        val kb = sizeBytes / 1024.0
        if (kb < 1024.0) return "${kb.toInt()} KB"
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.1f MB", mb)
    }
}

object FolderPickerResultText {
    fun messageForVideoCount(count: Int): String {
        return if (count == 0) {
            "선택한 폴더에서 지원 영상 파일을 찾지 못했습니다."
        } else {
            "영상 후보 ${count}개를 찾았습니다."
        }
    }
}
