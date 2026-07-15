package com.glass.safeclip.data.auth

import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseAuthConnector(
    private val activity: ComponentActivity
) {
    suspend fun signInWithGoogle(): AuthConnectionResult = withContext(Dispatchers.Main) {
        val webClientId = activity.firebaseWebClientId()
            ?: return@withContext AuthConnectionResult.NeedsFirebaseSetup(
                "Firebase 설정이 아직 없습니다. google-services.json을 app 폴더에 넣으면 Google 로그인을 사용할 수 있어요."
            )

        val firebaseAuth = activity.firebaseAuthOrNull()
            ?: return@withContext AuthConnectionResult.NeedsFirebaseSetup(
                "Firebase가 초기화되지 않았습니다. Firebase 프로젝트 연결을 먼저 확인해주세요."
            )

        val credentialManager = CredentialManager.create(activity)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(activity, request)
            val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
            AuthConnectionResult.SignedIn(
                displayName = user?.displayName,
                email = user?.email
            )
        } catch (exception: GetCredentialException) {
            AuthConnectionResult.Failed("Google 계정 선택이 취소되었거나 실패했습니다.")
        } catch (exception: GoogleIdTokenParsingException) {
            AuthConnectionResult.Failed("Google 로그인 토큰을 읽지 못했습니다.")
        } catch (exception: Exception) {
            AuthConnectionResult.Failed(exception.localizedMessage ?: "Google 로그인에 실패했습니다.")
        }
    }

    suspend fun startEmailSignUp(): AuthConnectionResult = withContext(Dispatchers.Main) {
        if (activity.firebaseAuthOrNull() == null) {
            AuthConnectionResult.NeedsFirebaseSetup(
                "Firebase 설정이 아직 없습니다. google-services.json을 app 폴더에 넣으면 이메일 가입을 연결할 수 있어요."
            )
        } else {
            AuthConnectionResult.NeedsFirebaseSetup(
                "이메일 가입 화면은 다음 단계에서 이메일/비밀번호 입력 폼으로 연결하면 됩니다."
            )
        }
    }

    fun currentUserEmail(): String? {
        return activity.firebaseAuthOrNull()?.currentUser?.email
    }

    suspend fun deleteCurrentUser(): AuthConnectionResult = withContext(Dispatchers.Main) {
        val user = activity.firebaseAuthOrNull()?.currentUser
            ?: return@withContext AuthConnectionResult.Failed("현재 연결된 회원 계정이 없습니다.")

        try {
            val email = user.email
            user.delete().await()
            AuthConnectionResult.SignedIn(
                displayName = null,
                email = email ?: "삭제 완료"
            )
        } catch (exception: Exception) {
            AuthConnectionResult.Failed(
                exception.localizedMessage ?: "회원탈퇴에 실패했습니다. 다시 로그인한 뒤 시도해주세요."
            )
        }
    }
}

private fun ComponentActivity.firebaseWebClientId(): String? {
    val resourceId = resources.getIdentifier("default_web_client_id", "string", packageName)
    return if (resourceId == 0) null else getString(resourceId).takeIf { it.isNotBlank() }
}

private fun ComponentActivity.firebaseAuthOrNull(): FirebaseAuth? {
    return runCatching {
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        FirebaseAuth.getInstance()
    }.getOrNull()
}
