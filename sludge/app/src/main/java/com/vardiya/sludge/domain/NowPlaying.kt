package com.vardiya.sludge.domain

/** What is playing and whether it is actually running (a paused track is still shown, but is not a listen). */
data class NowPlaying(val track: TrackInfo? = null, val playing: Boolean = false)

/** One player's media session as the listener sees it. */
data class SessionSnapshot(val track: TrackInfo?, val playing: Boolean)

/** The session to follow: a playing one wins, otherwise the first one that knows a track. */
fun pickCurrent(sessions: List<SessionSnapshot>): NowPlaying {
    val chosen = sessions.firstOrNull { it.playing && it.track != null } ?: sessions.firstOrNull { it.track != null }
    return NowPlaying(chosen?.track, chosen?.playing == true)
}
