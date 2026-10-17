package com.vardiya.notlar.domain

import java.util.Locale

/** Labels are plain names; two names that differ only in case or spacing are the same label. */
object Labels {

    private val Turkish: Locale = Locale.forLanguageTag("tr-TR")
    private val Spaces = Regex("\\s+")

    /** Longest label name; keeps chips on one line. */
    const val MAX_LENGTH = 30

    /** The name as stored: trimmed, inner runs of spaces made one, cut to [MAX_LENGTH]. */
    fun clean(name: String): String = name.trim().replace(Spaces, " ").take(MAX_LENGTH).trim()

    fun same(a: String, b: String): Boolean = clean(a).lowercase(Turkish) == clean(b).lowercase(Turkish)

    /**
     * Every label to offer: the user's list in its own order, then any carried by a note but
     * missing from it (for example, from an imported backup).
     */
    fun all(saved: List<String>, notes: List<Note>): List<String> =
        (saved + notes.filterNot { it.isTrashed }.flatMap { it.labels }).fold(emptyList()) { acc, name -> add(acc, name) }

    /** [labels] with [name] at the end, unless it is blank or already there. */
    fun add(labels: List<String>, name: String): List<String> {
        val cleaned = clean(name)
        return if (cleaned.isEmpty() || labels.any { same(it, cleaned) }) labels else labels + cleaned
    }

    /** Puts [name] on [note], or takes it off if it is already there. */
    fun toggle(note: Note, name: String): Note =
        if (note.labels.any { same(it, name) }) {
            note.copy(labels = note.labels.filterNot { same(it, name) })
        } else {
            note.copy(labels = add(note.labels, name))
        }

    /**
     * Renames a label in the list. Renaming onto another existing label merges the two under that
     * label's own spelling.
     */
    fun renameIn(labels: List<String>, old: String, new: String): List<String> {
        val cleaned = clean(new)
        if (cleaned.isEmpty()) return labels
        val target = labels.firstOrNull { same(it, cleaned) && !same(it, old) } ?: cleaned
        return labels.map { if (same(it, old)) target else it }.fold(emptyList()) { acc, name -> add(acc, name) }
    }

    /** Renames a label on every note that carries it. */
    fun renameOn(notes: List<Note>, old: String, new: String): List<Note> = notes.map { note ->
        if (note.labels.any { same(it, old) }) note.copy(labels = renameIn(note.labels, old, new)) else note
    }

    /** Takes a label off every note; the notes themselves stay. */
    fun removeFrom(notes: List<Note>, name: String): List<Note> = notes.map { note ->
        if (note.labels.any { same(it, name) }) note.copy(labels = note.labels.filterNot { same(it, name) }) else note
    }
}
