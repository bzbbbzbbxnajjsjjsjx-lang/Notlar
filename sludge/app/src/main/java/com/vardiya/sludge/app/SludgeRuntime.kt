package com.vardiya.sludge.app

import android.content.Context
import android.util.Log
import com.vardiya.sludge.audio.EffectEngine
import com.vardiya.sludge.audio.OutputMonitor
import com.vardiya.sludge.audio.TonePlayer
import com.vardiya.sludge.data.SettingsRepository
import com.vardiya.sludge.domain.EngineState
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.OutputRoute
import com.vardiya.sludge.domain.SweepKind
import com.vardiya.sludge.domain.ToneState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * The composition root. It builds the pieces once and hands the UI and the Android entry points (the
 * audio-session receiver) one place to talk to; the logic itself lives in [EqController] and the pure `domain`
 * package. They all run in the same process, so this is a plain singleton.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SludgeRuntime private constructor(context: Context) {
    // An effect that fails in an unexpected way must not take the process down with it.
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e -> Log.w(TAG, "background failure: ${e.javaClass.simpleName}") },
    )
    private val repo = SettingsRepository(context)
    private val output = OutputMonitor(context)

    private val engine = EffectEngine()
    val engineState: StateFlow<EngineState> = engine.state
    val route: StateFlow<OutputRoute> = output.route

    // The settings live in memory so a dragged slider answers at once; the store is written a moment after the last change.
    private val current = MutableStateFlow<EqSettings?>(null)
    private val changes = Channel<(EqSettings) -> EqSettings>(Channel.UNLIMITED)
    private val loaded: Flow<EqSettings> = current.filterNotNull()
    val settings: StateFlow<EqSettings> = loaded.stateIn(scope, SharingStarted.Eagerly, EqSettings())

    private val eq = EqController(scope, Dispatchers.IO.limitedParallelism(1), engine, loaded, output.route)
    private val tone = TonePlayer(context, scope, { session, done -> eq.attach(session, TONE_LABEL, done) }, { eq.detach(it) {} })
    val toneState: StateFlow<ToneState> = tone.state

    init {
        scope.launch {
            repo.dropRetiredKeys()
            current.value = repo.settings.first()
            // One coroutine applies the changes in the order they were made.
            launch { for (change in changes) current.update { it?.let(change) } }
            loaded.drop(1).debounce(SAVE_DELAY_MS).collect { repo.saveSettings(it) }
        }
    }

    fun attach(session: Int, packageName: String, done: () -> Unit) = eq.attach(session, packageName, done)

    fun detach(session: Int, done: () -> Unit) = eq.detach(session, done)

    fun startTone(kind: SweepKind) = tone.start(kind)

    fun stopTone() = tone.stop()

    fun update(transform: (EqSettings) -> EqSettings) {
        changes.trySend(transform)
    }

    companion object {
        private const val TAG = "Sludge"
        private const val TONE_LABEL = "Test tonu"
        private const val SAVE_DELAY_MS = 300L

        @Volatile private var instance: SludgeRuntime? = null

        fun get(context: Context): SludgeRuntime = instance ?: synchronized(this) {
            instance ?: SludgeRuntime(context.applicationContext).also { instance = it }
        }
    }
}
