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
     * narrowed by [filter] and [label]; every list can be searched in titles, text, to-do items
     * and labels.
     */
    fun visible(
        notes: List<Note>,
        section: Section,
        query: String = "",
        filter: NoteFilter = NoteFilter.ALL,
        sort: NoteSort = NoteSort.EDITED,
        label: String? = null,
        manualOrder: List<Long> = emptyList(),
    ): List<Note> {
        val needle = fold(query.trim().lowercase(Turkish))
        return notes
            .filter { note ->
                when (section) {
                    Section.NOTES -> !note.isTrashed && !note.archived && filter.accepts(note) &&
                        (label == null || note.labels.any { Labels.same(it, label) })
                    Section.ARCHIVE -> !note.isTrashed && note.archived
                    Section.TRASH -> note.isTrashed
                }
            }
            .filter { needle.isEmpty() || searchText(it).contains(needle) }
            .sortedWith(
                when (section) {
                    Section.TRASH -> compareByDescending { it.trashedAt }
                    // The archive has no manual order, it falls back to the last edit.
                    else -> compareByDescending<Note> { it.pinned }
                        .then(if (sort == NoteSort.MANUAL) manualComparator(manualOrder, section) else sort.comparator)
                },
            )
    }

    /**
     * [order] with the note at [from] moved to [to] among [shownIds], the ids of the notes in the
     * order they are shown. Ids in [order] that are not shown keep their place at the end.
     */
    fun reorder(order: List<Long>, shownIds: List<Long>, from: Int, to: Int): List<Long> {
        if (from !in shownIds.indices || to !in shownIds.indices || from == to) return order.ifEmpty { shownIds }
        val moved = shownIds.toMutableList().apply { add(to, removeAt(from)) }
        return moved + order.filterNot { it in moved }
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
            Markdown.strip(note.body.trim())
        }
        return listOf(note.title.trim(), content).filter { it.isNotEmpty() }.joinToString("\n\n")
    }

    /** Notes in [order] first by position, then notes the order does not know yet, newest edit first. */
    private fun manualComparator(order: List<Long>, section: Section): Comparator<Note> {
        if (section != Section.NOTES) return NoteSort.EDITED.comparator
        val position = order.withIndex().associate { it.value to it.index }
        return compareBy<Note> { position[it.id] != null }.thenBy { position[it.id] ?: 0 }.thenByDescending { it.updatedAt }
    }

    private val NoteSort.comparator: Comparator<Note>
        get() = when (this) {
            NoteSort.MANUAL -> compareByDescending { it.updatedAt }
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

    /** What a search looks in: nothing for a locked note, and letters without their Turkish marks (ç ğ ö ş ü). */
    private fun searchText(note: Note): String =
        if (note.locked) {
            ""
        } else {
            fold((listOf(note.title, note.body) + note.items.map { it.text } + note.labels).joinToString("\n").lowercase(Turkish))
        }

    /** Lower-case Turkish text with ç, ğ, ö, ş and ü as c, g, o, s and u, so "sut" finds "süt"; ı stays apart from i, and the length stays. */
    fun fold(lowercased: String): String = lowercased.map { FOLDS[it] ?: it }.joinToString("")

    private val FOLDS = mapOf('ç' to 'c', 'ğ' to 'g', 'ö' to 'o', 'ş' to 's', 'ü' to 'u')
}
