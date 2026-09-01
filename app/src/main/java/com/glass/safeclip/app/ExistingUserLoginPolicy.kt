package com.glass.safeclip.app

import com.glass.safeclip.data.profile.UserProfile
import com.glass.safeclip.data.profile.UserProfileSyncResult

sealed interface ExistingUserLoginResult {
    data class Allowed(val profile: UserProfile) : ExistingUserLoginResult
    data class Rejected(val message: String) : ExistingUserLoginResult
}

object ExistingUserLoginPolicy {
    fun evaluate(result: UserProfileSyncResult): ExistingUserLoginResult {
        return when (result) {
            is UserProfileSyncResult.Success -> ExistingUserLoginResult.Allowed(result.profile)
            is UserProfileSyncResult.Failed -> ExistingUserLoginResult.Rejected(result.loginFailureMessage())
        }
    }

    private fun UserProfileSyncResult.Failed.loginFailureMessage(): String {
        return if (message.contains("문서를 찾지 못")) {
            "가입된 계정을 찾지 못했습니다. 먼저 회원가입을 진행해주세요."
        } else {
            message
        }
    }
}
