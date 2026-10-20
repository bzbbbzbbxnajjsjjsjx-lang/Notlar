package com.vardiya.sludge.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurveMathTest {
    @Test fun `millibels are clamped to the device range`() {
        assertEquals(1200, CurveMath.toMillibels(12f, -1500, 1500))
        assertEquals(1500, CurveMath.toMillibels(20f, -1500, 1500))
        assertEquals(-1500, CurveMath.toMillibels(-20f, -1500, 1500))
        assertEquals(350, CurveMath.toMillibels(3.5f, -1500, 1500))
    }

    @Test fun `loudness gives way to a hot curve`() {
        assertEquals(500, CurveMath.safeLoudness(listOf(6f, 0f, 0f, 0f, 0f), 500))
        assertEquals(300, CurveMath.safeLoudness(listOf(10f, 0f, 0f, 0f, 0f), 500))
        assertEquals(0, CurveMath.safeLoudness(listOf(12f, 0f, 0f, 0f, 0f), 100))
    }
}

class TuningTest {
    @Test fun `applying a preset sets filters, effects and name and keeps the switches`() {
        val s = EqSettings(enabled = false, headphoneStrength = 40).applying(Genre.DOOM)
        assertFalse(s.enabled)
        assertEquals(40, s.headphoneStrength)
        assertEquals(Genre.DOOM.filters, s.filters)
        assertEquals(Genre.DOOM.bass, s.bassBoost)
        assertEquals(Genre.DOOM.virtualizer, s.virtualizer)
        assertEquals(Genre.DOOM.loudness, s.loudnessMb)
        assertEquals(Genre.DOOM.label, s.genre)
        assertEquals(TuneSource.PRESET, s.source)
    }

    @Test fun `the flat preset clears the filters`() {
        val s = EqSettings(filters = Genre.DOOM.filters, bassBoost = 700).applying(Genre.FLAT)
        assertTrue(s.filters.isEmpty())
        assertEquals(0, s.bassBoost)
    }
}

class PresetTest {
    @Test fun `the doom preset lifts the bottom and cuts the top`() {
        val curve = Parametric.sample(Genre.DOOM.filters, listOf(62f, 8_000f, 16_000f))
        assertTrue(curve[0] > 5f)
        assertTrue(curve[1] < -1f && curve[2] < -1f)
    }

    @Test fun `flat has no filters`() {
        assertTrue(Genre.FLAT.filters.isEmpty())
    }

    @Test fun `no preset boosts beyond eight dB or uses more than the filter limit`() {
        Genre.entries.forEach { genre ->
            assertTrue(genre.name, genre.filters.size <= Parametric.MAX_FILTERS)
            assertTrue(genre.name, genre.filters.all { it == it.clamped() })
            assertTrue(genre.name, Parametric.sample(genre.filters, Parametric.ENGINE_HZ).all { it <= 8f })
        }
    }

    @Test fun `harsh noise keeps the upper mids and treble down and the loudness gain off`() {
        val curve = Parametric.sample(Genre.HARSH_NOISE.filters, listOf(2_000f, 4_000f, 8_000f, 16_000f))
        assertTrue(curve.all { it <= 0f })
        assertEquals(0, Genre.HARSH_NOISE.loudness)
    }

    @Test fun `every preset has a distinct filter set`() {
        val sets = Genre.entries.map { it.filters }
        assertEquals(sets.size, sets.toSet().size)
    }
}

class StatusTest {
    @Test fun `the status follows what is really true`() {
        assertEquals(Health.OFF, statusOf(enabled = false, sessions = 2).health)
        assertEquals(Health.ACTIVE, statusOf(enabled = true, sessions = 1).health)
        assertEquals(Health.WAITING, statusOf(enabled = true, sessions = 0).health)
    }
}

class HeadphonesTest {
    private val p = Headphones.JBL_TUNE_520BT

    @Test fun `the 520BT profile keeps AutoEq's ten parametric filters as published`() {
        assertEquals(10, p.filters.size)
        val first = p.filters.first()
        assertEquals(FilterType.LOW_SHELF, first.kind)
        assertEquals(105f, first.freqHz, 0f)
        assertEquals(8f, first.gainDb, 0f)
        assertEquals(0.7f, first.q, 0f)
        assertEquals(listOf(105f, 62f, 3_532f, 2_056f, 4_892f, 10_000f, 944f, 173f, 48f, 60f), p.filters.map { it.freqHz })
        assertTrue(p.filters.all { it == it.clamped() })
    }

    @Test fun `the filters reproduce the curve AutoEq published for this headphone`() {
        // Values from the parametric_eq column of AutoEq's JBL Tune 520BT.csv.
        val published = mapOf(
            20f to 6.40f, 31f to 4.29f, 62f to -0.34f, 125f to -0.51f,
            1_000f to -1.14f, 3_150f to -2.33f, 8_000f to 2.18f, 16_000f to 3.02f,
        )
        published.forEach { (hz, db) -> assertEquals("$hz Hz", db, p.rawAt(hz), 0.25f) }
    }

