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
import com.google.firebase.auth.FirebaseUser
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

        try {
            val googleCredential = activity.requestGoogleCredential(webClientId)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
            user?.toSignedInResult(providerFallback = "google")
                ?: AuthConnectionResult.Failed("Google 로그인 사용자 정보를 읽지 못했습니다.")
        } catch (exception: GetCredentialException) {
            AuthConnectionResult.Failed("Google 계정 선택이 취소되었거나 실패했습니다.")
        } catch (exception: GoogleIdTokenParsingException) {
            AuthConnectionResult.Failed("Google 로그인 토큰을 읽지 못했습니다.")
        } catch (exception: Exception) {
            AuthConnectionResult.Failed(exception.localizedMessage ?: "Google 로그인에 실패했습니다.")
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): AuthConnectionResult = withContext(Dispatchers.Main) {
        val input = EmailAuthInput.create(email = email, password = password)
        input.errorMessage?.let { message ->
            return@withContext AuthConnectionResult.Failed(message)
        }

        val firebaseAuth = activity.firebaseAuthOrNull()
            ?: return@withContext AuthConnectionResult.NeedsFirebaseSetup(
                "Firebase 설정이 아직 없습니다. google-services.json을 app 폴더에 넣으면 이메일 가입을 연결할 수 있어요."
            )

        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(input.email, input.password).await()
            val user = authResult.user
            user?.toSignedInResult(providerFallback = "email")
                ?: AuthConnectionResult.Failed("이메일 가입 사용자 정보를 읽지 못했습니다.")
        } catch (exception: Exception) {
            AuthConnectionResult.Failed(exception.localizedMessage ?: "이메일 가입에 실패했습니다.")
        }
    }

    fun currentSignedInUser(): AuthConnectionResult.SignedIn? {
        return activity.firebaseAuthOrNull()?.currentUser?.toSignedInResult()
    }

    suspend fun reauthenticateCurrentUser(): AuthConnectionResult = withContext(Dispatchers.Main) {
        val user = activity.firebaseAuthOrNull()?.currentUser
            ?: return@withContext AuthConnectionResult.Failed("현재 연결된 회원 계정이 없습니다.")

        try {
            user.reauthenticateIfGoogle()
            user.toSignedInResult()
        } catch (exception: Exception) {
            AuthConnectionResult.Failed(
                exception.localizedMessage ?: "재인증에 실패했습니다. 다시 로그인한 뒤 시도해주세요."
            )
        }
    }

    fun currentUserEmail(): String? {
        return activity.firebaseAuthOrNull()?.currentUser?.email
    }

    fun currentUserUid(): String? {
        return activity.firebaseAuthOrNull()?.currentUser?.uid
    }

    fun signOut(): String {
        val firebaseAuth = activity.firebaseAuthOrNull()
            ?: return "Firebase 설정을 확인해주세요."
        firebaseAuth.signOut()
        return "로그아웃되었습니다. Guest ID는 그대로 유지됩니다."
    }

    suspend fun deleteCurrentUser(): AuthConnectionResult = withContext(Dispatchers.Main) {
        val user = activity.firebaseAuthOrNull()?.currentUser
            ?: return@withContext AuthConnectionResult.Failed("현재 연결된 회원 계정이 없습니다.")

        try {
            val email = user.email
            user.delete().await()
            AuthConnectionResult.SignedIn(
                uid = user.uid,
                displayName = null,
                email = email ?: "삭제 완료",
                provider = "deleted"
            )
        } catch (exception: Exception) {
            AuthConnectionResult.Failed(
                if (exception.localizedMessage?.contains("requires recent", ignoreCase = true) == true) {
                    "회원탈퇴는 보안을 위해 최근 로그인이 필요합니다. Google 계정은 다시 로그인 창을 거친 뒤 탈퇴되고, 이메일 계정은 다시 로그인 후 시도해주세요."
                } else {
                    exception.localizedMessage ?: "회원탈퇴에 실패했습니다. 다시 로그인한 뒤 시도해주세요."
                }
            )
        }
    }

    private suspend fun FirebaseUser.reauthenticateIfGoogle() {
        val usesGoogle = providerData.any { it.providerId == "google.com" }
        if (!usesGoogle) return

        val webClientId = activity.firebaseWebClientId() ?: return
        val googleCredential = activity.requestGoogleCredential(webClientId)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
        reauthenticate(firebaseCredential).await()
    }
}

private suspend fun ComponentActivity.requestGoogleCredential(webClientId: String): GoogleIdTokenCredential {
    val credentialManager = CredentialManager.create(this)
    val googleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(webClientId)
        .setFilterByAuthorizedAccounts(false)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    val result = credentialManager.getCredential(this, request)
    return GoogleIdTokenCredential.createFrom(result.credential.data)
}

private fun FirebaseUser.toSignedInResult(providerFallback: String = "unknown"): AuthConnectionResult.SignedIn {
    return AuthConnectionResult.SignedIn(
        uid = uid,
        displayName = displayName,
        email = email,
        provider = providerData.firstOrNull { it.providerId != "firebase" }?.providerId
            ?.removeSuffix(".com")
            ?: providerFallback
    )
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
