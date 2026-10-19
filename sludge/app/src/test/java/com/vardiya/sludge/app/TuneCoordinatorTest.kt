package com.vardiya.sludge.app

import com.vardiya.sludge.domain.AiConfig
import com.vardiya.sludge.domain.AiProfile
import com.vardiya.sludge.domain.AutoTuner
import com.vardiya.sludge.domain.BAND_COUNT
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.ListenEntry
import com.vardiya.sludge.domain.NowPlaying
import com.vardiya.sludge.domain.Genre
import com.vardiya.sludge.domain.ProfileSource
import com.vardiya.sludge.domain.TagSource
import com.vardiya.sludge.domain.TrackInfo
import com.vardiya.sludge.domain.TuneSource
import com.vardiya.sludge.domain.TuneStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private class Env(var settings: EqSettings = EqSettings(autoTune = true), var ai: AiConfig = AiConfig(apiKey = "sk-test")) : TuneEnvironment {
    val applied = mutableListOf<Pair<AiProfile, TuneSource>>()
    val listens = mutableListOf<ListenEntry>()
    var failRecordOnce = false
    var failApplyOnce = false
    override suspend fun settings() = settings
    override suspend fun aiConfig() = ai
    override suspend fun apply(profile: AiProfile, source: TuneSource) {
        if (failApplyOnce) {
            failApplyOnce = false
            throw IOException("disk")
        }
        applied += profile to source
    }
    override suspend fun record(entry: ListenEntry) {
        if (failRecordOnce) {
            failRecordOnce = false
            throw IOException("disk")
        }
        listens += entry
    }
}

private class SlowAi(private val millis: Long, private val answer: Result<AiProfile>) : ProfileSource {
    var calls = 0
    var lastTrack: TrackInfo? = null
    override suspend fun tune(config: AiConfig, track: TrackInfo): Result<AiProfile> {
        calls++
        lastTrack = track
        delay(millis)
        return answer
    }
}

private class MemoryStore : TuneStore {
    val data = mutableMapOf<String, AiProfile>()
    override suspend fun cached(key: String) = data[key]
    override suspend fun remember(key: String, profile: AiProfile) {
        data[key] = profile
    }
}

// Work started in backgroundScope is not waited for by advanceUntilIdle, so tests move the virtual clock explicitly.
private const val PASS_MS = 5_000L

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun TestScope.pass(millis: Long) {
    advanceTimeBy(millis)
    runCurrent()
}

@OptIn(ExperimentalCoroutinesApi::class)
class TuneCoordinatorTest {
    private val a = TrackInfo(title = "Dopesmoker", artist = "Sleep")
    private val b = TrackInfo(title = "Sleeping Giant", artist = "Earth")
    private val profile = AiProfile(genre = "Stoner", bands = List(BAND_COUNT) { 2f }, explanation = "ok")
    private val settle = 100L

    private fun playing(track: TrackInfo) = NowPlaying(track, playing = true)

    @Test fun `a playing track is tuned once and logged once`() = runTest {
        val env = Env()
        val ai = SlowAi(10, Result.success(profile))
        val now = MutableStateFlow(NowPlaying())
        val c = TuneCoordinator(backgroundScope, now, env, AutoTuner(MemoryStore(), ai), settleMillis = settle) { 1L }
        now.value = playing(a)
        advanceTimeBy(settle + 50)
        pass(PASS_MS)
        assertEquals(1, ai.calls)
        assertEquals(1, env.applied.size)
        assertEquals(listOf("Dopesmoker"), env.listens.map { it.title })
        assertEquals(TuneStatus.Idle, c.status.value)
    }

    @Test fun `a paused track is neither tuned nor logged`() = runTest {
        val env = Env()
        val ai = SlowAi(10, Result.success(profile))
        val now = MutableStateFlow(NowPlaying())
        TuneCoordinator(backgroundScope, now, env, AutoTuner(MemoryStore(), ai), settleMillis = settle)
        now.value = NowPlaying(a, playing = false)
        advanceTimeBy(settle * 5)
        pass(PASS_MS)
        assertEquals(0, ai.calls)
        assertTrue(env.listens.isEmpty())
    }

