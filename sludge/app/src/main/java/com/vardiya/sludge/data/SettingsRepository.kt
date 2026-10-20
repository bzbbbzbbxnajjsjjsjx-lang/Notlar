package com.vardiya.sludge.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vardiya.sludge.domain.BAND_COUNT
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.Headphones
import com.vardiya.sludge.domain.ListenEntry
import com.vardiya.sludge.domain.ListenStats
import com.vardiya.sludge.domain.Parametric
import com.vardiya.sludge.domain.ParametricFilter
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
    val history: Flow<List<ListenEntry>> = store.data.map { it.toHistory() }

    suspend fun updateSettings(transform: (EqSettings) -> EqSettings) {
        store.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }

    /** Earlier versions kept an AI key and a Last.fm key here; nothing uses them now, so they are wiped. */
    suspend fun dropRetiredKeys() {
        store.edit { prefs -> RETIRED_KEYS.forEach { prefs.remove(stringPreferencesKey(it)) } }
    }

    suspend fun addListen(entry: ListenEntry) {
        store.edit { prefs ->
            prefs[HISTORY] = json.encodeToString(HISTORY_SERIALIZER, ListenStats.append(prefs.toHistory(), entry))
        }
    }

    suspend fun clearHistory() {
        store.edit { it.remove(HISTORY) }
    }

    private fun Preferences.toHistory(): List<ListenEntry> = runCatching {
        json.decodeFromString(HISTORY_SERIALIZER, this[HISTORY].orEmpty())
    }.getOrDefault(emptyList())

    private fun Preferences.toSettings(): EqSettings {
        val bands = this[BANDS]?.split(',')?.mapNotNull { it.toFloatOrNull() }?.takeIf { it.size == BAND_COUNT }
        return EqSettings(
            enabled = this[ENABLED] ?: true,
            bands = bands ?: EqSettings.FLAT_BANDS,
            bassBoost = this[BASS] ?: 0,
            virtualizer = this[VIRTUALIZER] ?: 0,
            loudnessMb = this[LOUDNESS] ?: 0,
            genre = this[GENRE].orEmpty(),
            autoTune = this[AUTO_TUNE] ?: true,
            explanation = this[EXPLANATION].orEmpty(),
            source = runCatching { TuneSource.valueOf(this[SOURCE].orEmpty()) }.getOrDefault(TuneSource.MANUAL),
            // Never chosen: the default profile. Chosen "none" is stored as an empty string.
            headphoneId = Headphones.fromStored(this[HEADPHONE]),
            headphoneAlways = this[HEADPHONE_ALWAYS] ?: false,
            headphoneStrength = (this[HEADPHONE_STRENGTH] ?: FULL_STRENGTH).coerceIn(0, FULL_STRENGTH),
            filters = this[FILTERS]?.let { stored ->
                runCatching { json.decodeFromString(FILTERS_SERIALIZER, stored) }.getOrNull()
            }?.filter { it.isFinite }?.map { it.clamped() }?.take(Parametric.MAX_FILTERS)
                ?: Parametric.fromBands(bands ?: EqSettings.FLAT_BANDS),
        )
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.write(s: EqSettings) {
        this[ENABLED] = s.enabled
        this[BANDS] = s.bands.joinToString(",")
        this[BASS] = s.bassBoost
        this[VIRTUALIZER] = s.virtualizer
        this[LOUDNESS] = s.loudnessMb
        this[GENRE] = s.genre
        this[AUTO_TUNE] = s.autoTune
        this[EXPLANATION] = s.explanation
        this[SOURCE] = s.source.name
        this[HEADPHONE] = s.headphoneId.orEmpty()
        this[HEADPHONE_ALWAYS] = s.headphoneAlways
        this[HEADPHONE_STRENGTH] = s.headphoneStrength
        this[FILTERS] = json.encodeToString(FILTERS_SERIALIZER, s.filters)
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("enabled")
        val BANDS = stringPreferencesKey("bands")
        val BASS = intPreferencesKey("bass")
        val VIRTUALIZER = intPreferencesKey("virtualizer")
        val LOUDNESS = intPreferencesKey("loudness")
        val GENRE = stringPreferencesKey("genre")
        val AUTO_TUNE = booleanPreferencesKey("auto_tune")
        val EXPLANATION = stringPreferencesKey("explanation")
        val SOURCE = stringPreferencesKey("source")
        val HEADPHONE = stringPreferencesKey("headphone")
        val HEADPHONE_ALWAYS = booleanPreferencesKey("headphone_always")
        val HEADPHONE_STRENGTH = intPreferencesKey("headphone_strength")
        val FILTERS = stringPreferencesKey("filters")
        val FILTERS_SERIALIZER = ListSerializer(ParametricFilter.serializer())
        const val FULL_STRENGTH = 100
        val HISTORY = stringPreferencesKey("history")
        val RETIRED_KEYS = listOf("ai_provider", "ai_model", "ai_url", "ai_key", "tune_cache", "lastfm_key", "tag_cache", "eq_mode")
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        val HISTORY_SERIALIZER = ListSerializer(ListenEntry.serializer())
    }
}
