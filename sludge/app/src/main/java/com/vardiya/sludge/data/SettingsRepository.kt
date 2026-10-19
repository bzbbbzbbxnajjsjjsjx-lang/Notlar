package com.vardiya.sludge.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vardiya.sludge.domain.AiConfig
import com.vardiya.sludge.domain.AiProfile
import com.vardiya.sludge.domain.AiProtocol
import com.vardiya.sludge.domain.AiProvider
import com.vardiya.sludge.domain.BAND_COUNT
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.Headphones
import com.vardiya.sludge.domain.LastFmConfig
import com.vardiya.sludge.domain.ListenEntry
import com.vardiya.sludge.domain.ListenStats
import com.vardiya.sludge.domain.TuneSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

private val Context.store by preferencesDataStore(name = "sludge")

/** Everything the app remembers, in one Preferences DataStore. */
class SettingsRepository(context: Context) {
    private val store = context.applicationContext.store

    val settings: Flow<EqSettings> = store.data.map { it.toSettings() }
    val ai: Flow<AiConfig> = store.data.map { it.toAiConfig() }
    val cache: Flow<Map<String, AiProfile>> = store.data.map { it.toCache() }
    val history: Flow<List<ListenEntry>> = store.data.map { it.toHistory() }
    val lastFm: Flow<LastFmConfig> = store.data.map { LastFmConfig(KeyCipher.decrypt(it[LASTFM_KEY].orEmpty())) }
    val tagCache: Flow<Map<String, List<String>>> = store.data.map { it.toTagCache() }

    suspend fun updateSettings(transform: (EqSettings) -> EqSettings) {
        store.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }

    suspend fun saveAi(config: AiConfig) {
        store.edit { prefs ->
            prefs[AI_PROVIDER] = config.provider.name
            prefs[AI_MODEL] = config.model.trim()
            prefs[AI_URL] = config.baseUrl.trim()
            prefs[AI_KEY] = KeyCipher.encrypt(config.apiKey.trim())
        }
    }

    suspend fun saveLastFm(config: LastFmConfig) {
        store.edit { it[LASTFM_KEY] = KeyCipher.encrypt(config.apiKey.trim()) }
    }

    suspend fun updateTagCache(transform: (Map<String, List<String>>) -> Map<String, List<String>>) {
        store.edit { prefs -> prefs[TAG_CACHE] = AiProtocol.json.encodeToString(TAG_SERIALIZER, transform(prefs.toTagCache())) }
    }

    private fun Preferences.toTagCache(): Map<String, List<String>> = runCatching {
        AiProtocol.json.decodeFromString(TAG_SERIALIZER, this[TAG_CACHE].orEmpty())
    }.getOrDefault(emptyMap())

    suspend fun updateCache(transform: (Map<String, AiProfile>) -> Map<String, AiProfile>) {
        store.edit { prefs ->
            prefs[CACHE] = AiProtocol.json.encodeToString(CACHE_SERIALIZER, transform(prefs.toCache()))
        }
    }

    suspend fun addListen(entry: ListenEntry) {
        store.edit { prefs ->
            prefs[HISTORY] = AiProtocol.json.encodeToString(HISTORY_SERIALIZER, ListenStats.append(prefs.toHistory(), entry))
        }
    }

    suspend fun clearHistory() {
        store.edit { it.remove(HISTORY) }
    }

    private fun Preferences.toHistory(): List<ListenEntry> = runCatching {
        AiProtocol.json.decodeFromString(HISTORY_SERIALIZER, this[HISTORY].orEmpty())
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
            headphoneId = when (val stored = this[HEADPHONE]) {
                null -> Headphones.DEFAULT_ID
                "" -> null
                else -> stored
            },
        )
    }

    private fun Preferences.toAiConfig(): AiConfig {
        val provider = runCatching { AiProvider.valueOf(this[AI_PROVIDER].orEmpty()) }.getOrDefault(AiProvider.CLAUDE)
        return AiConfig(
            provider = provider,
            model = this[AI_MODEL] ?: provider.defaultModel,
            baseUrl = this[AI_URL] ?: provider.defaultBaseUrl,
            apiKey = KeyCipher.decrypt(this[AI_KEY].orEmpty()),
        )
    }

    // Entries written when the equalizer had five bands no longer fit and are dropped.
    private fun Preferences.toCache(): Map<String, AiProfile> = runCatching {
        AiProtocol.json.decodeFromString(CACHE_SERIALIZER, this[CACHE].orEmpty()).filterValues { it.bands.size == BAND_COUNT }
    }.getOrDefault(emptyMap())

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
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val AI_URL = stringPreferencesKey("ai_url")
        val AI_KEY = stringPreferencesKey("ai_key")
        val CACHE = stringPreferencesKey("tune_cache")
        val HISTORY = stringPreferencesKey("history")
        val LASTFM_KEY = stringPreferencesKey("lastfm_key")
        val TAG_CACHE = stringPreferencesKey("tag_cache")
        val TAG_SERIALIZER = MapSerializer(String.serializer(), ListSerializer(String.serializer()))
        val HISTORY_SERIALIZER = ListSerializer(ListenEntry.serializer())
        val CACHE_SERIALIZER = MapSerializer(String.serializer(), AiProfile.serializer())
    }
}
