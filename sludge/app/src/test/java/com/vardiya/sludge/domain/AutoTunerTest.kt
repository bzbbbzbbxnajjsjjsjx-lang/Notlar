package com.vardiya.sludge.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private class FakeStore(val data: MutableMap<String, AiProfile> = mutableMapOf()) : TuneStore {
    override suspend fun cached(key: String) = data[key]

    override suspend fun remember(key: String, profile: AiProfile) {
        data[key] = profile
    }
}

private class FakeAi(private val answer: Result<AiProfile>) : ProfileSource {
    var calls = 0
    override suspend fun tune(config: AiConfig, track: TrackInfo): Result<AiProfile> {
        calls++
        return answer
    }
}

class AutoTunerTest {
    private val track = TrackInfo(title = "Dopesmoker", artist = "Sleep")
    private val usable = AiConfig(apiKey = "sk-test")
    private val aiProfile = AiProfile(genre = "Stoner", bands = List(BAND_COUNT) { 1f }, explanation = "ok")

    private fun run(tuner: AutoTuner, config: AiConfig = usable) = runBlocking { tuner.tune(track, config) }

    @Test fun `a remembered answer is used and the AI is not asked`() {
        val ai = FakeAi(Result.success(aiProfile))
        val store = FakeStore(mutableMapOf(TuneCache.key(track) to aiProfile))
        val outcome = run(AutoTuner(store, ai))
        assertEquals(aiProfile, outcome.profile)
        assertEquals(TuneSource.AI, outcome.source)
        assertEquals(0, ai.calls)
    }

    @Test fun `without a key the offline guess is used and the AI is not asked`() {
        val ai = FakeAi(Result.success(aiProfile))
        val outcome = run(AutoTuner(FakeStore(), ai), AiConfig(apiKey = ""))
        assertEquals(TuneSource.LOCAL, outcome.source)
        assertEquals(Genre.STONER.label, outcome.profile.genre)
        assertNull(outcome.failure)
        assertEquals(0, ai.calls)
    }

    @Test fun `an AI answer is remembered`() {
        val store = FakeStore()
        val outcome = run(AutoTuner(store, FakeAi(Result.success(aiProfile))))
        assertEquals(TuneSource.AI, outcome.source)
        assertEquals(aiProfile, store.data[TuneCache.key(track)])
    }

    @Test fun `an AI failure falls back to the offline guess and says why`() {
        val store = FakeStore()
        val outcome = run(AutoTuner(store, FakeAi(Result.failure(IOException("İstek sınırına takıldı (429)")))))
        assertEquals(TuneSource.LOCAL, outcome.source)
        assertEquals("İstek sınırına takıldı (429)", outcome.failure)
        assertTrue(store.data.isEmpty())
    }

    @Test fun `the second request for the same track is served from memory`() {
        val ai = FakeAi(Result.success(aiProfile))
        val tuner = AutoTuner(FakeStore(), ai)
        run(tuner)
        run(tuner)
        assertEquals(1, ai.calls)
    }
}

class PickCurrentTest {
    private val a = TrackInfo(title = "A", artist = "x")
    private val b = TrackInfo(title = "B", artist = "y")

    @Test fun `no sessions means nothing is playing`() {
        assertEquals(NowPlaying(), pickCurrent(emptyList()))
    }

    @Test fun `a playing session wins over a paused one`() {
        val picked = pickCurrent(listOf(SessionSnapshot(a, playing = false), SessionSnapshot(b, playing = true)))
        assertEquals(b, picked.track)
        assertTrue(picked.playing)
    }

    @Test fun `a paused track is still shown but is not playing`() {
        val picked = pickCurrent(listOf(SessionSnapshot(a, playing = false)))
        assertEquals(a, picked.track)
        assertFalse(picked.playing)
    }

    @Test fun `sessions without a track are skipped`() {
        val picked = pickCurrent(listOf(SessionSnapshot(null, playing = true), SessionSnapshot(b, playing = false)))
        assertEquals(b, picked.track)
        assertNotNull(picked.track)
    }
}
