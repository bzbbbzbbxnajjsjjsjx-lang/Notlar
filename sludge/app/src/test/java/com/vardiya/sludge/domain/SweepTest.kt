package com.vardiya.sludge.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SweepTest {
    private val kind = SweepKind.BASS

    @Test fun `the sweep runs from its start to its end frequency`() {
        assertEquals(20f, Sweep.frequencyAt(kind, 0f), 0.01f)
        assertEquals(200f, Sweep.frequencyAt(kind, kind.seconds), 0.1f)
        assertEquals(200f, Sweep.frequencyAt(kind, kind.seconds * 3), 0.1f)
        assertEquals(20f, Sweep.frequencyAt(kind, -5f), 0.01f)
    }

    @Test fun `every octave takes the same time`() {
        val oneOctave = kind.seconds / (Math.log(10.0) / Math.log(2.0)).toFloat()
        val a = Sweep.frequencyAt(kind, 4f)
        val b = Sweep.frequencyAt(kind, 4f + oneOctave)
        assertEquals(2f, b / a, 0.01f)
    }

    @Test fun `the full sweep covers the audible range`() {
        assertEquals(20f, SweepKind.FULL.fromHz, 0f)
        assertEquals(20_000f, Sweep.frequencyAt(SweepKind.FULL, SweepKind.FULL.seconds), 1f)
    }

    @Test fun `the tone is quiet and fades in and out`() {
        val out = ShortArray(Sweep.totalSamples(kind))
        Sweep.fill(kind, 0, 0.0, out)
        val limit = (Sweep.AMPLITUDE * 32_767).toInt() + 1
        assertTrue(out.all { abs(it.toInt()) <= limit })
        assertTrue(out.take(10).all { abs(it.toInt()) < 50 })
        assertTrue(out.takeLast(10).all { abs(it.toInt()) < 50 })
        assertTrue(out.drop(out.size / 2).take(2_000).any { abs(it.toInt()) > limit / 2 })
    }

    @Test fun `chunks join exactly as one long fill`() {
        val whole = ShortArray(9_600)
        Sweep.fill(kind, 0, 0.0, whole)
        val first = ShortArray(4_800)
        val second = ShortArray(4_800)
        val phase = Sweep.fill(kind, 0, 0.0, first)
        Sweep.fill(kind, 4_800, phase, second)
        assertEquals(whole.toList(), first.toList() + second.toList())
    }

    @Test fun `past the end it stays silent`() {
        val out = ShortArray(100) { 7 }
        Sweep.fill(kind, Sweep.totalSamples(kind) + 10, 0.0, out)
        assertTrue(out.all { it.toInt() == 0 })
    }

    @Test fun `the frequency really is what the sweep says at a given moment`() {
        // Count rising zero crossings over a short stretch about 10 s in and compare with the instantaneous frequency.
        val start = 10 * Sweep.SAMPLE_RATE
        val out = ShortArray(Sweep.SAMPLE_RATE / 2)
        Sweep.fill(kind, start, 0.0, out)
        val crossings = (1 until out.size).count { out[it - 1] < 0 && out[it] >= 0 }
        val expected = Sweep.frequencyAt(kind, 10.25f) * 0.5f
        assertEquals(expected, crossings.toFloat(), 1.5f)
    }
}

class TrimTest {
    @Test fun `the slider maps to half decibel steps and back`() {
        assertEquals(0, Trim.toSlider(-3f))
        assertEquals(Trim.SLIDER_MAX, Trim.toSlider(3f))
        assertEquals(60, Trim.toSlider(0f))
        (0..Trim.SLIDER_MAX step 10).forEach { assertEquals(it, Trim.toSlider(Trim.fromSlider(it))) }
        assertEquals(-1.5f, Trim.fromSlider(30), 0.001f)
    }

    @Test fun `trims are clamped`() {
        assertEquals(3f, Trim.clamp(9f), 0f)
        assertEquals(-3f, Trim.clamp(-9f), 0f)
        assertEquals(Trim.SLIDER_MAX, Trim.toSlider(50f))
    }

    @Test fun `a zero trim adds no filter and a trim adds a shelf`() {
        assertTrue(Trim.filters(0f, 0f).isEmpty())
        val f = Trim.filters(2f, -1.5f)
        assertEquals(listOf(FilterType.LOW_SHELF, FilterType.HIGH_SHELF), f.map { it.kind })
        assertEquals(listOf(2f, -1.5f), f.map { it.gainDb })
    }

    @Test fun `the bass trim moves the low end against the rest and the treble trim the top`() {
        val hz = listOf(50f, 1_000f, 14_000f)
        val flat = EqSettings(headphoneId = null).curveAt(OutputRoute.UNKNOWN, hz)
        val bass = EqSettings(headphoneId = null, trimBassDb = 3f).curveAt(OutputRoute.UNKNOWN, hz)
        val treble = EqSettings(headphoneId = null, trimTrebleDb = -3f).curveAt(OutputRoute.UNKNOWN, hz)
        assertTrue((bass[0] - bass[1]) > (flat[0] - flat[1]) + 2.5f)
        assertTrue((treble[2] - treble[1]) < (flat[2] - flat[1]) - 2.5f)
    }

    @Test fun `a preset keeps the person's trim`() {
        val s = EqSettings(trimBassDb = 1.5f, trimTrebleDb = -1f).applying(Genre.DOOM)
        assertEquals(1.5f, s.trimBassDb, 0f)
        assertEquals(-1f, s.trimTrebleDb, 0f)
    }

    @Test fun `a bass trim that boosts is paid for in headroom`() {
        val base = EqSettings(filters = Genre.DOOM.filters, headphoneId = null)
        assertTrue(base.copy(trimBassDb = 3f).headroomDb() > base.headroomDb())
    }
}
