package com.vardiya.notlar.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vardiya.notlar.data.NoteRepository
import com.vardiya.notlar.domain.ChecklistItem
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.NoteEdits
import com.vardiya.notlar.domain.NoteRules
import com.vardiya.notlar.domain.Reminders
import com.vardiya.notlar.domain.Settings
import com.vardiya.notlar.reminders.ReminderScheduler
import com.vardiya.notlar.widget.NotlarWidgets
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val WIDGET_REFRESH_DELAY_MS = 1_000L

@OptIn(FlowPreview::class)
class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NoteRepository.get(application)
    private val scheduler = ReminderScheduler(application)

    val notes: StateFlow<List<Note>> =
        repository.notes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Notes are stored but cannot be read on this device (e.g. its Keystore key is gone). */
    val notesUnreadable: StateFlow<Boolean> =
        repository.notesUnreadable.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * The password chosen for the backup being exported, held here so a rotation while the file
     * picker is open does not lose it; never saved anywhere.
     */
    var pendingExportPassword: String? = null

    val settings: StateFlow<Settings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    /** False until the stored notes are loaded, so the list does not flash "empty" on start. */
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    init {
        viewModelScope.launch {
            val stored = repository.notes.first()
            repository.edit { NoteRules.purgeExpired(it, now()) }
            // The editor opened before a restart must see the stored note, not an empty list.
            notes.first { it.isNotEmpty() || stored.isEmpty() }
            _loaded.value = true
        }
        // Alarms follow the stored reminders: set when one is added or moved, cancelled when it goes.
        viewModelScope.launch {
            var scheduled = emptySet<Long>()
            repository.notes
                .distinctUntilChangedBy { notes -> Reminders.scheduled(notes).map { Triple(it.id, it.reminderAt, it.reminderRepeat) } }
                .collect { notes ->
                    val ids = Reminders.scheduled(notes).map { it.id }.toSet()
                    scheduler.sync(notes, goneIds = scheduled - ids)
                    scheduled = ids
                }
        }
        // Home screen widgets show the lists and hide them while the app lock is on.
        viewModelScope.launch {
            combine(repository.notes, repository.settings, repository.lock) { _, _, _ -> }
                .drop(1)
                .debounce(WIDGET_REFRESH_DELAY_MS)
                .collect { NotlarWidgets.refresh(application) }
        }
    }

    /** Adds, renames and deletes labels. */
    val labels = LabelEditor(repository, viewModelScope)

    /** The stored note [id], once the notes are loaded; null if there is no such note. */
    suspend fun find(id: Long): Note? = repository.notes.first().firstOrNull { it.id == id && !it.isTrashed }

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

    /** An empty to-do list with one row to type into; like [newNote] it is kept once something is written. */
    fun newChecklist(): Note = newNote().copy(isChecklist = true, items = listOf(ChecklistItem(1)))

    /** Stores a note made from text shared by another app and returns it. */
    fun createFromShare(text: String, subject: String?): Note {
        val now = now()
        return NoteEdits.fromSharedText(text, subject, now, now).also(::save)
    }

    /** Stores a copy of [note] and returns the copy. */
    fun duplicate(note: Note): Note {
        val now = now()
        return NoteEdits.duplicate(note, now, now).also(::save)
    }

    fun moveToTrash(ids: Set<Long>) = update(ids) { it.copy(trashedAt = now(), pinned = false) }

    fun restore(ids: Set<Long>) = update(ids) { it.copy(trashedAt = null) }

    fun setArchived(ids: Set<Long>, archived: Boolean) = update(ids) { it.copy(archived = archived, pinned = false) }

    /** Undoes a swipe: puts back where [before] was filed (archive, pin, trash), keeping later edits. */
    fun refile(before: Note) = update(setOf(before.id)) {
        it.copy(archived = before.archived, pinned = before.pinned, trashedAt = before.trashedAt)
    }

    fun setPinned(ids: Set<Long>, pinned: Boolean) = update(ids) { it.copy(pinned = pinned) }

    fun rememberSearch(query: String) = updateSettings {
        it.copy(recentSearches = NoteEdits.rememberSearch(it.recentSearches, query))
    }

    fun forgetSearch(query: String) = updateSettings { it.copy(recentSearches = it.recentSearches - query) }

    fun updateSettings(transform: (Settings) -> Settings) {
        viewModelScope.launch { repository.updateSettings(transform) }
    }

    /** Erases notes that cannot be read any more, on the user's explicit choice. */
    fun wipeUnreadableNotes() {
        viewModelScope.launch { repository.wipeNotes() }
    }

    fun exportBackup(): String = repository.encodeBackup(notes.value)

    /** Adds the notes of a backup file; returns how many it held, or null if it is not a backup. */
    suspend fun importBackup(raw: String): Int? {
        val imported = repository.decodeBackup(raw) ?: return null
        repository.edit { NoteEdits.merge(it, imported) }
        return imported.size
    }

    fun deleteForever(id: Long) {
        viewModelScope.launch { repository.edit { list -> list.filterNot { it.id == id } } }
    }

    fun emptyTrash() {
        viewModelScope.launch { repository.edit { list -> list.filterNot { it.isTrashed } } }
    }

    private fun update(ids: Set<Long>, transform: (Note) -> Note) {
        viewModelScope.launch {
            repository.edit { list -> list.map { if (it.id in ids) transform(it) else it } }
        }
    }
}

private fun now() = System.currentTimeMillis()
