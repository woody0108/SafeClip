package com.glass.safeclip.data.ask

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class FirestoreAskRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val askApiUrl: String = "",
    private val uploadKey: String = ""
) {
    suspend fun add(
        id: String,
        questionType: String,
        question: String
    ): AskSaveResult {
        val cleanQuestion = question.trim()
        val cleanQuestionType = questionType.trim().ifBlank { "기타 문의" }
        if (cleanQuestion.isBlank()) {
            return AskSaveResult.Failed("문의 내용을 입력해주세요.")
        }

        return try {
            if (askApiUrl.isNotBlank()) {
                createViaApi(id = id, questionType = cleanQuestionType, question = cleanQuestion)
            } else {
                firestore.collection(COLLECTION_ASK)
                    .add(AskDocument.createFields(id = id, questionType = cleanQuestionType, question = cleanQuestion))
                    .await()
            }
            AskSaveResult.Success("일반 문의가 접수되었습니다.")
        } catch (exception: Exception) {
            AskSaveResult.Failed(exception.localizedMessage ?: "일반 문의 접수에 실패했습니다.")
        }
    }

    suspend fun listById(id: String): AskListResult {
        if (id.isBlank()) {
            return AskListResult.Success(emptyList())
        }

        return try {
            val asks = if (askApiUrl.isNotBlank()) {
                listViaApi(id)
            } else {
                listViaFirestore(id)
            }
            AskListResult.Success(asks)
        } catch (exception: Exception) {
            AskListResult.Failed(exception.localizedMessage ?: "일반 문의 내역을 불러오지 못했습니다.")
        }
    }

    suspend fun delete(documentId: String): AskDeleteResult {
        val cleanDocumentId = documentId.trim()
        if (cleanDocumentId.isBlank()) {
            return AskDeleteResult.Failed("삭제할 문의를 찾지 못했습니다.")
        }

        return try {
            if (askApiUrl.isNotBlank()) {
                deleteViaApi(cleanDocumentId)
            } else {
                firestore.collection(COLLECTION_ASK)
                    .document(cleanDocumentId)
                    .delete()
                    .await()
            }
            AskDeleteResult.Success("문의가 삭제되었습니다.")
        } catch (exception: Exception) {
            AskDeleteResult.Failed(exception.localizedMessage ?: "문의 삭제에 실패했습니다.")
        }
    }

    private suspend fun createViaApi(
        id: String,
        questionType: String,
        question: String
    ) = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("action", "create")
            .put("id", id)
            .put("questionType", questionType)
            .put("question", question)
            .toString()
            .toByteArray(Charsets.UTF_8)
        val connection = (URL(askApiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            doInput = true
            connectTimeout = 10_000
            readTimeout = 20_000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (uploadKey.isNotBlank()) {
                setRequestProperty("X-SafeClip-Upload-Key", uploadKey)
            }
        }

        try {
            connection.outputStream.use { output ->
                output.write(body)
                output.flush()
            }
            val responseText = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: """{"ok":false,"error":"문의 API 요청 실패: HTTP ${connection.responseCode}"}"""
            }
            val response = JSONObject(responseText)
            if (!response.optBoolean("ok", false)) {
                throw IllegalStateException(response.optString("error", "문의 API 요청에 실패했습니다."))
            }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun listViaApi(id: String): List<AskItem> = withContext(Dispatchers.IO) {
        val encodedId = URLEncoder.encode(id, Charsets.UTF_8.name())
        val separator = if (askApiUrl.contains("?")) "&" else "?"
        val connection = (URL("$askApiUrl${separator}id=$encodedId&limit=50").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            doInput = true
            connectTimeout = 10_000
            readTimeout = 20_000
            if (uploadKey.isNotBlank()) {
                setRequestProperty("X-SafeClip-Upload-Key", uploadKey)
            }
        }

        try {
            val responseText = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: """{"ok":false,"error":"문의 목록 API 요청 실패: HTTP ${connection.responseCode}"}"""
            }
            val response = JSONObject(responseText)
            if (!response.optBoolean("ok", false)) {
                throw IllegalStateException(response.optString("error", "문의 목록 API 요청에 실패했습니다."))
            }
            response.optJSONArray("asks").toAskItems()
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun deleteViaApi(documentId: String) = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("action", "delete")
            .put("documentId", documentId)
            .toString()
            .toByteArray(Charsets.UTF_8)
        val connection = (URL(askApiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            doInput = true
            connectTimeout = 10_000
            readTimeout = 20_000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (uploadKey.isNotBlank()) {
                setRequestProperty("X-SafeClip-Upload-Key", uploadKey)
            }
        }

        try {
            connection.outputStream.use { output ->
                output.write(body)
                output.flush()
            }
            val responseText = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: """{"ok":false,"error":"문의 삭제 API 요청 실패: HTTP ${connection.responseCode}"}"""
            }
            val response = JSONObject(responseText)
            if (!response.optBoolean("ok", false)) {
                throw IllegalStateException(response.optString("error", "문의 삭제 API 요청에 실패했습니다."))
            }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun listViaFirestore(id: String): List<AskItem> {
        val snapshot = firestore.collection(COLLECTION_ASK)
            .whereEqualTo("id", id)
            .get()
            .await()
        return snapshot.documents
            .map { document ->
                AskItem(
                    documentId = document.id,
                    id = document.getString("id").orEmpty(),
                    questionType = document.getString("questionType").orEmpty(),
                    questionAtText = document.getTimestamp("questionAt")?.toDate()?.toString().orEmpty(),
                    question = document.getString("question").orEmpty(),
                    answer = document.getString("answer").orEmpty()
                )
            }
            .sortedByDescending { it.questionAtText }
    }

    companion object {
        const val COLLECTION_ASK = "ask"

        fun askApiUrlFromUploadUrl(uploadUrl: String): String {
            val cleanUrl = uploadUrl.trim()
            val uploadPath = "/api/nas-upload-api/public/upload.php"
            return if (cleanUrl.endsWith(uploadPath)) {
                cleanUrl.removeSuffix(uploadPath) + "/api/asks.php"
            } else {
                ""
            }
        }
    }
}

private fun JSONArray?.toAskItems(): List<AskItem> {
    if (this == null) {
        return emptyList()
    }

    return (0 until length()).mapNotNull { index ->
        val item = optJSONObject(index) ?: return@mapNotNull null
        AskItem(
            documentId = item.optString("documentId"),
            id = item.optString("id"),
            questionType = item.optString("questionType"),
            questionAtText = item.optString("questionAtText"),
            question = item.optString("question"),
            answer = item.optString("answer")
        )
    }
}

data class AskItem(
    val documentId: String,
    val id: String,
    val questionType: String,
    val questionAtText: String,
    val question: String,
    val answer: String
)

sealed interface AskSaveResult {
    data class Success(val message: String) : AskSaveResult
    data class Failed(val message: String) : AskSaveResult
}

sealed interface AskListResult {
    data class Success(val items: List<AskItem>) : AskListResult
    data class Failed(val message: String) : AskListResult
}

sealed interface AskDeleteResult {
    data class Success(val message: String) : AskDeleteResult
    data class Failed(val message: String) : AskDeleteResult
}
