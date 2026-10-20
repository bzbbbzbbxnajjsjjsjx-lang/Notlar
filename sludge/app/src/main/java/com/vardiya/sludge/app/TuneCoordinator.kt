package com.vardiya.sludge.app

import android.util.Log
import com.vardiya.sludge.domain.ListenEntry
import com.vardiya.sludge.domain.NowPlaying
import com.vardiya.sludge.domain.TrackInfo
import com.vardiya.sludge.domain.TuneSource
import com.vardiya.sludge.domain.GenreDetector
import com.vardiya.sludge.domain.localTuning
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Follows the player. A new track that has been playing for a moment is tuned to its genre's preset (when auto-tune is
 * on) and counted in the listening log. A newer track replaces one still being tuned.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class TuneCoordinator(
    scope: CoroutineScope,
    nowPlaying: Flow<NowPlaying>,
    private val env: TuneEnvironment,
    settleMillis: Long = TRACK_SETTLE_MS,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _status = MutableStateFlow<TuneStatus>(TuneStatus.Idle)
    val status: StateFlow<TuneStatus> = _status.asStateFlow()

    init {
        // Only a track that is really playing counts, and it counts once however its metadata fills in later.
        scope.launch {
            nowPlaying.filter { it.playing }.mapNotNull { it.track }
                .distinctUntilChangedBy { GenreDetector.normalize("${it.artist}|${it.title}") }
                .debounce(settleMillis)
                .collectLatest(::handle)
        }
    }

    private suspend fun handle(track: TrackInfo) {
        try {
            if (env.settings().autoTune) tuneAndApply(track)
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // A storage hiccup must not stop the pipeline for every later track.
            Log.w(TAG, "tuning failed: ${e.javaClass.simpleName}")
        } finally {
            withContext(NonCancellable) {
                // The listen is counted even when a newer track cancelled the tuning or the tuning failed.
                record(track)
                if (_status.value == TuneStatus.Working) _status.value = TuneStatus.Idle
            }
        }
    }

    private suspend fun tuneAndApply(track: TrackInfo) {
        _status.value = TuneStatus.Working
        env.apply(localTuning(track), TuneSource.LOCAL)
        _status.value = TuneStatus.Idle
    }

    /** One more play in the stats log, filed under the genre the detector found. */
    private suspend fun record(track: TrackInfo) {
        val bucket = GenreDetector.detect(track).genre
        try {
            env.record(ListenEntry(clock(), track.artist, track.title, bucket.name))
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // Losing one log line is fine; ending the pipeline for every later track is not.
            Log.w(TAG, "listen not saved: ${e.javaClass.simpleName}")
        }
    }

    private companion object {
        const val TAG = "Sludge"
        const val TRACK_SETTLE_MS = 1_500L
    }
}
