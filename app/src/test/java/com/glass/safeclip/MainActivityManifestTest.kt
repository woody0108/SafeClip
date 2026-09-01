package com.glass.safeclip

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityManifestTest {
    @Test
    fun mainActivityHandlesRecordingOrientationChangesWithoutRestarting() {
        val document = manifestDocument()
        val activities = document.getElementsByTagName("activity")
        val mainActivity = (0 until activities.length)
            .map { activities.item(it) }
            .first { node ->
                node.attributes.getNamedItemNS(ANDROID_NAMESPACE, "name")?.nodeValue == ".MainActivity"
            }
        val changes = mainActivity.attributes
            .getNamedItemNS(ANDROID_NAMESPACE, "configChanges")
            ?.nodeValue
            .orEmpty()
            .split('|')
            .toSet()

        assertTrue("orientation must be handled", "orientation" in changes)
        assertTrue("screenSize must be handled", "screenSize" in changes)
    }

    @Test
    fun declaresVersionAppropriateSharedMediaReadPermissions() {
        val permissions = manifestDocument().getElementsByTagName("uses-permission")
        val byName = (0 until permissions.length)
            .map { permissions.item(it) }
            .associateBy { node ->
                node.attributes.getNamedItemNS(ANDROID_NAMESPACE, "name")?.nodeValue
            }

        assertTrue("Android 13+ image permission missing", "android.permission.READ_MEDIA_IMAGES" in byName)
        assertTrue("Android 13+ video permission missing", "android.permission.READ_MEDIA_VIDEO" in byName)
        assertTrue(
            "Android 14+ selected media permission missing",
            "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" in byName
        )
        assertEquals(
            "32",
            byName.getValue("android.permission.READ_EXTERNAL_STORAGE")
                .attributes
                .getNamedItemNS(ANDROID_NAMESPACE, "maxSdkVersion")
                ?.nodeValue
        )
    }

    private fun manifestDocument() = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
