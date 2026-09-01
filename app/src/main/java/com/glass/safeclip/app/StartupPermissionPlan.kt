package com.glass.safeclip.app

object StartupPermissionPlan {
    @Suppress("UNUSED_PARAMETER")
    fun missingPermissions(
        sdkInt: Int,
        isGranted: (String) -> Boolean
    ): List<String> = emptyList()
}
