package com.glass.safeclip.data.submission

import kotlin.math.ceil

data class NasSubmissionUploadProgress(
    val fileIndex: Int,
    val totalFiles: Int,
    val fileName: String,
    val bytesSent: Long,
    val totalBytes: Long?,
    val startedAtMillis: Long? = null,
    val nowMillis: Long? = null
) {
    val currentFilePercent: Int?
        get() {
            val total = totalBytes ?: return null
            if (total <= 0L) return null
            return ((bytesSent.coerceAtMost(total) * 100L) / total).toInt()
        }

    val progressFraction: Float?
        get() = currentFilePercent?.let { it / 100f }

    val fileLabel: String
        get() = "$fileIndex/$totalFiles $fileName"

    val remainingSeconds: Long?
        get() {
            val start = startedAtMillis ?: return null
            val now = nowMillis ?: return null
            val total = totalBytes ?: return null
            val remainingBytes = (total - bytesSent).coerceAtLeast(0L)
            if (bytesSent <= 0L || total <= 0L || now <= start) return null
            if (remainingBytes == 0L) return 0L

            val elapsedSeconds = (now - start) / 1000.0
            val bytesPerSecond = bytesSent / elapsedSeconds
            if (bytesPerSecond <= 0.0) return null

            return ceil(remainingBytes / bytesPerSecond).toLong()
        }

    val remainingTimeText: String
        get() {
            val seconds = remainingSeconds ?: return "남은 시간 계산 중"
            return when {
                seconds <= 0L -> "곧 완료"
                seconds < 60L -> "예상 남은 시간 ${seconds}초"
                else -> "예상 남은 시간 ${seconds / 60L}분 ${seconds % 60L}초"
            }
        }

    companion object {
        fun changedPercentEvents(totalBytes: Long?, sentBytes: List<Long>): List<Int> {
            var lastPercent: Int? = null
            return sentBytes.mapNotNull { sent ->
                val percent = NasSubmissionUploadProgress(
                    fileIndex = 1,
                    totalFiles = 1,
                    fileName = "",
                    bytesSent = sent,
                    totalBytes = totalBytes
                ).currentFilePercent ?: return@mapNotNull null
                if (percent == lastPercent) {
                    null
                } else {
                    lastPercent = percent
                    percent
                }
            }
        }
    }
}