    @Test fun `boosts are capped but cuts are not`() {
        assertEquals(3f, p.appliedAt(20f), 0f)
        assertEquals(3f, p.appliedAt(16_000f), 0.05f)
        assertTrue(p.appliedAt(3_500f) < -3f)
        assertTrue(Parametric.GRID_HZ.all { p.appliedAt(it) <= HeadphoneProfile.DEFAULT_MAX_BOOST_DB })
    }

    @Test fun `the headroom is the boost that is really applied`() {
        assertEquals(-3f, p.preampDb(), 0.01f)
        assertEquals(-1.5f, p.preampDb(0.5f), 0.01f)
        assertEquals(0f, p.preampDb(0f), 0f)
    }

    @Test fun `the corrected curve never goes above zero`() {
        assertTrue(Parametric.GRID_HZ.all { p.correctionAt(it) <= 0.001f })
    }

    @Test fun `a lower strength scales the correction and costs less loudness`() {
        assertEquals(p.appliedAt(3_500f) / 2f, p.appliedAt(3_500f, 0.5f), 0.001f)
        assertTrue(p.preampDb(0.5f) > p.preampDb(1f))
        assertEquals(0f, p.correctionAt(1_000f, 0f), 0f)
    }

    @Test fun `it tames the 3 kHz peak this headphone measures and lifts the treble it lacks`() {
        assertTrue(p.correctionAt(3_500f) < p.correctionAt(1_000f) - 2f)
        assertTrue(p.correctionAt(14_000f) > p.correctionAt(1_000f) + 2f)
    }

    @Test fun `a fresh install starts with the default profile and an unknown id means none`() {
        assertEquals(Headphones.DEFAULT_ID, EqSettings().headphoneId)
        assertNotNull(Headphones.byId(Headphones.DEFAULT_ID))
        assertNull(Headphones.byId("nope"))
    }

    @Test fun `without a headphone the curve is only the filters`() {
        val s = EqSettings(filters = Genre.DOOM.filters, headphoneId = null)
        val hz = listOf(62f, 1_000f)
        assertEquals(Parametric.sample(Genre.DOOM.filters, hz), s.curveAt(OutputRoute.UNKNOWN, hz))
        assertEquals(listOf(0f, 0f), EqSettings(headphoneId = "nope").curveAt(OutputRoute.UNKNOWN, hz))
    }

    @Test fun `the correction is added on top of the person's filters`() {
        val hz = listOf(62f, 1_000f, 3_500f)
        val with = EqSettings(filters = Genre.DOOM.filters).curveAt(OutputRoute.UNKNOWN, hz)
        val without = EqSettings(filters = Genre.DOOM.filters, headphoneId = null).curveAt(OutputRoute.UNKNOWN, hz)
        hz.indices.forEach { assertEquals(without[it] + p.correctionAt(hz[it]), with[it], 0.001f) }
    }

    @Test fun `the strength setting scales what the equalizer is set to`() {
        val hz = listOf(3_500f)
        val full = EqSettings().curveAt(OutputRoute.UNKNOWN, hz).single()
        val half = EqSettings(headphoneStrength = 50).curveAt(OutputRoute.UNKNOWN, hz).single()
        val none = EqSettings(headphoneStrength = 0).curveAt(OutputRoute.UNKNOWN, hz).single()
        assertEquals(0f, none, 0f)
        assertTrue(full < half && half < none)
    }
}

class OutputRouteTest {
    private val profile = Headphones.JBL_TUNE_520BT
    private fun bt(vararg names: String) = OutputRoute(OutputKind.BLUETOOTH, names.toList())

    @Test fun `the correction is not used on the speaker or a wired headset`() {
        assertFalse(profile.appliesTo(OutputRoute(OutputKind.SPEAKER)))
        assertFalse(profile.appliesTo(OutputRoute(OutputKind.WIRED)))
    }

    @Test fun `the headphone is recognised by name however it spells itself`() {
        assertTrue(profile.appliesTo(bt("JBL Tune 520BT")))
        assertTrue(profile.appliesTo(bt("JBL TUNE520BT")))
        assertTrue(profile.appliesTo(bt("JBL T520BT")))
        assertTrue(profile.appliesTo(bt("Ahmet'in JBL Tune 520BT")))
    }

    @Test fun `another Bluetooth device does not get the correction`() {
        assertFalse(profile.appliesTo(bt("Sony WH-1000XM5")))
    }

    @Test fun `a Bluetooth output whose name cannot be read counts as a match`() {
        assertTrue(profile.appliesTo(bt("")))
        assertTrue(profile.appliesTo(bt()))
    }

