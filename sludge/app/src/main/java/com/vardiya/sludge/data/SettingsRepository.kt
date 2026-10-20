package com.vardiya.sludge.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.Headphones
import com.vardiya.sludge.domain.Parametric
import com.vardiya.sludge.domain.ParametricFilter
import com.vardiya.sludge.domain.Trim
import com.vardiya.sludge.domain.TuneSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.store by preferencesDataStore(name = "sludge")

/** Everything the app remembers, in one Preferences DataStore. */
class SettingsRepository(context: Context) {
    private val store = context.applicationContext.store

    val settings: Flow<EqSettings> = store.data.map { it.toSettings() }

    suspend fun saveSettings(settings: EqSettings) {
        store.edit { prefs -> prefs.write(settings) }
    }

    /** Earlier versions kept keys, a listening log and tuning notes here; nothing uses them now, so they are wiped. */
    suspend fun dropRetiredKeys() {
        store.edit { prefs -> RETIRED_KEYS.forEach { prefs.remove(stringPreferencesKey(it)) } }
    }

    private fun Preferences.toSettings(): EqSettings {
        return EqSettings(
            enabled = this[ENABLED] ?: true,
            bassBoost = this[BASS] ?: 0,
            virtualizer = this[VIRTUALIZER] ?: 0,
            loudnessMb = this[LOUDNESS] ?: 0,
            genre = this[GENRE].orEmpty(),
            source = runCatching { TuneSource.valueOf(this[SOURCE].orEmpty()) }.getOrDefault(TuneSource.MANUAL),
            // Never chosen: the default profile. Chosen "none" is stored as an empty string.
            headphoneId = Headphones.fromStored(this[HEADPHONE]),
            headphoneAlways = this[HEADPHONE_ALWAYS] ?: false,
            trimBassDb = Trim.clamp(this[TRIM_BASS] ?: 0f),
            trimTrebleDb = Trim.clamp(this[TRIM_TREBLE] ?: 0f),
            filters = this[FILTERS]?.let { stored ->
                runCatching { json.decodeFromString(FILTERS_SERIALIZER, stored) }.getOrNull()
            }?.filter { it.isFinite }?.map { it.clamped() }?.take(Parametric.MAX_FILTERS)
                ?: emptyList(),
        )
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.write(s: EqSettings) {
        this[ENABLED] = s.enabled
        this[BASS] = s.bassBoost
        this[VIRTUALIZER] = s.virtualizer
        this[LOUDNESS] = s.loudnessMb
        this[GENRE] = s.genre
        this[SOURCE] = s.source.name
        this[HEADPHONE] = s.headphoneId.orEmpty()
        this[HEADPHONE_ALWAYS] = s.headphoneAlways
        this[TRIM_BASS] = s.trimBassDb
        this[TRIM_TREBLE] = s.trimTrebleDb
        this[FILTERS] = json.encodeToString(FILTERS_SERIALIZER, s.filters)
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("enabled")
        val BASS = intPreferencesKey("bass")
        val VIRTUALIZER = intPreferencesKey("virtualizer")
        val LOUDNESS = intPreferencesKey("loudness")
        val GENRE = stringPreferencesKey("genre")
        val SOURCE = stringPreferencesKey("source")
        val HEADPHONE = stringPreferencesKey("headphone")
        val TRIM_BASS = floatPreferencesKey("trim_bass")
        val TRIM_TREBLE = floatPreferencesKey("trim_treble")
        val HEADPHONE_ALWAYS = booleanPreferencesKey("headphone_always")
        val FILTERS = stringPreferencesKey("filters")
        val FILTERS_SERIALIZER = ListSerializer(ParametricFilter.serializer())
        val RETIRED_KEYS = listOf(
            "ai_provider", "ai_model", "ai_url", "ai_key", "tune_cache", "lastfm_key", "tag_cache", "eq_mode", "bands",
            "history", "auto_tune", "explanation", "headphone_strength",
        )
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    }
}
