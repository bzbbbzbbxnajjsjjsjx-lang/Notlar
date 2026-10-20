package com.vardiya.sludge.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParametricTest {
    private fun peak(hz: Float, gain: Float, q: Float = 1f) = ParametricFilter("peak", hz, gain, q)

    @Test fun `peak has its gain at the centre and none far away`() {
        val f = peak(1_000f, 6f, 2f)
        assertEquals(6f, Biquad.responseDb(f, 1_000f), 0.05f)
        assertEquals(0f, Biquad.responseDb(f, 40f), 0.2f)
        assertEquals(0f, Biquad.responseDb(f, 16_000f), 0.5f)
    }

    @Test fun `a cut is negative at the centre`() {
        assertEquals(-9f, Biquad.responseDb(peak(3_000f, -9f, 4f), 3_000f), 0.05f)
    }

    @Test fun `higher q is narrower`() {
        val wide = Biquad.responseDb(peak(1_000f, 6f, 0.5f), 1_600f)
        val narrow = Biquad.responseDb(peak(1_000f, 6f, 6f), 1_600f)
        assertTrue(wide > narrow + 1f)
    }

    @Test fun `shelves reach their gain on their side`() {
        val low = ParametricFilter("lowshelf", 200f, 5f, 0.7f)
        val high = ParametricFilter("highshelf", 4_000f, -4f, 0.7f)
        assertEquals(5f, Biquad.responseDb(low, 20f), 0.2f)
        assertEquals(0f, Biquad.responseDb(low, 10_000f), 0.2f)
        assertEquals(-4f, Biquad.responseDb(high, 20_000f), 0.3f)
        assertEquals(0f, Biquad.responseDb(high, 100f), 0.2f)
    }

    @Test fun `filters add in dB`() {
        val a = peak(500f, 3f)
        val b = peak(500f, 2f)
        assertEquals(Biquad.responseDb(a, 500f) + Biquad.responseDb(b, 500f), Parametric.responseDb(listOf(a, b), 500f), 0.001f)
        assertEquals(0f, Parametric.responseDb(emptyList(), 500f), 0f)
    }

    @Test fun `values are clamped and names read leniently`() {
        val f = ParametricFilter("Low Shelf", 5f, 40f, 99f).clamped()
        assertEquals(FilterType.LOW_SHELF, f.kind)
        assertEquals(20f, f.freqHz, 0f)
        assertEquals(12f, f.gainDb, 0f)
        assertEquals(10f, f.q, 0f)
        assertEquals(FilterType.PEAK, ParametricFilter("nonsense").kind)
    }

    @Test fun `engine bands cover 20 Hz to 20 kHz in order`() {
        val hz = Parametric.ENGINE_HZ
        val edges = Parametric.ENGINE_EDGES_HZ
        assertEquals(31, hz.size)
        assertEquals(hz.size, edges.size)
        assertTrue(hz.zipWithNext().all { (a, b) -> a < b })
        assertTrue(edges.zipWithNext().all { (a, b) -> a < b })
        assertTrue(hz.indices.all { hz[it] < edges[it] })
        assertTrue(edges.last() < 24_000f)
    }

    @Test fun `parametric curve follows the filters and still gets the bass shelf`() {
        val s = EqSettings(filters = listOf(peak(1_000f, 6f, 2f)), headphoneId = null, bassBoost = 1000)
        val curve = s.curveAt(OutputRoute.UNKNOWN, listOf(31f, 1_000f))
        assertEquals(6f, curve[1], 0.2f)
        assertTrue(curve[0] > 2f)
    }

    @Test fun `curve never exceeds twelve dB`() {
        val s = EqSettings(filters = List(5) { peak(1_000f, 12f, 1f) }, headphoneId = null)
        assertTrue(s.curveAt(OutputRoute.UNKNOWN, Parametric.ENGINE_HZ).all { it <= 12f })
    }

    @Test fun `bands become peak filters and flat bands none`() {
        assertTrue(Parametric.fromBands(EqSettings.FLAT_BANDS).isEmpty())
        val filters = Parametric.fromBands(List(BAND_COUNT) { if (it == 2) 4f else 0f })
        assertEquals(1, filters.size)
        assertEquals(BAND_HZ[2], filters[0].freqHz, 0f)
        assertEquals(4f, filters[0].gainDb, 0f)
    }

    @Test fun `applying a preset puts its filters and bands in the settings`() {
        val s = EqSettings().applying(Genre.DOOM.toTuning(), TuneSource.PRESET)
        assertEquals(Genre.DOOM.filters, s.filters)
        assertEquals(Genre.DOOM.bands, s.bands)
    }

    @Test fun `a preset in parametric mode shapes the sound through its filters`() {
        val s = EqSettings(headphoneId = null).applying(Genre.DOOM.toTuning(), TuneSource.PRESET)
        val curve = s.curveAt(OutputRoute.UNKNOWN, listOf(90f, 8_000f))
        assertTrue(curve[0] > 4f)
        assertTrue(curve[1] < -1f)
    }

    @Test fun `slider scales round-trip and hit their ends`() {
        assertEquals(0, FilterScale.freqToSlider(20f))
        assertEquals(FilterScale.MAX, FilterScale.freqToSlider(20_000f))
        assertEquals(1_000f, FilterScale.sliderToFreq(FilterScale.freqToSlider(1_000f)), 20f)
        assertEquals(2.5f, FilterScale.sliderToQ(FilterScale.qToSlider(2.5f)), 0.1f)
        assertEquals(0, FilterScale.gainToSlider(-12f))
        assertEquals(FilterScale.GAIN_MAX, FilterScale.gainToSlider(12f))
        assertEquals(3f, FilterScale.sliderToGain(FilterScale.gainToSlider(3f)), 0.05f)
    }
}
