package com.vardiya.sludge.audio

import android.content.Context
import com.vardiya.sludge.data.AiTuner
import com.vardiya.sludge.data.SettingsRepository
import com.vardiya.sludge.domain.AiConfig
import com.vardiya.sludge.domain.AiProfile
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.TrackInfo
import com.vardiya.sludge.domain.TuneCache
import com.vardiya.sludge.domain.TuneSource
import com.vardiya.sludge.domain.applying
import com.vardiya.sludge.domain.localProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the tuning request is doing right now, for the screen to show. */
sealed interface TuneStatus {
    data object Idle : TuneStatus
    data object Working : TuneStatus
    data class Failed(val message: String) : TuneStatus
}

/**
 * The one place that owns the effects, the settings and the tuning loop. The listener service, the audio-session
 * receiver and the UI all live in the same process and talk to this single instance.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SludgeRuntime private constructor(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    // Audio effect calls block on the media server; keep them off the main thread and in order.
    private val audio = Dispatchers.Default.limitedParallelism(1)
    private val repo = SettingsRepository(context)
    private val tuner = AiTuner()
    val engine = EffectEngine()

    val settings: StateFlow<EqSettings> = repo.settings.stateIn(scope, SharingStarted.Eagerly, EqSettings())
    val ai: StateFlow<AiConfig> = repo.ai.stateIn(scope, SharingStarted.Eagerly, AiConfig())

    private val _track = MutableStateFlow<TrackInfo?>(null)
    val track: StateFlow<TrackInfo?> = _track.asStateFlow()

    private val _status = MutableStateFlow<TuneStatus>(TuneStatus.Idle)
    val status: StateFlow<TuneStatus> = _status.asStateFlow()

    init {
        scope.launch(audio) {
            // Wait for the stored settings before touching the audio, so a restart never flashes the defaults.
            val first = repo.settings.first()
            engine.tryGlobalMix(first)
            repo.settings.distinctUntilChanged().collect { engine.apply(it) }
        }
        scope.launch {
            _track.filterNotNull().distinctUntilChanged().debounce(TRACK_SETTLE_MS).collectLatest { track ->
                if (settings.value.autoTune) tune(track, manual = false)
            }
        }
    }

    fun publishTrack(track: TrackInfo?) {
        _track.value = track
    }

    fun attach(session: Int, packageName: String, done: () -> Unit) {
        scope.launch(audio) {
            try {
                engine.attach(session, packageName, repo.settings.first())
            } finally {
                done()
            }
        }
    }

    fun detach(session: Int, done: () -> Unit) {
        scope.launch(audio) {
            try {
                engine.detach(session)
            } finally {
                done()
            }
        }
    }

    fun update(transform: (EqSettings) -> EqSettings) {
        scope.launch { repo.updateSettings(transform) }
    }

    fun saveAi(config: AiConfig) {
        scope.launch { repo.saveAi(config) }
    }

    /** The AI button: tunes for the current track even when auto-tune is off. */
    fun tuneNow() {
        val track = _track.value ?: return
        scope.launch { tune(track, manual = true) }
    }

    /** Connection test from the AI screen: runs a known stoner track through the model without changing the sound. */
    suspend fun testConnection(config: AiConfig): Result<AiProfile> = tuner.tune(config, SAMPLE_TRACK)

    private suspend fun tune(track: TrackInfo, manual: Boolean) {
        _status.value = TuneStatus.Working
        val config = ai.value
        val key = TuneCache.key(track)
        val cached = repo.cache.first()[key]
        if (cached != null) {
            applyProfile(cached, TuneSource.AI)
            _status.value = TuneStatus.Idle
            return
        }
        if (!config.isUsable) {
            applyProfile(localProfile(track), TuneSource.LOCAL)
            _status.value = TuneStatus.Idle
            return
        }
        tuner.tune(config, track).fold(
            onSuccess = { profile ->
                repo.updateCache { TuneCache.put(it, key, profile) }
                applyProfile(profile, TuneSource.AI)
                _status.value = TuneStatus.Idle
            },
            onFailure = { error ->
                // Offline or over quota: the local guess still beats leaving the previous track's curve on.
                applyProfile(localProfile(track), TuneSource.LOCAL)
                _status.value = if (manual) TuneStatus.Failed(error.message.orEmpty()) else TuneStatus.Idle
            },
        )
    }

    private suspend fun applyProfile(profile: AiProfile, source: TuneSource) {
        repo.updateSettings { it.applying(profile, source) }
    }

    companion object {
        private const val TRACK_SETTLE_MS = 1_500L
        private val SAMPLE_TRACK = TrackInfo(title = "Dopesmoker", artist = "Sleep", album = "Dopesmoker", genreTag = "Stoner Metal")

        @Volatile private var instance: SludgeRuntime? = null

        fun get(context: Context): SludgeRuntime = instance ?: synchronized(this) {
            instance ?: SludgeRuntime(context.applicationContext).also { instance = it }
        }
    }
}
