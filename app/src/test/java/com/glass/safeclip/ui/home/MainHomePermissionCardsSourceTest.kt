package com.glass.safeclip.ui.home

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class MainHomePermissionCardsSourceTest {
    @Test
    fun homeScreenDoesNotRenderFolderOrCameraPermissionCards() {
        val source = File("src/main/java/com/glass/safeclip/ui/home/MainHomeScreen.kt").readText()

        assertFalse(source.contains("PermissionStatusTile("))
        assertFalse(source.contains("label = \"폴더 권한\""))
        assertFalse(source.contains("label = \"카메라 권한\""))
    }
}
