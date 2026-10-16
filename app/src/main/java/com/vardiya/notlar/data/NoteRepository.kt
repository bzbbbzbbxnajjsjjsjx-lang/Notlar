package com.vardiya.notlar.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vardiya.notlar.domain.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "notlar")

/** Persists all notes as one JSON list in Preferences DataStore. */
class NoteRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(Note.serializer())

    val notes: Flow<List<Note>> = context.dataStore.data.map { prefs -> decode(prefs[NOTES]) }

    /** Applies [transform] to the stored list in one transaction. */
    suspend fun edit(transform: (List<Note>) -> List<Note>) {
        context.dataStore.edit { prefs ->
            prefs[NOTES] = json.encodeToString(serializer, transform(decode(prefs[NOTES])))
        }
    }

    private fun decode(raw: String?): List<Note> =
        raw?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }.orEmpty()

    private companion object {
        val NOTES = stringPreferencesKey("notes")
    }
}
