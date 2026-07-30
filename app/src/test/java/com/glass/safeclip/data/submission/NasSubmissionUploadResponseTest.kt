package com.glass.safeclip.data.submission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NasSubmissionUploadResponseTest {
    @Test
    fun parsesSuccessfulUploadResponse() {
        val json = """{"ok":true,"stored_name":"stored.mp4","relative_path":"2026/07/29/베짱이들/01/stored.mp4","size_bytes":1234,"submission_folder":"2026/07/29/베짱이들/01","submission_sequence":1,"submission_sequence_text":"01"}"""

        val response = NasSubmissionUploadResponse.parse(json)

        assertEquals("stored.mp4", response.storedName)
        assertEquals("2026/07/29/베짱이들/01/stored.mp4", response.relativePath)
        assertEquals(1234L, response.sizeBytes)
        assertEquals("2026/07/29/베짱이들/01", response.submissionFolder)
        assertEquals(1, response.submissionSequence)
        assertEquals("01", response.submissionSequenceText)
    }

    @Test
    fun rejectsFailedUploadResponse() {
        val json = """{"ok":false,"error":"Invalid upload key."}"""

        val result = runCatching { NasSubmissionUploadResponse.parse(json) }

        assertTrue(result.isFailure)
        assertEquals("Invalid upload key.", result.exceptionOrNull()?.message)
    }
}
