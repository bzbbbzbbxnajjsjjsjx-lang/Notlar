package com.vardiya.sludge.app

import com.vardiya.sludge.domain.AiConfig
import com.vardiya.sludge.domain.AiProfile
import com.vardiya.sludge.domain.AutoTuner
import com.vardiya.sludge.domain.BAND_COUNT
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.ListenEntry
import com.vardiya.sludge.domain.NowPlaying
import com.vardiya.sludge.domain.ProfileSource
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
    override suspend fun settings() = settings
    override suspend fun aiConfig() = ai
    override suspend fun apply(profile: AiProfile, source: TuneSource) {
        applied += profile to source
    }
    override suspend fun record(entry: ListenEntry) {
        listens += entry
    }
}

private class SlowAi(private val millis: Long, private val answer: Result<AiProfile>) : ProfileSource {
    var calls = 0
    override suspend fun tune(config: AiConfig, track: TrackInfo): Result<AiProfile> {
        calls++
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
        val c = TuneCoordinator(backgroundScope, now, env, AutoTuner(MemoryStore(), ai), settle) { 1L }
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
        TuneCoordinator(backgroundScope, now, env, AutoTuner(MemoryStore(), ai), settle)
        now.value = NowPlaying(a, playing = false)
        advanceTimeBy(settle * 5)
        pass(PASS_MS)
        assertEquals(0, ai.calls)
        assertTrue(env.listens.isEmpty())
    }

    @Test fun `metadata that fills in later does not count the same track twice`() = runTest {
        val env = Env()
        val now = MutableStateFlow(NowPlaying())
        TuneCoordinator(backgroundScope, now, env, AutoTuner(MemoryStore(), SlowAi(10, Result.success(profile))), settle)
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
        TuneCoordinator(backgroundScope, now, env, AutoTuner(MemoryStore(), ai), settle)
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
        val c = TuneCoordinator(backgroundScope, MutableStateFlow(NowPlaying()), env, AutoTuner(store, ai), settle)
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
        val c = TuneCoordinator(backgroundScope, now, env, AutoTuner(MemoryStore(), ai), settle) { 1L }
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

    @Test fun `a paused newer track does not leave the status stuck on working`() = runTest {
        val env = Env()
        val now = MutableStateFlow(NowPlaying())
        val c = TuneCoordinator(backgroundScope, now, env, AutoTuner(MemoryStore(), SlowAi(1_000, Result.success(profile))), settle)
        now.value = playing(a)
        advanceTimeBy(settle + 50)
        runCurrent()
        now.value = NowPlaying(b, playing = false)
        pass(PASS_MS)
        assertEquals(TuneStatus.Idle, c.status.value)
    }

    @Test fun `a slider moved while the AI thinks is not undone by the automatic result`() = runTest {
        val env = Env()
        val ai = SlowAi(1_000, Result.success(profile))
        val now = MutableStateFlow(NowPlaying())
        TuneCoordinator(backgroundScope, now, env, AutoTuner(MemoryStore(), ai), settle)
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
        val c = TuneCoordinator(backgroundScope, MutableStateFlow(NowPlaying()), env, tuner, settle)
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
        val c = TuneCoordinator(backgroundScope, now, env, AutoTuner(MemoryStore(), failing), settle)
        now.value = playing(a)
        advanceTimeBy(settle + 50)
        pass(PASS_MS)
        assertEquals(TuneStatus.Idle, c.status.value)
        assertEquals(TuneSource.LOCAL, env.applied.single().second)
        c.tuneNow(b)
        pass(PASS_MS)
        assertEquals(TuneStatus.Failed("429"), c.status.value)
    }
}
