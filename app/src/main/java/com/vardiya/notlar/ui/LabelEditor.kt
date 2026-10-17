package com.vardiya.notlar.ui

import com.vardiya.notlar.data.NoteRepository
import com.vardiya.notlar.domain.Labels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The label list lives in the settings and on the notes; a rename or delete changes both in one
 * transaction, so a label never stays half renamed.
 */
class LabelEditor(private val repository: NoteRepository, private val scope: CoroutineScope) {

    /** A label made from a label sheet; it is kept even before a note carries it. */
    fun add(name: String) {
        scope.launch { repository.updateSettings { it.copy(labels = Labels.add(it.labels, name)) } }
    }

    /** Renames a label everywhere; renaming onto another label merges them under its spelling. */
    fun rename(old: String, new: String) {
        scope.launch {
            repository.editLabels { settings, notes ->
                val all = Labels.all(settings.labels, notes)
                val target = all.firstOrNull { Labels.same(it, new) && !Labels.same(it, old) } ?: new
                settings.copy(labels = Labels.renameIn(all, old, target)) to Labels.renameOn(notes, old, target)
            }
        }
    }

    /** Undoes [delete]: the label comes back to the list and to the notes [noteIds] that carried it. */
    fun restore(name: String, noteIds: Set<Long>) {
        scope.launch {
            repository.editLabels { settings, notes ->
                settings.copy(labels = Labels.add(settings.labels, name)) to
                    notes.map { if (it.id in noteIds && it.labels.none { label -> Labels.same(label, name) }) Labels.toggle(it, name) else it }
            }
        }
    }

    /** Deletes a label: it leaves the list and every note, the notes themselves stay. */
    fun delete(name: String) {
        scope.launch {
            repository.editLabels { settings, notes ->
                val all = Labels.all(settings.labels, notes)
                settings.copy(labels = all.filterNot { Labels.same(it, name) }) to Labels.removeFrom(notes, name)
            }
        }
    }
}
