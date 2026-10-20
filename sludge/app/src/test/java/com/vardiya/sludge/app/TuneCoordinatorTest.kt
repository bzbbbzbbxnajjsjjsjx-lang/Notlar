package com.vardiya.sludge.app

import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.Genre
import com.vardiya.sludge.domain.ListenEntry
import com.vardiya.sludge.domain.NowPlaying
import com.vardiya.sludge.domain.TrackInfo
import com.vardiya.sludge.domain.TuneSource
import com.vardiya.sludge.domain.Tuning
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private class Env(var settings: EqSettings = EqSettings(autoTune = true), private val applyMillis: Long = 0) : TuneEnvironment {
    val applied = mutableListOf<Pair<Tuning, TuneSource>>()
    val listens = mutableListOf<ListenEntry>()
    var failRecordOnce = false
    var failApplyOnce = false
    override suspend fun settings() = settings
    override suspend fun apply(tuning: Tuning, source: TuneSource) {
        delay(applyMillis)
        if (failApplyOnce) {
            failApplyOnce = false
            throw IOException("disk")
        }
        applied += tuning to source
    }
    override suspend fun record(entry: ListenEntry) {
        if (failRecordOnce) {
            failRecordOnce = false
            throw IOException("disk")
        }
        listens += entry
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
    private val settle = 100L

    private fun playing(track: TrackInfo) = NowPlaying(track, playing = true)

    @Test fun `a playing track gets its genre's preset once and is logged once`() = runTest {
        val env = Env()
        val now = MutableStateFlow(NowPlaying())
        val c = TuneCoordinator(backgroundScope, now, env, settleMillis = settle) { 1L }
        now.value = playing(a)
        advanceTimeBy(settle + 50)
        pass(PASS_MS)
        val (tuning, source) = env.applied.single()
        assertEquals(Genre.STONER.filters, tuning.filters)
        assertEquals(TuneSource.LOCAL, source)
        assertEquals(listOf("Dopesmoker"), env.listens.map { it.title })
        assertEquals(Genre.STONER.name, env.listens.single().genre)
        assertEquals(TuneStatus.Idle, c.status.value)
    }

    @Test fun `a paused track is neither tuned nor logged`() = runTest {
        val env = Env()
        val now = MutableStateFlow(NowPlaying())
        TuneCoordinator(backgroundScope, now, env, settleMillis = settle)
        now.value = NowPlaying(a, playing = false)
        advanceTimeBy(settle * 5)
        pass(PASS_MS)
        assertTrue(env.applied.isEmpty())
        assertTrue(env.listens.isEmpty())
    }

    @Test fun `metadata that fills in later does not count the same track twice`() = runTest {
        val env = Env()
        val now = MutableStateFlow(NowPlaying())
        TuneCoordinator(backgroundScope, now, env, settleMillis = settle)
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
        val now = MutableStateFlow(NowPlaying())
        TuneCoordinator(backgroundScope, now, env, settleMillis = settle)
        now.value = playing(a)
        advanceTimeBy(settle + 50)
        pass(PASS_MS)
        assertTrue(env.applied.isEmpty())
        assertEquals(1, env.listens.size)
    }

    @Test fun `an unknown artist gets the flat preset but is still logged`() = runTest {
        val env = Env()
        val now = MutableStateFlow(NowPlaying())
        TuneCoordinator(backgroundScope, now, env, settleMillis = settle)
        now.value = playing(TrackInfo(title = "Unknown", artist = "Nobody"))
        pass(PASS_MS)
        assertTrue(env.applied.single().first.filters.isEmpty())
        assertEquals(Genre.FLAT.name, env.listens.single().genre)
    }

    @Test fun `a newer track replaces the one being tuned but the first listen is still logged`() = runTest {
        val env = Env(applyMillis = 1_000)
        val now = MutableStateFlow(NowPlaying())
        val c = TuneCoordinator(backgroundScope, now, env, settleMillis = settle) { 1L }
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

    @Test fun `a failed log write does not stop the next track from being tuned and counted`() = runTest {
        val env = Env().apply { failRecordOnce = true }
        val now = MutableStateFlow(NowPlaying())
        TuneCoordinator(backgroundScope, now, env, settleMillis = settle)
        now.value = playing(a)
        pass(PASS_MS)
        now.value = playing(b)
        pass(PASS_MS)
        assertEquals(2, env.applied.size)
        assertEquals(listOf("Sleeping Giant"), env.listens.map { it.title })
    }

    @Test fun `a storage failure on one track does not stop the next from being tuned`() = runTest {
        val env = Env().apply { failApplyOnce = true }
        val now = MutableStateFlow(NowPlaying())
        TuneCoordinator(backgroundScope, now, env, settleMillis = settle)
        now.value = playing(a)
        pass(PASS_MS)
        now.value = playing(b)
        pass(PASS_MS)
        assertEquals(1, env.applied.size)
        assertEquals(2, env.listens.size)
    }
}
