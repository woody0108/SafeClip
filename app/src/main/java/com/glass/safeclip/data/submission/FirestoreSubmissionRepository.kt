package com.glass.safeclip.data.submission

import com.glass.safeclip.ui.status.LocalSubmissionRecord
import com.glass.safeclip.data.profile.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreSubmissionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun add(input: SubmissionInput): SubmissionSaveResult {
        return try {
            val document = firestore.collection(COLLECTION_SUBMISSIONS)
                .add(SubmissionDocument.createFields(input))
                .await()
            SubmissionSaveResult.Success(
                documentId = document.id,
                message = "제출 내역이 Firestore에 저장되었습니다."
            )
        } catch (exception: Exception) {
            SubmissionSaveResult.Failed(
                exception.localizedMessage ?: "제출 내역 저장에 실패했습니다."
            )
        }
    }

    suspend fun findBy(key: SubmissionLookupKey): SubmissionListResult {
        return try {
            val snapshot = firestore.collection(COLLECTION_SUBMISSIONS)
                .whereEqualTo(key.fieldName, key.value)
                .get()
                .await()
            val records = snapshot.documents.mapNotNull { document ->
                document.data?.let { data ->
                    SubmissionDocument.toLocalRecord(
                        documentId = document.id,
                        data = data
                    )
                }
            }
            SubmissionListResult.Success(
                records = records,
                message = "제출 내역 ${records.size}개를 불러왔습니다."
            )
        } catch (exception: Exception) {
            SubmissionListResult.Failed(
                exception.localizedMessage ?: "제출 내역 불러오기에 실패했습니다."
            )
        }
    }

    suspend fun linkGuestSubmissionsToUser(profile: UserProfile): SubmissionOwnerLinkResult {
        if (profile.guestId.isBlank()) {
            return SubmissionOwnerLinkResult.Success(
                updatedCount = 0,
                message = "연결할 비회원 ID가 없습니다."
            )
        }
        return try {
            val snapshot = firestore.collection(COLLECTION_SUBMISSIONS)
                .whereEqualTo("guestId", profile.guestId)
                .get()
                .await()
            val fields = SubmissionDocument.guestOwnerLinkFields(profile)
            snapshot.documents.forEach { document ->
                document.reference.set(fields, SetOptions.merge()).await()
            }
            SubmissionOwnerLinkResult.Success(
                updatedCount = snapshot.size(),
                message = "비회원 제출내역 ${snapshot.size()}개가 계정 정보와 연결되었습니다."
            )
        } catch (exception: Exception) {
            SubmissionOwnerLinkResult.Failed(
                exception.localizedMessage ?: "비회원 제출내역 계정 연결에 실패했습니다."
            )
        }
    }

    private companion object {
        const val COLLECTION_SUBMISSIONS = "submissions"
    }
}

sealed interface SubmissionSaveResult {
    data class Success(
        val documentId: String,
        val message: String
    ) : SubmissionSaveResult

    data class Failed(
        val message: String
    ) : SubmissionSaveResult
}

sealed interface SubmissionListResult {
    data class Success(
        val records: List<LocalSubmissionRecord>,
        val message: String
    ) : SubmissionListResult

    data class Failed(
        val message: String
    ) : SubmissionListResult
}

sealed interface SubmissionOwnerLinkResult {
    data class Success(
        val updatedCount: Int,
        val message: String
    ) : SubmissionOwnerLinkResult

    data class Failed(
        val message: String
    ) : SubmissionOwnerLinkResult
}
