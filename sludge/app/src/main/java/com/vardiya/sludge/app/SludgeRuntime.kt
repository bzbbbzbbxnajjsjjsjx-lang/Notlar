package com.vardiya.sludge.app

import android.content.Context
import android.util.Log
import com.vardiya.sludge.audio.EffectEngine
import com.vardiya.sludge.audio.OutputMonitor
import com.vardiya.sludge.data.AiTuner
import com.vardiya.sludge.data.LastFmClient
import com.vardiya.sludge.data.LastFmTagSource
import com.vardiya.sludge.data.ModelCatalog
import com.vardiya.sludge.data.test
import com.vardiya.sludge.data.RepoTuneStore
import com.vardiya.sludge.data.SettingsRepository
import com.vardiya.sludge.domain.AiConfig
import com.vardiya.sludge.domain.AiProfile
import com.vardiya.sludge.domain.AutoTuner
import com.vardiya.sludge.domain.EngineState
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.LastFmConfig
import com.vardiya.sludge.domain.ListenEntry
import com.vardiya.sludge.domain.ModelOption
import com.vardiya.sludge.domain.NowPlaying
import com.vardiya.sludge.domain.OutputRoute
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
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e -> Log.w(TAG, "background failure: ${e.javaClass.simpleName}") },
    )
    private val repo = SettingsRepository(context)
    private val aiTuner = AiTuner()
    private val lastFm = LastFmClient()
    private val models = ModelCatalog()
    private val output = OutputMonitor(context)

    private val engine = EffectEngine()
    val engineState: StateFlow<EngineState> = engine.state
    val route: StateFlow<OutputRoute> = output.route
    val settings: StateFlow<EqSettings> = repo.settings.stateIn(scope, SharingStarted.Eagerly, EqSettings())
    val ai: StateFlow<AiConfig> = repo.ai.stateIn(scope, SharingStarted.Eagerly, AiConfig())
    val history: StateFlow<List<ListenEntry>> = repo.history.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _nowPlaying = MutableStateFlow(NowPlaying())
    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying.asStateFlow()

    private val eq = EqController(scope, Dispatchers.IO.limitedParallelism(1), engine, repo, output.route)
    private val tuning = TuneCoordinator(
        scope, nowPlaying, RepoTuneEnvironment(repo), AutoTuner(RepoTuneStore(repo), aiTuner), LastFmTagSource(lastFm, repo),
    )
    val status: StateFlow<TuneStatus> = tuning.status
    val tags: StateFlow<List<String>> = tuning.tags
    val lastFmConfig: StateFlow<LastFmConfig> = repo.lastFm.stateIn(scope, SharingStarted.Eagerly, LastFmConfig())

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

    fun saveLastFm(config: LastFmConfig) {
        scope.launch { repo.saveLastFm(config) }
    }

    /** The Last.fm try-it button: looks up a known stoner track with the key as typed, without saving it. */
    suspend fun testLastFm(config: LastFmConfig): Result<List<String>> = lastFm.test(config, SAMPLE_TRACK)

    /** The provider's model list for the picker (cached for this run). */
    suspend fun loadModels(baseUrl: String): Result<List<ModelOption>> = models.load(baseUrl)

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
        private const val TAG = "Sludge"
        private val SAMPLE_TRACK = TrackInfo(title = "Dopesmoker", artist = "Sleep", album = "Dopesmoker", genreTag = "Stoner Metal")

        @Volatile private var instance: SludgeRuntime? = null

        fun get(context: Context): SludgeRuntime = instance ?: synchronized(this) {
            instance ?: SludgeRuntime(context.applicationContext).also { instance = it }
        }
    }
}
