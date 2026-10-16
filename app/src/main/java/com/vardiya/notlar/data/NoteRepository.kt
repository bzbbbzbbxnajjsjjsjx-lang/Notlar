package com.vardiya.notlar.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "notlar")

/** Persists all notes as one JSON list, and the settings as one JSON object, in Preferences DataStore. */
class NoteRepository(private val context: Context) {

    private val serializer = ListSerializer(Note.serializer())

    val notes: Flow<List<Note>> = context.dataStore.data.map { prefs -> decode(prefs[NOTES]) }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        prefs[SETTINGS]?.let { runCatching { json.decodeFromString(Settings.serializer(), it) }.getOrNull() } ?: Settings()
    }

    /** Applies [transform] to the stored list in one transaction. */
    suspend fun edit(transform: (List<Note>) -> List<Note>) {
        context.dataStore.edit { prefs ->
            prefs[NOTES] = json.encodeToString(serializer, transform(decode(prefs[NOTES])))
        }
    }

    suspend fun saveSettings(settings: Settings) {
        context.dataStore.edit { prefs -> prefs[SETTINGS] = json.encodeToString(Settings.serializer(), settings) }
    }

    /** All notes, trash included, as the JSON written to a backup file. */
    fun encodeBackup(notes: List<Note>): String = backupJson.encodeToString(serializer, notes)

    /** Notes read from a backup file; null if it is not a Notlar backup. */
    fun decodeBackup(raw: String): List<Note>? = runCatching { json.decodeFromString(serializer, raw) }.getOrNull()

    private fun decode(raw: String?): List<Note> = raw?.let(::decodeBackup).orEmpty()

    private companion object {
        val NOTES = stringPreferencesKey("notes")
        val SETTINGS = stringPreferencesKey("settings")
        val json = Json { ignoreUnknownKeys = true }
        val backupJson = Json(json) { prettyPrint = true }
    }
}