    @Test fun `metadata that fills in later does not count the same track twice`() = runTest {
        val env = Env()
        val now = MutableStateFlow(NowPlaying())
        TuneCoordinator(backgroundScope, now, env, AutoTuner(MemoryStore(), SlowAi(10, Result.success(profile))), settleMillis = settle)
        now.value = playing(a)
        advanceTimeBy(settle + 50)
        pass(PASS_MS)
        now.value = playing(a.copy(album = "Dopesmoker", genreTag = "Stoner Metal"))
        advanceTimeBy(settle * 3)
        pass(PASS_MS)
        assertEquals(1, env.listens.size)
    }

    @Test fun `with auto-tune off the listen is logged but nothing is tuned`() = runTest {
        val env = Env(settings = EqSettings(autoTune = false))
        val ai = SlowAi(10, Result.success(profile))
        val now = MutableStateFlow(NowPlaying())
        TuneCoordinator(backgroundScope, now, env, AutoTuner(MemoryStore(), ai), settleMillis = settle)
        now.value = playing(a)
        advanceTimeBy(settle + 50)
        pass(PASS_MS)
        assertEquals(0, ai.calls)
        assertEquals(1, env.listens.size)
    }

    @Test fun `the AI button tunes even with auto-tune off and asks again despite a remembered answer`() = runTest {
        val env = Env(settings = EqSettings(autoTune = false))
        val ai = SlowAi(10, Result.success(profile))
        val store = MemoryStore()
        val c = TuneCoordinator(backgroundScope, MutableStateFlow(NowPlaying()), env, AutoTuner(store, ai), settleMillis = settle)
        c.tuneNow(a)
        pass(PASS_MS)
        c.tuneNow(a)
        pass(PASS_MS)
        assertEquals(2, ai.calls)
        assertEquals(2, env.applied.size)
        assertTrue(env.listens.isEmpty())
    }

    @Test fun `a newer track replaces the one being tuned but the first listen is still logged`() = runTest {
        val env = Env()
        val ai = SlowAi(1_000, Result.success(profile))
        val now = MutableStateFlow(NowPlaying())
        val c = TuneCoordinator(backgroundScope, now, env, AutoTuner(MemoryStore(), ai), settleMillis = settle) { 1L }
        now.value = playing(a)
        advanceTimeBy(settle + 50)
        runCurrent()
        assertEquals(TuneStatus.Working, c.status.value)
        now.value = playing(b)
        advanceTimeBy(settle + 50)
        pass(PASS_MS)
        assertEquals(listOf("Dopesmoker", "Sleeping Giant"), env.listens.map { it.title })
        assertEquals(1, env.applied.size)
        assertEquals(TuneStatus.Idle, c.status.value)
    }

    @Test fun `a manual request replaces an automatic one in flight and the status ends idle`() = runTest {
        val env = Env()
        val now = MutableStateFlow(NowPlaying())
        val c = TuneCoordinator(backgroundScope, now, env, AutoTuner(MemoryStore(), SlowAi(1_000, Result.success(profile))), settleMillis = settle)
        now.value = playing(a)
        advanceTimeBy(settle + 50)
        runCurrent()
        assertEquals(TuneStatus.Working, c.status.value)
        c.tuneNow(b)
        pass(PASS_MS)
        assertEquals(TuneStatus.Idle, c.status.value)
        assertEquals(1, env.applied.size)
        assertEquals(listOf("Dopesmoker"), env.listens.map { it.title })
    }

    @Test fun `a failed log write does not stop the next track from being tuned and counted`() = runTest {
        val env = Env().apply { failRecordOnce = true }
        val now = MutableStateFlow(NowPlaying())
        TuneCoordinator(backgroundScope, now, env, AutoTuner(MemoryStore(), SlowAi(10, Result.success(profile))), settleMillis = settle)
        now.value = playing(a)
        pass(PASS_MS)
        now.value = playing(b)
        pass(PASS_MS)
        assertEquals(2, env.applied.size)
        assertEquals(listOf("Sleeping Giant"), env.listens.map { it.title })
    }

