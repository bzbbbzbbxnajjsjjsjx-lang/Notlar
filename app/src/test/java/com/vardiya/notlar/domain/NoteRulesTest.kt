package com.vardiya.notlar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteRulesTest {

    private val day = 24L * 60 * 60 * 1000

    private val notes = listOf(
        Note(1, title = "Market", isChecklist = true, items = listOf(ChecklistItem(1, "Süt"), ChecklistItem(2, "Ekmek", true)), updatedAt = 10),
        Note(2, title = "Vardiya planı", body = "Pazartesi sabah", pinned = true, updatedAt = 5),
        Note(3, title = "Kira", body = "Ayın 5'i", updatedAt = 20),
        Note(4, title = "Eski adres", archived = true, updatedAt = 30),
        Note(5, title = "Toplantı", trashedAt = 100, updatedAt = 40),
    )

    @Test
    fun mainListPutsPinnedFirstThenNewest() {
        assertEquals(listOf(2L, 3L, 1L), NoteRules.visible(notes, Section.NOTES).map { it.id })
    }

    @Test
    fun archiveAndTrashAreSeparate() {
        assertEquals(listOf(4L), NoteRules.visible(notes, Section.ARCHIVE).map { it.id })
        assertEquals(listOf(5L), NoteRules.visible(notes, Section.TRASH).map { it.id })
    }

    @Test
    fun filterByKind() {
        assertEquals(listOf(1L), NoteRules.visible(notes, Section.NOTES, filter = NoteFilter.CHECKLIST).map { it.id })
        assertEquals(listOf(2L, 3L), NoteRules.visible(notes, Section.NOTES, filter = NoteFilter.TEXT).map { it.id })
    }

    @Test
    fun searchIgnoresTurkishCaseAndLooksInItems() {
        assertEquals(listOf(1L), NoteRules.visible(notes, Section.NOTES, query = "SÜT").map { it.id })
        assertEquals(listOf(2L), NoteRules.visible(notes, Section.NOTES, query = "planı").map { it.id })
        assertEquals(listOf(3L), NoteRules.visible(notes, Section.NOTES, query = "KİRA").map { it.id })
    }

    @Test
    fun trashExpiresAfterThirtyDays() {
        val trashed = Note(1, title = "a", trashedAt = 0)
        assertEquals(30, NoteRules.daysLeft(trashed, nowMillis = 0))
        assertEquals(1, NoteRules.daysLeft(trashed, nowMillis = 29 * day + 1))
        assertEquals(1, NoteRules.purgeExpired(listOf(trashed), nowMillis = 29 * day).size)
        assertTrue(NoteRules.purgeExpired(listOf(trashed), nowMillis = 30 * day).isEmpty())
    }

    @Test
    fun textAndChecklistConvertBothWays() {
        var id = 0L
        val list = NoteRules.toChecklist(Note(1, body = "Süt\n\n  Ekmek "), newId = { ++id })
        assertTrue(list.isChecklist)
        assertEquals(listOf("Süt", "Ekmek"), list.items.map { it.text })
        val text = NoteRules.toText(list.copy(items = list.items + ChecklistItem(9, "Yumurta", checked = true)))
        assertFalse(text.isChecklist)
        assertEquals("Süt\nEkmek\nYumurta", text.body)
    }

    @Test
    fun shareTextMarksDoneItems() {
        assertEquals("Market\n\n☐ Süt\n☑ Ekmek", NoteRules.shareText(notes[0]))
        assertEquals("Kira\n\nAyın 5'i", NoteRules.shareText(notes[2]))
    }

    @Test
    fun blankNotesAreDetected() {
        assertTrue(Note(1, items = listOf(ChecklistItem(1, " "))).isBlank)
        assertFalse(Note(1, title = "x").isBlank)
    }
}
