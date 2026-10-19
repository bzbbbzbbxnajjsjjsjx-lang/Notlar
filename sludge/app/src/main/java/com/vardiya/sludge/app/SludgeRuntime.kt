package com.vardiya.sludge.app

import android.content.Context
import com.vardiya.sludge.audio.EffectEngine
import com.vardiya.sludge.data.AiTuner
import com.vardiya.sludge.data.RepoTuneStore
import com.vardiya.sludge.data.SettingsRepository
import com.vardiya.sludge.domain.AiConfig
import com.vardiya.sludge.domain.AiProfile
import com.vardiya.sludge.domain.AutoTuner
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.ListenEntry
import com.vardiya.sludge.domain.NowPlaying
import com.vardiya.sludge.domain.TrackInfo
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The composition root. It builds the pieces once and hands the UI and the Android entry points (listener service,
 * audio-session receiver) one place to talk to; the logic itself lives in [EqController], [TuneCoordinator] and the
 * pure `domain` package. They all run in the same process, so this is a plain singleton.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SludgeRuntime private constructor(context: Context) {
    // An effect that fails in an unexpected way must not take the process (and the listener) down with it.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, _ -> })
    private val repo = SettingsRepository(context)
    private val aiTuner = AiTuner()

    val engine = EffectEngine()
    val settings: StateFlow<EqSettings> = repo.settings.stateIn(scope, SharingStarted.Eagerly, EqSettings())
    val ai: StateFlow<AiConfig> = repo.ai.stateIn(scope, SharingStarted.Eagerly, AiConfig())
    val history: StateFlow<List<ListenEntry>> = repo.history.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _nowPlaying = MutableStateFlow(NowPlaying())
    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying.asStateFlow()

    private val eq = EqController(scope, Dispatchers.Default.limitedParallelism(1), engine, repo)
    private val tuning = TuneCoordinator(scope, nowPlaying, settings, ai, AutoTuner(RepoTuneStore(repo), aiTuner), repo)
    val status: StateFlow<TuneStatus> = tuning.status

    fun publish(nowPlaying: NowPlaying) {
        _nowPlaying.value = nowPlaying
    }

    fun attach(session: Int, packageName: String, done: () -> Unit) = eq.attach(session, packageName, done)

    fun detach(session: Int, done: () -> Unit) = eq.detach(session, done)

    fun update(transform: (EqSettings) -> EqSettings) {
        scope.launch { repo.updateSettings(transform) }
    }

    fun saveAi(config: AiConfig) {
        scope.launch { repo.saveAi(config) }
    }

    fun clearHistory() {
        scope.launch { repo.clearHistory() }
    }

    /** The AI button. Does nothing when no track is known. */
    fun tuneNow() {
        nowPlaying.value.track?.let(tuning::tuneNow)
    }

    /** Connection test from the AI screen: runs a known stoner track through the model without changing the sound. */
    suspend fun testConnection(config: AiConfig): Result<AiProfile> = aiTuner.tune(config, SAMPLE_TRACK)

    companion object {
        private val SAMPLE_TRACK = TrackInfo(title = "Dopesmoker", artist = "Sleep", album = "Dopesmoker", genreTag = "Stoner Metal")

        @Volatile private var instance: SludgeRuntime? = null

        fun get(context: Context): SludgeRuntime = instance ?: synchronized(this) {
            instance ?: SludgeRuntime(context.applicationContext).also { instance = it }
        }
    }
}
