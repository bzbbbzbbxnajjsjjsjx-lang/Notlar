package com.vardiya.sludge.app

import android.content.Context
import android.util.Log
import com.vardiya.sludge.audio.EffectEngine
import com.vardiya.sludge.audio.OutputMonitor
import com.vardiya.sludge.data.SettingsRepository
import com.vardiya.sludge.domain.EngineState
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.ListenEntry
import com.vardiya.sludge.domain.NowPlaying
import com.vardiya.sludge.domain.OutputRoute
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
    private val output = OutputMonitor(context)

    private val engine = EffectEngine()
    val engineState: StateFlow<EngineState> = engine.state
    val route: StateFlow<OutputRoute> = output.route
    val settings: StateFlow<EqSettings> = repo.settings.stateIn(scope, SharingStarted.Eagerly, EqSettings())
    val history: StateFlow<List<ListenEntry>> = repo.history.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _nowPlaying = MutableStateFlow(NowPlaying())
    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying.asStateFlow()

    private val eq = EqController(scope, Dispatchers.IO.limitedParallelism(1), engine, repo, output.route)
    private val tuning = TuneCoordinator(scope, nowPlaying, RepoTuneEnvironment(repo))
    val status: StateFlow<TuneStatus> = tuning.status

    init {
        scope.launch { repo.dropRetiredKeys() }
    }

    fun publish(nowPlaying: NowPlaying) {
        _nowPlaying.value = nowPlaying
    }

    fun attach(session: Int, packageName: String, done: () -> Unit) = eq.attach(session, packageName, done)

    fun detach(session: Int, done: () -> Unit) = eq.detach(session, done)

    fun update(transform: (EqSettings) -> EqSettings) {
        scope.launch { repo.updateSettings(transform) }
    }

    fun clearHistory() {
        scope.launch { repo.clearHistory() }
    }

    companion object {
        private const val TAG = "Sludge"

        @Volatile private var instance: SludgeRuntime? = null

        fun get(context: Context): SludgeRuntime = instance ?: synchronized(this) {
            instance ?: SludgeRuntime(context.applicationContext).also { instance = it }
        }
    }
}
