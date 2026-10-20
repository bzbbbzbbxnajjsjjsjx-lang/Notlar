package com.vardiya.sludge.ui

import androidx.compose.runtime.Immutable
import com.vardiya.sludge.domain.AttachedSession
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.Genre
import com.vardiya.sludge.domain.OutputRoute
import com.vardiya.sludge.domain.SweepKind
import com.vardiya.sludge.domain.ToneState
import com.vardiya.sludge.domain.ParametricFilter

enum class Tab(val title: String) {
    EQUALIZER("Ekolayzır"),
    SETUP("Kurulum"),
}

/** Everything a screen draws. */
@Immutable
data class UiState(
    val settings: EqSettings = EqSettings(),
    /** Where the sound is going: the headphone correction is only used on the headphone it was measured for. */
    val route: OutputRoute = OutputRoute.UNKNOWN,
    val sessions: List<AttachedSession> = emptyList(),
    val globalMixWorks: Boolean? = null,
    val tone: ToneState = ToneState(),
)

/** What the screens can ask for. Every member has a default so previews and screenshot tests pass [NoActions]. */
interface SludgeActions {
    fun setEnabled(enabled: Boolean) = Unit
    fun addFilter() = Unit
    fun updateFilter(index: Int, filter: ParametricFilter) = Unit
    fun removeFilter(index: Int) = Unit
    fun setBass(value: Int) = Unit
    fun setVirtualizer(value: Int) = Unit
    fun setLoudness(valueMb: Int) = Unit
    fun setHeadphone(id: String?) = Unit
    fun setHeadphoneAlways(always: Boolean) = Unit
    fun applyPreset(genre: Genre) = Unit
    fun setTrimBass(db: Float) = Unit
    fun setTrimTreble(db: Float) = Unit
    fun startTone(kind: SweepKind) = Unit
    fun stopTone() = Unit
    fun openAppInfo() = Unit
    fun openBatterySettings() = Unit
}

object NoActions : SludgeActions
