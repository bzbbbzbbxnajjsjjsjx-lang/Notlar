package com.vardiya.notlar.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vardiya.notlar.data.NoteRepository
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.NoteRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NoteRepository(application)

    val notes: StateFlow<List<Note>> =
        repository.notes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** False until the stored notes are loaded, so the list does not flash "empty" on start. */
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    init {
        viewModelScope.launch {
            repository.notes.first()
            repository.edit { NoteRules.purgeExpired(it, now()) }
            _loaded.value = true
        }
    }

    fun newNote(): Note {
        val now = now()
        return Note(id = now, createdAt = now, updatedAt = now)
    }

    /** Stores [draft]; a blank note is removed instead of kept. Unchanged content keeps its time. */
    fun save(draft: Note) {
        viewModelScope.launch {
            repository.edit { list ->
                val existing = list.firstOrNull { it.id == draft.id }
                when {
                    draft.isBlank -> list.filterNot { it.id == draft.id }
                    existing == null -> list + draft.copy(updatedAt = now())
                    existing.sameContentAs(draft) -> list
                    else -> list.map { if (it.id == draft.id) draft.copy(updatedAt = now()) else it }
                }
            }
        }
    }

    fun moveToTrash(id: Long) = update(id) { it.copy(trashedAt = now(), pinned = false) }

    fun restore(id: Long) = update(id) { it.copy(trashedAt = null) }

    fun setArchived(id: Long, archived: Boolean) = update(id) { it.copy(archived = archived, pinned = false) }

    fun deleteForever(id: Long) {
        viewModelScope.launch { repository.edit { list -> list.filterNot { it.id == id } } }
    }

    fun emptyTrash() {
        viewModelScope.launch { repository.edit { list -> list.filterNot { it.isTrashed } } }
    }

    private fun update(id: Long, transform: (Note) -> Note) {
        viewModelScope.launch {
            repository.edit { list -> list.map { if (it.id == id) transform(it) else it } }
        }
    }

    private fun now() = System.currentTimeMillis()
}
