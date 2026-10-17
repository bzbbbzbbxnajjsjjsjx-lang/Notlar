package com.vardiya.notlar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelsTest {

    @Test
    fun namesAreCleanedAndComparedTheTurkishWay() {
        assertEquals("Ev işleri", Labels.clean("  Ev    işleri "))
        assertTrue(Labels.same("İŞ", "iş"))
        assertEquals(listOf("İş"), Labels.add(listOf("İş"), " iş "))
        assertEquals(listOf("İş"), Labels.add(listOf("İş"), "   "))
        assertEquals(Labels.MAX_LENGTH, Labels.clean("a".repeat(50)).length)
    }

    @Test
    fun allKeepsTheSavedOrderAndAddsLabelsOnlyNotesCarry() {
        val notes = listOf(Note(1, labels = listOf("Ev", "Okul")), Note(2, labels = listOf("Çöp"), trashedAt = 1))
        assertEquals(listOf("İş", "Ev", "Okul"), Labels.all(listOf("İş", "Ev"), notes))
    }

    @Test
    fun toggleAddsAndRemoves() {
        val note = Note(1, labels = listOf("İş"))
        assertEquals(listOf("İş", "Ev"), Labels.toggle(note, "Ev").labels)
        assertEquals(emptyList<String>(), Labels.toggle(note, "iş").labels)
    }

    @Test
    fun renameMergesAndDeleteKeepsNotes() {
        val notes = listOf(Note(1, labels = listOf("Is", "Ev")), Note(2, labels = listOf("Ev")))
        assertEquals(listOf(listOf("Ev"), listOf("Ev")), Labels.renameOn(notes, "Is", "ev").map { it.labels })
        assertEquals(listOf("Ev", "Okul"), Labels.renameIn(listOf("Is", "Ev", "Okul"), "Is", "Ev"))
        val removed = Labels.removeFrom(notes, "ev")
        assertEquals(listOf(listOf("Is"), emptyList()), removed.map { it.labels })
        assertEquals(2, removed.size)
    }

    @Test
    fun theMainListFiltersByLabel() {
        val notes = listOf(Note(1, title = "a", labels = listOf("İş")), Note(2, title = "b"), Note(3, title = "c", labels = listOf("iş")))
        assertEquals(listOf(1L, 3L), NoteRules.visible(notes, Section.NOTES, label = "İŞ").map { it.id }.sorted())
        assertEquals(listOf(1L, 3L), NoteRules.visible(notes, Section.NOTES, query = "iş").map { it.id }.sorted())
    }
}
