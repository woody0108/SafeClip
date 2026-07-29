package com.glass.safeclip.data.submission

data class NasSubmissionUploadResponse(
    val storedName: String,
    val relativePath: String,
    val sizeBytes: Long
) {
    companion object {
        fun parse(json: String): NasSubmissionUploadResponse {
            val ok = Regex(""""ok"\s*:\s*true""").containsMatchIn(json)
            if (!ok) {
                val error = stringValue(json, "error") ?: "NAS 업로드에 실패했습니다."
                throw IllegalStateException(error)
            }
            return NasSubmissionUploadResponse(
                storedName = stringValue(json, "stored_name") ?: throw IllegalStateException("NAS 응답에 stored_name이 없습니다."),
                relativePath = stringValue(json, "relative_path") ?: throw IllegalStateException("NAS 응답에 relative_path가 없습니다."),
                sizeBytes = numberValue(json, "size_bytes") ?: 0L
            )
        }

        private fun stringValue(json: String, key: String): String? {
            val pattern = Regex(""""$key"\s*:\s*"([^"]*)"""")
            return pattern.find(json)?.groupValues?.get(1)
        }

        private fun numberValue(json: String, key: String): Long? {
            val pattern = Regex(""""$key"\s*:\s*(\d+)""")
            return pattern.find(json)?.groupValues?.get(1)?.toLongOrNull()
        }
    }
}
