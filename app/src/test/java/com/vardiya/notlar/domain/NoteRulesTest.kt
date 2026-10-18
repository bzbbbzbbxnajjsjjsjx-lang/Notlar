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

    @Test
    fun sortKeepsPinnedOnTop() {
        val list = listOf(
            Note(1, title = "Çay", createdAt = 3, updatedAt = 1),
            Note(2, title = "ayva", createdAt = 1, updatedAt = 3),
            Note(3, title = "", createdAt = 2, updatedAt = 2),
            Note(4, title = "Zil", pinned = true, createdAt = 0, updatedAt = 0),
        )
        assertEquals(listOf(4L, 2L, 3L, 1L), NoteRules.visible(list, Section.NOTES, sort = NoteSort.EDITED).map { it.id })
        assertEquals(listOf(4L, 1L, 3L, 2L), NoteRules.visible(list, Section.NOTES, sort = NoteSort.CREATED).map { it.id })
        // Turkish alphabet order (Ç after C), untitled notes last.
        assertEquals(listOf(4L, 2L, 1L, 3L), NoteRules.visible(list, Section.NOTES, sort = NoteSort.TITLE).map { it.id })
    }

    @Test
    fun progressCountsNonBlankItems() {
        assertEquals(1 to 2, NoteEdits.progress(notes[0]))
        assertEquals(null, NoteEdits.progress(notes[1]))
        assertEquals(null, NoteEdits.progress(Note(1, isChecklist = true, items = listOf(ChecklistItem(1, " ")))))
    }

    @Test
    fun wordCountCoversTitleAndContent() {
        assertEquals(4, NoteEdits.wordCount(notes[1]))
        assertEquals(3, NoteEdits.wordCount(notes[0]))
        assertEquals(0, NoteEdits.wordCount(Note(1, body = "  \n ")))
    }

    @Test
    fun duplicateIsAnUnpinnedCopy() {
        val copy = NoteEdits.duplicate(notes[1], id = 99, nowMillis = 50)
        assertEquals(99L, copy.id)
        assertFalse(copy.pinned)
        assertEquals(50L, copy.updatedAt)
        assertTrue(copy.sameContentAs(notes[1].copy(id = 99, pinned = false)))
    }

    @Test
    fun checklistBulkActions() {
        assertTrue(NoteEdits.uncheckAll(notes[0]).items.none { it.checked })
        assertEquals(listOf("Süt"), NoteEdits.removeChecked(notes[0]).items.map { it.text })
    }

    @Test
    fun sharedTextBecomesANote() {
        val note = NoteEdits.fromSharedText("  https://ornek.com \n", "Bağlantı", id = 7, nowMillis = 7)
        assertEquals("Bağlantı", note.title)
        assertEquals("https://ornek.com", note.body)
        assertEquals("", NoteEdits.fromSharedText("x", null, 1, 1).title)
    }

    @Test
    fun mergeKeepsNewestVersionOfEachNote() {
        val merged = NoteEdits.merge(
            current = listOf(Note(1, title = "eski", updatedAt = 1), Note(2, title = "yerel", updatedAt = 9)),
            imported = listOf(Note(1, title = "yeni", updatedAt = 5), Note(2, title = "yedek", updatedAt = 3), Note(3, title = "ek")),
        ).associateBy { it.id }
        assertEquals("yeni", merged.getValue(1).title)
        assertEquals("yerel", merged.getValue(2).title)
        assertEquals("ek", merged.getValue(3).title)
    }

    @Test
    fun matchRangesFindEveryOccurrence() {
        assertEquals(listOf(0..2, 7..9), NoteEdits.matchRanges("Süt ve süt", "SÜT"))
        assertEquals(listOf(5..8), NoteEdits.matchRanges("Ayın kira günü", " KİRA "))
        assertTrue(NoteEdits.matchRanges("Süt", "  ").isEmpty())
        // Turkish: "kira" is not "KIRA" (kıra), but "İ" is the capital of "i".
        assertTrue(NoteEdits.matchRanges("KIRA", "kira").isEmpty())
        assertEquals(listOf(0..3), NoteEdits.matchRanges("KİRA", "kira"))
    }

    @Test
    fun markdownUsesHeadingAndTaskList() {
        assertEquals("# Market\n\n- [ ] Süt\n- [x] Ekmek", NoteEdits.markdown(notes[0]))
        assertEquals("# Kira\n\nAyın 5'i", NoteEdits.markdown(notes[2]))
        assertEquals("Sadece metin", NoteEdits.markdown(Note(1, body = " Sadece metin ")))
        assertEquals("# başlık\n- madde\n**kalın**", NoteEdits.markdown(Note(1, body = "# başlık\n- madde\n**kalın**")))
        assertEquals("", NoteEdits.markdown(Note(1)))
    }

    @Test
    fun recentSearchesKeepNewestFirstWithoutDuplicates() {
        val recent = NoteEdits.rememberSearch(listOf("market", "kira"), " KİRA ")
        assertEquals(listOf("KİRA", "market"), recent)
        assertEquals(recent, NoteEdits.rememberSearch(recent, "  "))
        val full = (1..12).fold(emptyList<String>()) { list, i -> NoteEdits.rememberSearch(list, "a$i") }
        assertEquals(NoteEdits.MAX_RECENT_SEARCHES, full.size)
        assertEquals("a12", full.first())
    }

    @Test
    fun manualOrderFollowsTheDraggedOrderAndKeepsPinnedOnTop() {
        val list = listOf(
            Note(1, title = "a", updatedAt = 1),
            Note(2, title = "b", updatedAt = 2),
            Note(3, title = "c", updatedAt = 3),
            Note(4, title = "d", pinned = true, updatedAt = 4),
            Note(5, title = "e", updatedAt = 9),
        )
        // 5 is not in the order yet, so it comes first among the unpinned notes.
        val shown = NoteRules.visible(list, Section.NOTES, sort = NoteSort.MANUAL, manualOrder = listOf(3, 1, 2)).map { it.id }
        assertEquals(listOf(4L, 5L, 3L, 1L, 2L), shown)
        // The archive ignores the manual order.
        val archived = list.map { it.copy(archived = true, pinned = false) }
        val archive = NoteRules.visible(archived, Section.ARCHIVE, sort = NoteSort.MANUAL, manualOrder = listOf(3, 1, 2))
        assertEquals(listOf(5L, 4L, 3L, 2L, 1L), archive.map { it.id })
    }

    @Test
    fun reorderMovesOneNoteAndKeepsTheHiddenOnes() {
        assertEquals(listOf(3L, 1L, 2L, 9L), NoteRules.reorder(listOf(1, 2, 3, 9), listOf(1, 2, 3), from = 2, to = 0))
        assertEquals(listOf(1L, 2L), NoteRules.reorder(emptyList(), listOf(1, 2), from = 0, to = 0))
        assertEquals(listOf(2L, 1L), NoteRules.reorder(emptyList(), listOf(1, 2), from = 0, to = 1))
    }

    @Test
    fun imagesCountAsContentAndShareTextDropsMarkdownMarks() {
        assertFalse(Note(1, images = listOf("a.jpg")).isBlank)
        assertEquals("Başlık\n\nSelam **dünya**".replace("**", ""), NoteRules.shareText(Note(1, title = "Başlık", body = "Selam **dünya**")))
    }
}
