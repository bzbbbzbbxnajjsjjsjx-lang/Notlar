package com.vardiya.sludge.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vardiya.sludge.app.SludgeRuntime
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.Genre
import com.vardiya.sludge.domain.MAX_EFFECT
import com.vardiya.sludge.domain.MAX_LOUDNESS_MB
import com.vardiya.sludge.domain.Parametric
import com.vardiya.sludge.domain.ParametricFilter
import com.vardiya.sludge.domain.TuneSource
import com.vardiya.sludge.domain.applying
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

// Every member is one entry of SludgeActions; the logic itself lives in the runtime and in domain/.
@Suppress("TooManyFunctions")
class SludgeViewModel(private val app: Application) : AndroidViewModel(app), SludgeActions {
    private val runtime = SludgeRuntime.get(app)
    private val system = SystemSettings(app)

    val state: StateFlow<UiState> = combine(runtime.settings, runtime.engineState, runtime.route) { settings, engine, route ->
        UiState(settings = settings, route = route, sessions = engine.sessions, globalMixWorks = engine.globalMixWorks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState())

    private fun manual(transform: (EqSettings) -> EqSettings) = runtime.update { transform(it).copy(source = TuneSource.MANUAL) }

    override fun setEnabled(enabled: Boolean) = runtime.update { it.copy(enabled = enabled) }

    override fun addFilter() = manual { s ->
        if (s.filters.size >= Parametric.MAX_FILTERS) s else s.copy(filters = s.filters + ParametricFilter(gainDb = 3f))
    }

    override fun updateFilter(index: Int, filter: ParametricFilter) = manual { s ->
        s.copy(filters = s.filters.mapIndexed { i, old -> if (i == index) filter.clamped() else old })
    }

    override fun removeFilter(index: Int) = manual { s -> s.copy(filters = s.filters.filterIndexed { i, _ -> i != index }) }

    override fun setBass(value: Int) = manual { it.copy(bassBoost = value.coerceIn(0, MAX_EFFECT)) }

    override fun setVirtualizer(value: Int) = manual { it.copy(virtualizer = value.coerceIn(0, MAX_EFFECT)) }

    override fun setLoudness(valueMb: Int) = manual { it.copy(loudnessMb = valueMb.coerceIn(0, MAX_LOUDNESS_MB)) }

    override fun setHeadphone(id: String?) = runtime.update { it.copy(headphoneId = id) }

    override fun setHeadphoneAlways(always: Boolean) = runtime.update { it.copy(headphoneAlways = always) }

    override fun setHeadphoneStrength(percent: Int) = runtime.update { it.copy(headphoneStrength = percent.coerceIn(0, MAX_PERCENT)) }

    override fun applyPreset(genre: Genre) = runtime.update { it.applying(genre) }

    override fun openAppInfo() = system.openAppInfo()

    override fun openBatterySettings() = system.openBatterySettings()

    private companion object {
        const val MAX_PERCENT = 100
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