    @Test fun `a storage failure after the AI button says so and the pipeline keeps working`() = runTest {
        val env = Env().apply { failApplyOnce = true }
        val tuner = AutoTuner(MemoryStore(), SlowAi(10, Result.success(profile)))
        val c = TuneCoordinator(backgroundScope, MutableStateFlow(NowPlaying()), env, tuner, settleMillis = settle)
        c.tuneNow(a)
        pass(PASS_MS)
        assertTrue(c.status.value is TuneStatus.Failed)
        c.tuneNow(b)
        pass(PASS_MS)
        assertEquals(TuneStatus.Idle, c.status.value)
        assertEquals(1, env.applied.size)
    }

    @Test fun `a slider moved while the AI thinks is not undone by the automatic result`() = runTest {
        val env = Env()
        val ai = SlowAi(1_000, Result.success(profile))
        val now = MutableStateFlow(NowPlaying())
        TuneCoordinator(backgroundScope, now, env, AutoTuner(MemoryStore(), ai), settleMillis = settle)
        now.value = playing(a)
        advanceTimeBy(settle + 50)
        runCurrent()
        env.settings = env.settings.copy(bands = List(BAND_COUNT) { -3f })
        pass(PASS_MS)
        assertTrue(env.applied.isEmpty())
        assertEquals(1, env.listens.size)
    }

    @Test fun `a manual AI request applies even if the sliders moved meanwhile`() = runTest {
        val env = Env()
        val tuner = AutoTuner(MemoryStore(), SlowAi(1_000, Result.success(profile)))
        val c = TuneCoordinator(backgroundScope, MutableStateFlow(NowPlaying()), env, tuner, settleMillis = settle)
        c.tuneNow(a)
        runCurrent()
        env.settings = env.settings.copy(bands = List(BAND_COUNT) { -3f })
        pass(PASS_MS)
        assertEquals(1, env.applied.size)
    }

    @Test fun `an AI failure shows only for the AI button`() = runTest {
        val env = Env()
        val failing = SlowAi(10, Result.failure(IOException("429")))
        val now = MutableStateFlow(NowPlaying())
        val c = TuneCoordinator(backgroundScope, now, env, AutoTuner(MemoryStore(), failing), settleMillis = settle)
        now.value = playing(a)
        advanceTimeBy(settle + 50)
        pass(PASS_MS)
        assertEquals(TuneStatus.Idle, c.status.value)
        assertEquals(TuneSource.LOCAL, env.applied.single().second)
        c.tuneNow(b)
        pass(PASS_MS)
        assertEquals(TuneStatus.Failed("429"), c.status.value)
    }

    @Test fun `Last_fm tags go to the tuner after the player's own tag`() = runTest {
        val env = Env()
        val ai = SlowAi(10, Result.success(profile))
        val now = MutableStateFlow(NowPlaying())
        val c = TuneCoordinator(
            backgroundScope, now, env, AutoTuner(MemoryStore(), ai), TagSource { listOf("doom metal", "sludge") }, settle,
        )
        now.value = playing(a.copy(genreTag = "Rock"))
        pass(PASS_MS)
        assertEquals("Rock, doom metal, sludge", ai.lastTrack?.genreTag)
        assertEquals(listOf("doom metal", "sludge"), c.tags.value)
    }

    @Test fun `the listen is filed by the Last_fm tags even when nothing is tuned`() = runTest {
        val env = Env(settings = EqSettings(autoTune = false))
        val now = MutableStateFlow(NowPlaying())
        TuneCoordinator(
            backgroundScope, now, env, AutoTuner(MemoryStore(), SlowAi(10, Result.success(profile))),
            TagSource { listOf("doom metal") }, settle,
        )
        now.value = playing(TrackInfo(title = "Unknown", artist = "Nobody"))
        pass(PASS_MS)
        assertEquals(Genre.DOOM.name, env.listens.single().genre)
    }

    @Test fun `no tags found means the track goes on unchanged`() = runTest {
        val env = Env()
        val ai = SlowAi(10, Result.success(profile))
        val now = MutableStateFlow(NowPlaying())
        val c = TuneCoordinator(backgroundScope, now, env, AutoTuner(MemoryStore(), ai), TagSource { emptyList() }, settle)
        now.value = playing(a)
        pass(PASS_MS)
        assertEquals(a, ai.lastTrack)
        assertTrue(c.tags.value.isEmpty())
    }
}
