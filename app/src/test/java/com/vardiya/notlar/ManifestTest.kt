package com.vardiya.notlar

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element

/**
 * Notlar is one offline app. A second launcher entry or a new permission (such as INTERNET) has
 * shipped by accident before, so adding one must be a deliberate edit of this test.
 */
class ManifestTest {

    private val android = "http://schemas.android.com/apk/res/android"

    // Unit tests run with the module directory as the working directory.
    private val manifest = DocumentBuilderFactory.newInstance()
        .apply { isNamespaceAware = true }
        .newDocumentBuilder()
        .parse(File("src/main/AndroidManifest.xml"))

    private fun elements(tag: String): List<Element> {
        val nodes = manifest.getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    @Test
    fun onlyMainActivityIsLaunchable() {
        // An activity-alias with the launcher category adds an icon as well.
        val launchers = (elements("activity") + elements("activity-alias")).filter { activity ->
            val categories = activity.getElementsByTagName("category")
            (0 until categories.length).any {
                (categories.item(it) as Element).getAttributeNS(android, "name") == "android.intent.category.LAUNCHER"
            }
        }
        assertEquals(listOf(".MainActivity"), launchers.map { it.getAttributeNS(android, "name") })
    }

    @Test
    fun permissionsAreTheExpectedOnes() {
        val permissions = elements("uses-permission").map { it.getAttributeNS(android, "name") }.sorted()
        assertEquals(
            listOf(
                "android.permission.POST_NOTIFICATIONS",
                "android.permission.RECEIVE_BOOT_COMPLETED",
                "android.permission.SCHEDULE_EXACT_ALARM",
            ),
            permissions,
        )
    }
}
