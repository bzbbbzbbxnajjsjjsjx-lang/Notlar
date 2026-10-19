package com.vardiya.sludge.app

import com.vardiya.sludge.data.SettingsRepository
import com.vardiya.sludge.domain.AiConfig
import com.vardiya.sludge.domain.AiProfile
import com.vardiya.sludge.domain.AutoTuner
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.ListenEntry
import com.vardiya.sludge.domain.ListenStats
import com.vardiya.sludge.domain.NowPlaying
import com.vardiya.sludge.domain.TrackInfo
import com.vardiya.sludge.domain.applying
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Follows the player: when a new track has been playing for a moment it is tuned (if auto-tune is on) and counted in
 * the listening log. The AI button goes through [tuneNow].
 */
@OptIn(FlowPreview::class)
class TuneCoordinator(
    private val scope: CoroutineScope,
    nowPlaying: Flow<NowPlaying>,
    private val settings: StateFlow<EqSettings>,
    private val config: StateFlow<AiConfig>,
    private val tuner: AutoTuner,
    private val repo: SettingsRepository,
) {
    private val _status = MutableStateFlow<TuneStatus>(TuneStatus.Idle)
    val status: StateFlow<TuneStatus> = _status.asStateFlow()

    init {
        scope.launch {
            // Only a track that is really playing counts; a paused one is neither tuned nor logged.
            nowPlaying.filter { it.playing }.map { it.track }.distinctUntilChanged().debounce(TRACK_SETTLE_MS)
                .collectLatest { track ->
                    if (track == null) return@collectLatest
                    val profile = if (settings.value.autoTune) tuneAndApply(track, manual = false) else null
                    record(track, profile?.genre.orEmpty())
                }
        }
    }

    /** The AI button: tunes for [track] even when auto-tune is off. */
    fun tuneNow(track: TrackInfo) {
        scope.launch { tuneAndApply(track, manual = true) }
    }

    private suspend fun tuneAndApply(track: TrackInfo, manual: Boolean): AiProfile {
        _status.value = TuneStatus.Working
        val outcome = tuner.tune(track, config.value)
        repo.updateSettings { it.applying(outcome.profile, outcome.source) }
        _status.value = if (manual && outcome.failure != null) TuneStatus.Failed(outcome.failure) else TuneStatus.Idle
        return outcome.profile
    }

    /** One more play in the stats log, filed under the genre the tuner named or, failing that, the offline guess. */
    private suspend fun record(track: TrackInfo, genreText: String) {
        val bucket = ListenStats.bucket(genreText, track)
        repo.addListen(ListenEntry(System.currentTimeMillis(), track.artist, track.title, bucket.name))
    }

    private companion object {
        const val TRACK_SETTLE_MS = 1_500L
    }
}
