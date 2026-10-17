package com.vardiya.notlar.domain

/** Changes to and facts about single notes: checklist progress, copies, shares and backups. */
object NoteEdits {

    private val whitespace = Regex("\\s+")

    /** Done and total to-do items, blank rows left out; null for a text note or an empty list. */
    fun progress(note: Note): Pair<Int, Int>? {
        if (!note.isChecklist) return null
        val items = note.items.filter { it.text.isNotBlank() }
        return if (items.isEmpty()) null else items.count { it.checked } to items.size
    }

    /** Words in the title and the text or to-do items. */
    fun wordCount(note: Note): Int =
        (listOf(note.title, note.body) + note.items.map { it.text })
            .sumOf { text -> text.split(whitespace).count { it.isNotEmpty() } }

    /** A new, unpinned note with the same content and color, its to-do items given fresh ids. */
    fun duplicate(note: Note, id: Long, nowMillis: Long): Note = note.copy(
        id = id,
        items = note.items.mapIndexed { index, item -> item.copy(id = index + 1L) },
        pinned = false,
        trashedAt = null,
        createdAt = nowMillis,
        updatedAt = nowMillis,
    )

    fun uncheckAll(note: Note): Note = note.copy(items = note.items.map { it.copy(checked = false) })

    fun removeChecked(note: Note): Note = note.copy(items = note.items.filterNot { it.checked })

    /** A note made from text shared by another app; the share subject, if any, becomes the title. */
    fun fromSharedText(text: String, subject: String?, id: Long, nowMillis: Long): Note =
        Note(id = id, title = subject?.trim().orEmpty(), body = text.trim(), createdAt = nowMillis, updatedAt = nowMillis)

    /**
     * Adds a backup to the stored notes. A note with an id that is already there is kept in its
     * most recently edited version.
     */
    fun merge(current: List<Note>, imported: List<Note>): List<Note> {
        val byId = current.associateBy { it.id }.toMutableMap()
        imported.forEach { note ->
            val existing = byId[note.id]
            if (existing == null || note.updatedAt > existing.updatedAt) byId[note.id] = note
        }
        return byId.values.toList()
    }

    /** Where [query] occurs in [text], ignoring case; empty for a blank query. */
    fun matchRanges(text: String, query: String): List<IntRange> {
        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()
        val ranges = mutableListOf<IntRange>()
        var from = text.indexOf(needle, ignoreCase = true)
        while (from >= 0) {
            ranges += from until from + needle.length
            from = text.indexOf(needle, from + needle.length, ignoreCase = true)
        }
        return ranges
    }
}
