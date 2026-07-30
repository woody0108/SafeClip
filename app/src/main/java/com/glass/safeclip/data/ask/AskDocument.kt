package com.glass.safeclip.data.ask

import com.google.firebase.firestore.FieldValue

object AskDocument {
    fun createFields(
        id: String,
        questionType: String,
        question: String
    ): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "questionType" to questionType,
            "questionAt" to FieldValue.serverTimestamp(),
            "question" to question,
            "answer" to ""
        )
    }
}
