package com.vardiya.notlar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vardiya.notlar.domain.LockState
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "notlar")

/**
 * Persists all notes as one JSON list, the settings as one JSON object and the app lock, in
 * Preferences DataStore. Every value is encrypted with a Keystore key ([KeystoreCipher]) bound to
 * its own name; values from before encryption are read as they are and encrypted at start.
 *
 * Nothing that is stored but unreadable (key lost or invalidated, data damaged) is ever overwritten
 * by an edit: the notes report [notesUnreadable] and the lock reports itself unreadable, which keeps
 * the app locked rather than open.
 */
class NoteRepository private constructor(private val store: DataStore<Preferences>, private val cipher: KeystoreCipher) {

    private val serializer = ListSerializer(Note.serializer())

    val notes: Flow<List<Note>> = store.data.map { prefs -> readNotes(prefs).orEmpty() }

    /** True while notes are stored but cannot be read. */
    val notesUnreadable: Flow<Boolean> = store.data.map { prefs -> prefs[NOTES] != null && readNotes(prefs) == null }

    val settings: Flow<Settings> = store.data.map { prefs ->
        read(prefs, SETTINGS)?.let { runCatching { json.decodeFromString(Settings.serializer(), it) }.getOrNull() } ?: Settings()
    }

    val lock: Flow<LockState> = store.data.map(::readLock)

    /** Applies [transform] to the stored list in one transaction; skipped while the notes are unreadable. */
    suspend fun edit(transform: (List<Note>) -> List<Note>) {
        store.edit { prefs ->
            val current = if (prefs[NOTES] == null) emptyList() else readNotes(prefs) ?: return@edit
            prefs[NOTES] = cipher.encrypt(json.encodeToString(serializer, transform(current)), NOTES.name)
        }
    }

    suspend fun saveSettings(settings: Settings) {
        store.edit { prefs -> prefs[SETTINGS] = cipher.encrypt(json.encodeToString(Settings.serializer(), settings), SETTINGS.name) }
    }

    suspend fun updateSettings(transform: (Settings) -> Settings) {
        store.edit { prefs ->
            val current = read(prefs, SETTINGS)?.let { runCatching { json.decodeFromString(Settings.serializer(), it) }.getOrNull() }
            prefs[SETTINGS] = cipher.encrypt(json.encodeToString(Settings.serializer(), transform(current ?: Settings())), SETTINGS.name)
        }
    }

    /**
     * Applies [transform] to the lock in one transaction. An unreadable lock is handed over as
     * unreadable, so only a deliberate reset (a new PIN after the device's own lock) replaces it.
     */
    suspend fun editLock(transform: (LockState) -> LockState) {
        store.edit { prefs ->
            val next = transform(readLock(prefs))
            if (!next.unreadable) prefs[LOCK] = cipher.encrypt(json.encodeToString(LockState.serializer(), next), LOCK.name)
        }
    }

    /** Erases the unreadable notes for good, on the user's explicit choice. */
    suspend fun wipeNotes() {
        store.edit { prefs -> prefs.remove(NOTES) }
    }

    /** Encrypts values written before encryption existed; run once at start. */
    suspend fun encryptLegacyValues() {
        store.edit { prefs ->
            listOf(NOTES, SETTINGS, LOCK).forEach { key ->
                prefs[key]?.takeUnless(cipher::isEncrypted)?.let { prefs[key] = cipher.encrypt(it, key.name) }
            }
        }
    }

    /** All notes, trash included, as the JSON written to a backup file. */
    fun encodeBackup(notes: List<Note>): String = backupJson.encodeToString(serializer, notes)

    /** Notes read from a backup file; null if it is not a Notlar backup. */
    fun decodeBackup(raw: String): List<Note>? = runCatching { json.decodeFromString(serializer, raw) }.getOrNull()

    /** The stored notes; null if there are none or they cannot be decrypted or parsed. */
    private fun readNotes(prefs: Preferences): List<Note>? = read(prefs, NOTES)?.let(::decodeBackup)

    private fun readLock(prefs: Preferences): LockState {
        if (prefs[LOCK] == null) return LockState()
        return read(prefs, LOCK)?.let { runCatching { json.decodeFromString(LockState.serializer(), it) }.getOrNull() }
            ?: LockState(unreadable = true)
    }

    /** The plain text of a stored value; null if it is missing or cannot be decrypted. */
    private fun read(prefs: Preferences, key: Preferences.Key<String>): String? =
        prefs[key]?.let { runCatching { cipher.decrypt(it, key.name) }.getOrNull() }

    companion object {
        private val NOTES = stringPreferencesKey("notes")
        private val SETTINGS = stringPreferencesKey("settings")
        private val LOCK = stringPreferencesKey("lock")
        private val json = Json { ignoreUnknownKeys = true }
        private val backupJson = Json(json) { prettyPrint = true }

        @Volatile private var instance: NoteRepository? = null

        /** One repository, and one cipher, for the whole app. */
        fun get(context: Context): NoteRepository = instance ?: synchronized(this) {
            // Only the DataStore is kept, not a Context, so the shared instance cannot leak one.
            instance ?: NoteRepository(context.applicationContext.dataStore, KeystoreCipher()).also { instance = it }
        }
    }
}
