package com.vardiya.sludge.ui

import androidx.compose.runtime.Immutable
import com.vardiya.sludge.app.TuneStatus
import com.vardiya.sludge.domain.AiConfig
import com.vardiya.sludge.domain.AiProfile
import com.vardiya.sludge.domain.AttachedSession
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.Genre
import com.vardiya.sludge.domain.LastFmConfig
import com.vardiya.sludge.domain.ModelOption
import com.vardiya.sludge.domain.ListenSummary
import com.vardiya.sludge.domain.TrackInfo

enum class Tab(val title: String) {
    EQUALIZER("Ekolayzır"),
    STATS("Dinleme"),
    AI("Yapay Zekâ"),
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
    val ai: AiConfig = AiConfig(),
    val lastFm: LastFmConfig = LastFmConfig(),
    /** Genre tags Last.fm gave the current track. */
    val tags: List<String> = emptyList(),
    val sessions: List<AttachedSession> = emptyList(),
    val globalMixWorks: Boolean? = null,
    val accessGranted: Boolean = false,
    val listens: ListenSummary = ListenSummary(),
)

/** What the screens can ask for. Every member has a default so previews and screenshot tests pass [NoActions]. */
interface SludgeActions {
    fun setEnabled(enabled: Boolean) = Unit
    fun setBand(index: Int, db: Float) = Unit
    fun resetBands() = Unit
    fun setBass(value: Int) = Unit
    fun setVirtualizer(value: Int) = Unit
    fun setLoudness(valueMb: Int) = Unit
    fun setAutoTune(enabled: Boolean) = Unit
    fun setHeadphone(id: String?) = Unit
    fun applyPreset(genre: Genre) = Unit
    fun tuneNow() = Unit
    fun saveAi(config: AiConfig) = Unit
    fun saveLastFm(config: LastFmConfig) = Unit
    suspend fun loadModels(baseUrl: String): Result<List<ModelOption>> = Result.failure(UnsupportedOperationException())
    suspend fun testLastFm(config: LastFmConfig): Result<List<String>> = Result.failure(UnsupportedOperationException())
    suspend fun testAi(config: AiConfig): Result<AiProfile> = Result.failure(UnsupportedOperationException())
    fun clearHistory() = Unit
    fun openNotificationAccess() = Unit
    fun openAppInfo() = Unit
    fun openBatterySettings() = Unit
}

object NoActions : SludgeActions
