package com.vardiya.notlar.domain

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BackupFormatTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(Note.serializer())

    @Test
    fun picturesTravelInsideTheBackupAndStayOutOfStoredNotes() {
        val stored = listOf(Note(1, title = "a", images = listOf("x.jpg"), locked = true))
        assertFalse("embeddedImages" in json.encodeToString(serializer, stored))
        val backup = stored.map { it.copy(embeddedImages = mapOf("x.jpg" to "AAEC")) }
        val read = json.decodeFromString(serializer, json.encodeToString(serializer, backup))
        assertEquals(mapOf("x.jpg" to "AAEC"), read.single().embeddedImages)
        assertEquals(true, read.single().locked)
    }

    @Test
    fun backupsFromBeforePicturesAndLocksStillRead() {
        val old = """[{"id":1,"title":"Eski","body":"metin"}]"""
        val read = json.decodeFromString(serializer, old).single()
        assertEquals(emptyList<String>(), read.images)
        assertFalse(read.locked)
    }
}
