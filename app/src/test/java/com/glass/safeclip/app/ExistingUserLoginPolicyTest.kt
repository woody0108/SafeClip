package com.glass.safeclip.app

import com.glass.safeclip.data.profile.UserProfile
import com.glass.safeclip.data.profile.UserProfileSyncResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExistingUserLoginPolicyTest {
    @Test
    fun allowsLoginWhenUsersDocumentExists() {
        val profile = UserProfile(
            uid = "uid-123",
            guestId = "",
            email = "safeclip@example.com",
            displayName = "SafeClip",
            provider = "google"
        )

        val result = ExistingUserLoginPolicy.evaluate(
            UserProfileSyncResult.Success(profile, "Firestore 사용자 정보를 불러왔습니다.")
        )

        assertTrue(result is ExistingUserLoginResult.Allowed)
        assertEquals(profile, (result as ExistingUserLoginResult.Allowed).profile)
    }

    @Test
    fun rejectsLoginWhenUsersDocumentIsMissing() {
        val result = ExistingUserLoginPolicy.evaluate(
            UserProfileSyncResult.Failed("Firestore 사용자 문서를 찾지 못했습니다.")
        )

        assertTrue(result is ExistingUserLoginResult.Rejected)
        assertEquals(
            "가입된 계정을 찾지 못했습니다. 먼저 회원가입을 진행해주세요.",
            (result as ExistingUserLoginResult.Rejected).message
        )
    }

    @Test
    fun keepsFirestoreFailureMessageWhenLoadFailsForAnotherReason() {
        val result = ExistingUserLoginPolicy.evaluate(
            UserProfileSyncResult.Failed("Firestore 권한이 없습니다.")
        )

        assertTrue(result is ExistingUserLoginResult.Rejected)
        assertEquals("Firestore 권한이 없습니다.", (result as ExistingUserLoginResult.Rejected).message)
    }
}
