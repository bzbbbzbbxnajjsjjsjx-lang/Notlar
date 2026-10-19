package com.vardiya.sludge.app

import android.util.Log
import com.vardiya.sludge.domain.AiProfile
import com.vardiya.sludge.domain.AutoTuner
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.ListenEntry
import com.vardiya.sludge.domain.ListenStats
import com.vardiya.sludge.domain.NowPlaying
import com.vardiya.sludge.domain.TrackInfo
import com.vardiya.sludge.domain.TuneCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Follows the player. A new track that has been playing for a moment is tuned (when auto-tune is on) and counted in
 * the listening log; the AI button adds a manual request. Both kinds go through one pipeline, so only one request is
 * ever in flight and a newer one replaces an older one.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class TuneCoordinator(
    scope: CoroutineScope,
    nowPlaying: Flow<NowPlaying>,
    private val env: TuneEnvironment,
    private val tuner: AutoTuner,
    settleMillis: Long = TRACK_SETTLE_MS,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private class Request(val track: TrackInfo, val manual: Boolean)

    private val _status = MutableStateFlow<TuneStatus>(TuneStatus.Idle)
    val status: StateFlow<TuneStatus> = _status.asStateFlow()

    // Buffered, so a press that comes before the pipeline is listening is not lost; only the newest press is kept.
    private val manualRequests = Channel<TrackInfo>(Channel.CONFLATED)

    init {
        // Only a track that is really playing counts, and it counts once however its metadata fills in later.
        val automatic = nowPlaying.filter { it.playing }.mapNotNull { it.track }
            .distinctUntilChangedBy { TuneCache.key(it) }
            .debounce(settleMillis)
            .map { Request(it, manual = false) }
        val manual = manualRequests.receiveAsFlow().map { Request(it, manual = true) }
        scope.launch { merge(automatic, manual).collectLatest(::handle) }
    }

    /** The AI button: tunes for [track] even when auto-tune is off, and asks the AI again even if it answered before. */
    fun tuneNow(track: TrackInfo) {
        manualRequests.trySend(track)
    }

    private suspend fun handle(request: Request) {
        var genre = ""
        try {
            if (request.manual || env.settings().autoTune) genre = tuneAndApply(request).genre
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // A storage hiccup must not stop the pipeline for every later track.
            Log.w(TAG, "tuning failed: ${e.javaClass.simpleName}")
        } finally {
            withContext(NonCancellable) {
                // The listen is counted even when a newer track cancelled the tuning or the tuning failed.
                if (!request.manual) record(request.track, genre)
                if (_status.value == TuneStatus.Working) _status.value = TuneStatus.Idle
            }
        }
    }

    private suspend fun tuneAndApply(request: Request): AiProfile {
        val before = env.settings()
        _status.value = TuneStatus.Working
        val outcome = tuner.tune(request.track, env.aiConfig(), refresh = request.manual)
        // If a slider moved while the AI was thinking, an automatic result must not undo it.
        if (request.manual || !soundChanged(before, env.settings())) env.apply(outcome.profile, outcome.source)
        _status.value = if (request.manual && outcome.failure != null) TuneStatus.Failed(outcome.failure) else TuneStatus.Idle
        return outcome.profile
    }

    private fun soundChanged(a: EqSettings, b: EqSettings) =
        a.bands != b.bands || a.bassBoost != b.bassBoost || a.virtualizer != b.virtualizer || a.loudnessMb != b.loudnessMb

    /** One more play in the stats log, filed under the genre the tuner named or, failing that, the offline guess. */
    private suspend fun record(track: TrackInfo, genreText: String) {
        val bucket = ListenStats.bucket(genreText, track)
        env.record(ListenEntry(clock(), track.artist, track.title, bucket.name))
    }

    private companion object {
        const val TAG = "Sludge"
        const val TRACK_SETTLE_MS = 1_500L
    }
}
