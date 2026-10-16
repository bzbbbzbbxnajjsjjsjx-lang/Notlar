package com.vardiya.notlar.domain

import java.text.Collator
import java.util.Locale

object NoteRules {

    /** Days a note stays in the trash before it is deleted for good. */
    const val TRASH_DAYS = 30
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000
    private val Turkish: Locale = Locale.forLanguageTag("tr-TR")

    /**
     * Notes shown in [section], newest first. The main list puts pinned notes on top and can be
     * narrowed by [filter]; every list can be searched in titles, text and to-do items.
     */
    fun visible(
        notes: List<Note>,
        section: Section,
        query: String = "",
        filter: NoteFilter = NoteFilter.ALL,
        sort: NoteSort = NoteSort.EDITED,
    ): List<Note> {
        val needle = query.trim().lowercase(Turkish)
        return notes
            .filter { note ->
                when (section) {
                    Section.NOTES -> !note.isTrashed && !note.archived && filter.accepts(note)
                    Section.ARCHIVE -> !note.isTrashed && note.archived
                    Section.TRASH -> note.isTrashed
                }
            }
            .filter { needle.isEmpty() || searchText(it).contains(needle) }
            .sortedWith(
                when (section) {
                    Section.TRASH -> compareByDescending { it.trashedAt }
                    else -> compareByDescending<Note> { it.pinned }.then(sort.comparator)
                },
            )
    }

    /** Drops notes that have been in the trash for [TRASH_DAYS] days. */
    fun purgeExpired(notes: List<Note>, nowMillis: Long): List<Note> =
        notes.filterNot { note -> note.trashedAt?.let { nowMillis - it >= TRASH_DAYS * DAY_MILLIS } == true }

    /** Whole days left before a trashed note is deleted (at least 1 while it is still there). */
    fun daysLeft(note: Note, nowMillis: Long): Int {
        val trashedAt = note.trashedAt ?: return TRASH_DAYS
        val left = TRASH_DAYS * DAY_MILLIS - (nowMillis - trashedAt)
        return ((left + DAY_MILLIS - 1) / DAY_MILLIS).toInt().coerceIn(1, TRASH_DAYS)
    }

    /** Turns each non-empty line of the text into an unchecked to-do item. */
    fun toChecklist(note: Note, newId: () -> Long): Note = note.copy(
        isChecklist = true,
        body = "",
        items = note.body.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { ChecklistItem(newId(), it) },
    )

    /** Turns the to-do items back into one line each, checked ones included. */
    fun toText(note: Note): Note = note.copy(
        isChecklist = false,
        body = note.items.map { it.text }.filter { it.isNotBlank() }.joinToString("\n"),
        items = emptyList(),
    )

    /** Plain text for sharing: the title, a blank line, then the text or the to-do list. */
    fun shareText(note: Note): String {
        val content = if (note.isChecklist) {
            note.items.filter { it.text.isNotBlank() }.joinToString("\n") { (if (it.checked) "☑ " else "☐ ") + it.text }
        } else {
            note.body.trim()
        }
        return listOf(note.title.trim(), content).filter { it.isNotEmpty() }.joinToString("\n\n")
    }

    private val NoteSort.comparator: Comparator<Note>
        get() = when (this) {
            NoteSort.EDITED -> compareByDescending { it.updatedAt }
            NoteSort.CREATED -> compareByDescending { it.createdAt }
            NoteSort.TITLE -> compareBy<Note> { it.title.isBlank() }
                .thenBy(Collator.getInstance(Turkish)) { it.title.trim() }
                .thenByDescending { it.updatedAt }
        }

    private fun NoteFilter.accepts(note: Note): Boolean = when (this) {
        NoteFilter.ALL -> true
        NoteFilter.TEXT -> !note.isChecklist
        NoteFilter.CHECKLIST -> note.isChecklist
    }

    private fun searchText(note: Note): String =
        (listOf(note.title, note.body) + note.items.map { it.text }).joinToString("\n").lowercase(Turkish)
}
