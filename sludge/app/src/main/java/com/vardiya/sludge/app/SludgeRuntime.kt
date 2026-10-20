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
import com.vardiya.sludge.domain.TONE_PACKAGE
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
import kotlinx.coroutines.flow.filter
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
    private val tone = TonePlayer(context, scope, { session, done -> eq.attach(session, TONE_PACKAGE, done) }, { eq.detach(it) {} })
    val toneState: StateFlow<ToneState> = tone.state

    init {
        scope.launch {
            // A store that cannot be read or cleaned must not leave the equalizer dead: the defaults are safe.
            runCatching { repo.dropRetiredKeys() }.onFailure { Log.w(TAG, "retired keys not wiped: ${it.javaClass.simpleName}") }
            val initial = runCatching { repo.settings.first() }
                .onFailure { Log.w(TAG, "settings not read: ${it.javaClass.simpleName}") }
                .getOrDefault(EqSettings())
            current.value = initial
            // Changes and saving are separate coroutines: a failing save or change must not stop the other.
            launch { applyChanges() }
            launch { save(initial) }
        }
        // Another output (a pulled plug) must not carry the tone onto the loudspeaker.
        scope.launch { output.route.drop(1).collect { tone.stop() } }
    }

    // One coroutine applies the changes in the order they were made.
    private suspend fun applyChanges() {
        for (change in changes) {
            runCatching { current.update { it?.let(change) } }.onFailure { Log.w(TAG, "change failed: ${it.javaClass.simpleName}") }
        }
    }

    private suspend fun save(initial: EqSettings) {
        loaded.filter { it != initial }.debounce(SAVE_DELAY_MS).collect { write(it) }
    }

    private suspend fun write(settings: EqSettings) {
        runCatching { repo.saveSettings(settings) }.onFailure { Log.w(TAG, "settings not saved: ${it.javaClass.simpleName}") }
    }

    /** Saves at once; the screen leaving the front must not lose the last change that the short delay is still holding. */
    fun flush() {
        val settings = current.value ?: return
        scope.launch { write(settings) }
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
        private const val SAVE_DELAY_MS = 300L

        @Volatile private var instance: SludgeRuntime? = null

        fun get(context: Context): SludgeRuntime = instance ?: synchronized(this) {
            instance ?: SludgeRuntime(context.applicationContext).also { instance = it }
        }
    }
}
