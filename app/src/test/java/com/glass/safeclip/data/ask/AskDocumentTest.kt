package com.glass.safeclip.data.ask

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AskDocumentTest {
    @Test
    fun createsAskFieldsWithEmptyAnswer() {
        val fields = AskDocument.createFields(
            id = "Guest-C2D0-3702",
            questionType = "아이디 관련 문의",
            question = "문의합니다."
        )

        assertEquals("Guest-C2D0-3702", fields["id"])
        assertEquals("아이디 관련 문의", fields["questionType"])
        assertEquals("문의합니다.", fields["question"])
        assertEquals("", fields["answer"])
        assertTrue(fields.containsKey("questionAt"))
    }

    @Test
    fun derivesAskApiUrlFromNasUploadUrl() {
        val askUrl = FirestoreAskRepository.askApiUrlFromUploadUrl(
            "http://192.168.0.3:8080/api/nas-upload-api/public/upload.php"
        )

        assertEquals("http://192.168.0.3:8080/api/asks.php", askUrl)
    }
}
