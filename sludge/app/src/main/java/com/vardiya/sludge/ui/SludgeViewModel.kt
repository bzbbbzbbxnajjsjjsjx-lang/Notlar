package com.vardiya.sludge.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vardiya.sludge.app.SludgeRuntime
import com.vardiya.sludge.domain.AiConfig
import com.vardiya.sludge.domain.AiProfile
import com.vardiya.sludge.domain.CurveMath
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.Genre
import com.vardiya.sludge.domain.LastFmConfig
import com.vardiya.sludge.domain.ModelOption
import com.vardiya.sludge.domain.ListenStats
import com.vardiya.sludge.domain.MAX_EFFECT
import com.vardiya.sludge.domain.MAX_LOUDNESS_MB
import com.vardiya.sludge.domain.TuneSource
import com.vardiya.sludge.domain.applying
import com.vardiya.sludge.domain.toProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId

class SludgeViewModel(private val app: Application) : AndroidViewModel(app), SludgeActions {
    private val runtime = SludgeRuntime.get(app)
    private val system = SystemSettings(app)
    private val access = MutableStateFlow(system.isNotificationAccessGranted())

    val accessGranted: Boolean get() = access.value

    private val core = combine(runtime.settings, runtime.nowPlaying, runtime.status, runtime.ai) { settings, playing, status, ai ->
        UiState(settings = settings, track = playing.track, playing = playing.playing, status = status, ai = ai)
    }.combine(combine(runtime.lastFmConfig, runtime.tags, runtime.route, ::Triple)) { ui, (lastFm, tags, route) ->
        ui.copy(lastFm = lastFm, tags = tags, route = route)
    }

    val state: StateFlow<UiState> = combine(core, runtime.engineState, access, runtime.history) { ui, engine, granted, history ->
        ui.copy(
            sessions = engine.sessions,
            globalMixWorks = engine.globalMixWorks,
            accessGranted = granted,
            listens = ListenStats.summarize(history, System.currentTimeMillis(), ZoneId.systemDefault()),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState(accessGranted = access.value))

    /** Called when the app comes back to the front: the user may just have flipped the switch in system settings. */
    fun refreshAccess() {
        access.value = system.isNotificationAccessGranted()
    }

    private fun manual(transform: (EqSettings) -> EqSettings) = runtime.update { transform(it).copy(source = TuneSource.MANUAL) }

    override fun setEnabled(enabled: Boolean) = runtime.update { it.copy(enabled = enabled) }

    override fun setBand(index: Int, db: Float) = manual { s ->
        s.copy(bands = s.bands.mapIndexed { i, old -> if (i == index) db else old })
    }

    override fun resetBands() = manual { it.copy(bands = EqSettings.FLAT_BANDS) }

    override fun setBass(value: Int) = manual { it.copy(bassBoost = value.coerceIn(0, MAX_EFFECT)) }

    override fun setVirtualizer(value: Int) = manual { it.copy(virtualizer = value.coerceIn(0, MAX_EFFECT)) }

    override fun setLoudness(valueMb: Int) = manual { it.copy(loudnessMb = valueMb.coerceIn(0, MAX_LOUDNESS_MB)) }

    override fun setAutoTune(enabled: Boolean) = runtime.update { it.copy(autoTune = enabled) }

    override fun setHeadphone(id: String?) = runtime.update { it.copy(headphoneId = id) }

    override fun setHeadphoneAlways(always: Boolean) = runtime.update { it.copy(headphoneAlways = always) }

    override fun setHeadphoneStrength(percent: Int) = runtime.update { it.copy(headphoneStrength = percent.coerceIn(0, MAX_PERCENT)) }

    override fun applyPreset(genre: Genre) = runtime.update { it.applying(genre.toProfile(), TuneSource.PRESET) }

    override fun tuneNow() = runtime.tuneNow()

    override fun saveAi(config: AiConfig) = runtime.saveAi(config)

    override fun saveLastFm(config: LastFmConfig) = runtime.saveLastFm(config)

    override suspend fun loadModels(baseUrl: String): Result<List<ModelOption>> = runtime.loadModels(baseUrl)

    override suspend fun testLastFm(config: LastFmConfig): Result<List<String>> = runtime.testLastFm(config)

    override suspend fun testAi(config: AiConfig): Result<AiProfile> = runtime.testConnection(config)

    override fun clearHistory() = runtime.clearHistory()

    override fun openNotificationAccess() = system.openNotificationAccess()

    override fun openAppInfo() = system.openAppInfo()

    override fun openBatterySettings() = system.openBatterySettings()

    private companion object {
        const val MAX_PERCENT = 100
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
