package com.glass.safeclip.ui.onboarding

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartFlowSourceTest {
    @Test
    fun startScreenOnlyKeepsLoginAndStartActions() {
        val source = startScreenSource()

        assertFalse(source.contains("showSignUpDialog"))
        assertFalse(source.contains("ConnectionStatusBox("))
        assertFalse(source.contains("GuestIdentityRow("))
        assertFalse(source.contains("AccountActionRow("))
        assertTrue(source.contains("linkedAccountId?.let"))
        assertTrue(source.contains("LinkedAccountIdRow("))
        assertTrue(source.contains("text = if (hasLinkedAccount) \"로그인됨\" else \"로그인\""))
        assertTrue(source.contains("text = \"시작하기\""))
    }

    @Test
    fun brandBlockIsCenteredAndShiftedSlightlyUp() {
        val source = startScreenSource()

        assertTrue(source.contains(".align(Alignment.Center)"))
        assertTrue(source.contains(".offset(y = StartScreenLayout.BrandVerticalOffset)"))
    }

    @Test
    fun startButtonLoadsHomeWithoutRequestingPermissions() {
        val activitySource = File("src/main/java/com/glass/safeclip/MainActivity.kt").readText()
        val startRoute = activitySource
            .substringAfter("SafeClipScreen.Start -> StartScreen(")
            .substringBefore("SafeClipScreen.Connecting ->")

        assertTrue(startRoute.contains("onStart = ::continueStartupToHome"))
        assertFalse(startRoute.contains("StartupPermissionPlan.missingPermissions"))
        assertFalse(startRoute.contains("startupPermissionLauncher.launch"))
        assertFalse(activitySource.contains("val startupPermissionLauncher"))
    }

    private fun startScreenSource(): String =
        File("src/main/java/com/glass/safeclip/ui/onboarding/StartScreen.kt")
            .readText()
            .substringAfter("fun StartScreen(")
            .substringBefore("fun ConnectingScreen()")
}
