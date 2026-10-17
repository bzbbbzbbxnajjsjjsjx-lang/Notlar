package com.vardiya.notlar.data

import android.content.Context
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
 * Preferences DataStore. Every value is encrypted with a Keystore key ([KeystoreCipher]); values
 * from before encryption are read as they are and encrypted on the next write.
 */
class NoteRepository(private val context: Context, private val cipher: KeystoreCipher = KeystoreCipher()) {

    private val serializer = ListSerializer(Note.serializer())

    val notes: Flow<List<Note>> = context.dataStore.data.map { prefs -> decode(prefs[NOTES]) }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        read(prefs[SETTINGS])?.let { runCatching { json.decodeFromString(Settings.serializer(), it) }.getOrNull() } ?: Settings()
    }

    val lock: Flow<LockState> = context.dataStore.data.map { prefs ->
        read(prefs[LOCK])?.let { runCatching { json.decodeFromString(LockState.serializer(), it) }.getOrNull() } ?: LockState()
    }

    /** Applies [transform] to the stored list in one transaction. */
    suspend fun edit(transform: (List<Note>) -> List<Note>) {
        context.dataStore.edit { prefs ->
            prefs[NOTES] = cipher.encrypt(json.encodeToString(serializer, transform(decode(prefs[NOTES]))))
        }
    }

    suspend fun saveSettings(settings: Settings) {
        context.dataStore.edit { prefs -> prefs[SETTINGS] = cipher.encrypt(json.encodeToString(Settings.serializer(), settings)) }
    }

    suspend fun editLock(transform: (LockState) -> LockState) {
        context.dataStore.edit { prefs ->
            val current = read(prefs[LOCK])?.let { runCatching { json.decodeFromString(LockState.serializer(), it) }.getOrNull() }
            prefs[LOCK] = cipher.encrypt(json.encodeToString(LockState.serializer(), transform(current ?: LockState())))
        }
    }

    /** Encrypts values written before encryption existed; run once at start. */
    suspend fun encryptLegacyValues() {
        context.dataStore.edit { prefs ->
            listOf(NOTES, SETTINGS, LOCK).forEach { key ->
                prefs[key]?.takeUnless(cipher::isEncrypted)?.let { prefs[key] = cipher.encrypt(it) }
            }
        }
    }

    /** All notes, trash included, as the JSON written to a backup file. */
    fun encodeBackup(notes: List<Note>): String = backupJson.encodeToString(serializer, notes)

    /** Notes read from a backup file; null if it is not a Notlar backup. */
    fun decodeBackup(raw: String): List<Note>? = runCatching { json.decodeFromString(serializer, raw) }.getOrNull()

    private fun decode(raw: String?): List<Note> = read(raw)?.let(::decodeBackup).orEmpty()

    /** The plain text of a stored value; null if it is missing or cannot be decrypted. */
    private fun read(stored: String?): String? = stored?.let { runCatching { cipher.decrypt(it) }.getOrNull() }

    private companion object {
        val NOTES = stringPreferencesKey("notes")
        val SETTINGS = stringPreferencesKey("settings")
        val LOCK = stringPreferencesKey("lock")
        val json = Json { ignoreUnknownKeys = true }
        val backupJson = Json(json) { prettyPrint = true }
    }
}
