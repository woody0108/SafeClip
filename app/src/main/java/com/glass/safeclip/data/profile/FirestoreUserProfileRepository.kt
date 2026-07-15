package com.glass.safeclip.data.profile

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreUserProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun saveLogin(profile: UserProfile): UserProfileSyncResult {
        return try {
            val document = firestore.collection(COLLECTION_USERS).document(profile.uid)
            val snapshot = document.get().await()
            val fields = if (snapshot.exists()) {
                UserProfileDocument.loginUpdateFields(profile)
            } else {
                UserProfileDocument.createFields(profile)
            }
            document.set(fields, com.google.firebase.firestore.SetOptions.merge()).await()
            UserProfileSyncResult.Success(
                profile = profile,
                message = if (snapshot.exists()) {
                    "Firestore 사용자 정보가 갱신되었습니다."
                } else {
                    "Firestore 사용자 정보가 저장되었습니다."
                }
            )
        } catch (exception: Exception) {
            UserProfileSyncResult.Failed(
                exception.localizedMessage ?: "Firestore 사용자 정보 저장에 실패했습니다."
            )
        }
    }

    suspend fun load(uid: String): UserProfileSyncResult {
        return try {
            val snapshot = firestore.collection(COLLECTION_USERS).document(uid).get().await()
            val data = snapshot.data
                ?: return UserProfileSyncResult.Failed("Firestore 사용자 문서를 찾지 못했습니다.")
            UserProfileSyncResult.Success(
                profile = UserProfileDocument.fromFirestore(uid = uid, data = data),
                message = "Firestore 사용자 정보를 불러왔습니다."
            )
        } catch (exception: Exception) {
            UserProfileSyncResult.Failed(
                exception.localizedMessage ?: "Firestore 사용자 정보 불러오기에 실패했습니다."
            )
        }
    }

    suspend fun delete(uid: String): UserProfileSyncResult {
        return try {
            firestore.collection(COLLECTION_USERS).document(uid).delete().await()
            UserProfileSyncResult.Success(
                profile = UserProfile(
                    uid = uid,
                    guestId = "",
                    email = null,
                    displayName = null,
                    provider = "deleted"
                ),
                message = "Firestore 사용자 문서가 삭제되었습니다."
            )
        } catch (exception: Exception) {
            UserProfileSyncResult.Failed(
                exception.localizedMessage ?: "Firestore 사용자 문서 삭제에 실패했습니다."
            )
        }
    }

    private companion object {
        const val COLLECTION_USERS = "users"
    }
}
