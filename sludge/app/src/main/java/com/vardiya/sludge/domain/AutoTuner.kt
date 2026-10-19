package com.vardiya.sludge.domain

/** Where tuning results are remembered, so replaying a record costs no second request. */
interface TuneStore {
    suspend fun cached(key: String): AiProfile?
    suspend fun remember(key: String, profile: AiProfile)
}

/** Whatever answers "what settings suit this track": the AI client in the app, a fake in tests. */
fun interface ProfileSource {
    suspend fun tune(config: AiConfig, track: TrackInfo): Result<AiProfile>
}

/** The decision for one track. [failure] is set when the AI was tried and failed, so the local guess was used. */
data class TuneOutcome(val profile: AiProfile, val source: TuneSource, val failure: String? = null)

/**
 * The tuning policy, with no Android in it: a remembered answer first, then the AI when a key is set, and the offline
 * genre guess when there is no key or the AI cannot be reached. It never throws for an AI failure.
 */
class AutoTuner(private val store: TuneStore, private val ai: ProfileSource) {
    suspend fun tune(track: TrackInfo, config: AiConfig): TuneOutcome {
        val key = TuneCache.key(track)
        store.cached(key)?.let { return TuneOutcome(it, TuneSource.AI) }
        if (!config.isUsable) return local(track)
        return ai.tune(config, track).fold(
            onSuccess = {
                store.remember(key, it)
                TuneOutcome(it, TuneSource.AI)
            },
            // Offline or over quota: the local guess still beats leaving the previous track's curve on.
            onFailure = { local(track).copy(failure = it.message.orEmpty()) },
        )
    }

    private fun local(track: TrackInfo) = TuneOutcome(localProfile(track), TuneSource.LOCAL)
}
