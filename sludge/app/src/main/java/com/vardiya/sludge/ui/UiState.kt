package com.vardiya.sludge.ui

import androidx.compose.runtime.Immutable
import com.vardiya.sludge.app.TuneStatus
import com.vardiya.sludge.domain.AttachedSession
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.Genre
import com.vardiya.sludge.domain.OutputRoute
import com.vardiya.sludge.domain.ParametricFilter
import com.vardiya.sludge.domain.ListenSummary
import com.vardiya.sludge.domain.TrackInfo

enum class Tab(val title: String) {
    EQUALIZER("Ekolayzır"),
    STATS("Dinleme"),
    SETUP("Kurulum"),
}

/** Everything a screen draws. */
@Immutable
data class UiState(
    val settings: EqSettings = EqSettings(),
    val track: TrackInfo? = null,
    /** False while the player is paused: the track is still shown, but it is not being listened to. */
    val playing: Boolean = false,
    val status: TuneStatus = TuneStatus.Idle,
    /** Where the sound is going: the headphone correction is only used on the headphone it was measured for. */
    val route: OutputRoute = OutputRoute.UNKNOWN,
    val sessions: List<AttachedSession> = emptyList(),
    val globalMixWorks: Boolean? = null,
    val accessGranted: Boolean = false,
    val listens: ListenSummary = ListenSummary(),
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
    fun setAutoTune(enabled: Boolean) = Unit
    fun setHeadphone(id: String?) = Unit
    fun setHeadphoneAlways(always: Boolean) = Unit
    fun setHeadphoneStrength(percent: Int) = Unit
    fun applyPreset(genre: Genre) = Unit
    fun clearHistory() = Unit
    fun openNotificationAccess() = Unit
    fun openAppInfo() = Unit
    fun openBatterySettings() = Unit
}

object NoActions : SludgeActions