    @Test fun `an unknown route applies the chosen correction`() {
        assertTrue(profile.appliesTo(OutputRoute.UNKNOWN))
    }

    private val hz = listOf(62f, 1_000f, 3_500f)

    @Test fun `the curve is only the filters when the correction is not active`() {
        val s = EqSettings(filters = Genre.DOOM.filters)
        val speaker = OutputRoute(OutputKind.SPEAKER)
        assertEquals(Parametric.sample(Genre.DOOM.filters, hz), s.curveAt(speaker, hz))
        assertFalse(s.correctionActive(speaker))
    }

    @Test fun `the correction is added when the headphone is connected`() {
        val s = EqSettings(filters = Genre.DOOM.filters)
        assertTrue(s.correctionActive(bt("JBL Tune 520BT")))
        val expected = Parametric.sample(Genre.DOOM.filters, hz).mapIndexed { i, db -> db + profile.correctionAt(hz[i]) }
        assertEquals(expected, s.curveAt(bt("JBL Tune 520BT"), hz))
    }

    @Test fun `forcing the correction applies it on any output`() {
        val s = EqSettings(filters = Genre.DOOM.filters, headphoneAlways = true)
        val expected = Parametric.sample(Genre.DOOM.filters, hz).mapIndexed { i, db -> db + profile.correctionAt(hz[i]) }
        assertEquals(expected, s.curveAt(OutputRoute(OutputKind.SPEAKER), hz))
    }

    @Test fun `without a chosen headphone nothing is active however the route looks`() {
        val s = EqSettings(headphoneId = null, headphoneAlways = true)
        assertFalse(s.correctionActive(bt("JBL Tune 520BT")))
    }

    @Test fun `stored headphone values read back as chosen`() {
        assertEquals(Headphones.DEFAULT_ID, Headphones.fromStored(null))
        assertNull(Headphones.fromStored(""))
        assertEquals("jbl_tune_520bt", Headphones.fromStored("jbl_tune_520bt"))
    }

    @Test fun `routes are named for the screen`() {
        assertEquals("telefon hoparlörü", OutputRoute(OutputKind.SPEAKER).label())
        assertEquals("JBL Tune 520BT", bt("JBL Tune 520BT").label())
        assertEquals("Bluetooth cihazı", bt("").label())
    }
}

class ShapingTest {
    @Test fun `the bass shelf is absent at zero and full below its corner at the top`() {
        assertNull(Bass.filter(0))
        val full = Bass.filter(MAX_EFFECT)!!
        assertEquals(FilterType.LOW_SHELF, full.kind)
        assertEquals(Bass.MAX_SHELF_DB, full.gainDb, 0f)
        assertEquals(Bass.MAX_SHELF_DB, Biquad.responseDb(full, 40f), 0.2f)
        assertEquals(0f, Biquad.responseDb(full, 2_000f), 0.2f)
    }

    @Test fun `the shelf scales with the knob and ignores out of range values`() {
        assertEquals(Bass.MAX_SHELF_DB / 2f, Bass.filter(MAX_EFFECT / 2)!!.gainDb, 0.001f)
        assertEquals(Bass.filter(MAX_EFFECT), Bass.filter(5_000))
        assertNull(Bass.filter(-50))
    }

    @Test fun `the bass knob lifts the low end of the curve and leaves the top alone`() {
        val off = EqSettings(headphoneId = null).curveAt(OutputRoute.UNKNOWN, listOf(50f, 5_000f))
        val on = EqSettings(headphoneId = null, bassBoost = 500).curveAt(OutputRoute.UNKNOWN, listOf(50f, 5_000f))
        assertTrue(on[0] > off[0] + 1.5f)
        assertEquals(off[1], on[1], 0.1f)
    }

    @Test fun `a boosted curve with the bass knob up is capped at the top of the range`() {
        val hot = List(5) { ParametricFilter(FilterType.PEAK.key, 100f, MAX_GAIN_DB, 1f) }
        val s = EqSettings(filters = hot, bassBoost = MAX_EFFECT, headphoneId = null)
        assertTrue(s.curveAt(OutputRoute.UNKNOWN, Parametric.ENGINE_HZ).all { it <= MAX_GAIN_DB })
    }

    @Test fun `the limiter's make-up gain is the loudness setting in decibels`() {
        assertEquals(0f, EqSettings(loudnessMb = 0).limiterGainDb(), 0f)
        assertEquals(3f, EqSettings(loudnessMb = 300).limiterGainDb(), 0.001f)
        assertEquals(10f, EqSettings(loudnessMb = 99_999).limiterGainDb(), 0.001f)
        assertEquals(0f, EqSettings(loudnessMb = -5).limiterGainDb(), 0f)
    }
}
