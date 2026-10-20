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
}

class TuningTest {
    @Test fun `applying a preset sets filters, effects and name and keeps the switches`() {
        val s = EqSettings(enabled = false, headphoneAlways = true).applying(Genre.DOOM)
        assertFalse(s.enabled)
        assertTrue(s.headphoneAlways)
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

    @Test fun `kyuss is a thick low end with a dark top`() {
        val c = Parametric.sample(Genre.KYUSS.filters, listOf(60f, 250f, 1_000f, 10_000f, 16_000f))
        assertTrue(c[0] > 5f)
        assertTrue(c[1] > 1f)
        assertTrue(c[3] < 0f && c[4] < c[3])
    }

    @Test fun `high rise cuts the mud and pushes the fuzz mids`() {
        val c = Parametric.sample(Genre.HIGH_RISE.filters, listOf(300f, 1_200f, 3_000f, 6_000f, 10_000f))
        assertTrue(c[0] < -1f)
        assertTrue(c[1] > 1.5f && c[2] > 1.5f)
        assertTrue(c[3] < 0f && c[4] < 0f)
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
    private val flatAt = listOf(25f, 105f, 1_000f, 3_500f, 14_000f)

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

    @Test fun `the correction alone needs no headroom, since its boost is capped at the slack`() {
        assertEquals(0f, EqSettings().headroomDb(), 0.01f)
        assertEquals(0f, EqSettings(headphoneId = null).headroomDb(), 0f)
    }

    @Test fun `the headroom follows the loudest point of everything together`() {
        val doom = EqSettings(filters = Genre.DOOM.filters, headphoneId = null)
        val peak = Parametric.ENGINE_HZ.filter { it >= HEADROOM_FROM_HZ }.maxOf { Parametric.responseDb(Genre.DOOM.filters, it) }
        assertEquals(peak - HEADROOM_SLACK_DB, doom.headroomDb(), 0.05f)
        assertTrue(EqSettings(filters = Genre.DOOM.filters, bassBoost = 800, headphoneId = null).headroomDb() > doom.headroomDb())
    }

    @Test fun `the bottom octave does not set the headroom but is still held to the slack`() {
        val sub = EqSettings(filters = listOf(ParametricFilter(FilterType.PEAK.key, 25f, 12f, 4f)), headphoneId = null)
        assertEquals(0f, sub.headroomDb(), 0.5f)
        val low = sub.curveAt(OutputRoute.UNKNOWN, listOf(20f, 25f, 31.5f))
        assertTrue(low.all { it <= HEADROOM_SLACK_DB + 0.001f })
    }

    @Test fun `from 40 Hz up the curve never goes above the slack whatever the preset or headphone`() {
        Genre.entries.forEach { genre ->
            listOf(null, Headphones.DEFAULT_ID).forEach { id ->
                val s = EqSettings(filters = genre.filters, bassBoost = genre.bass, headphoneId = id)
                val curve = s.curveAt(OutputRoute.UNKNOWN, Parametric.ENGINE_HZ)
                assertTrue("${genre.name} $id", curve.all { it <= HEADROOM_SLACK_DB + 0.001f })
            }
        }
    }

    @Test fun `the headroom does not change the shape, only the level`() {
        val s = EqSettings(filters = Genre.KYUSS.filters, headphoneId = null)
        val hz = listOf(60f, 1_000f)
        val shape = Parametric.sample(Genre.KYUSS.filters, hz)
        val curve = s.curveAt(OutputRoute.UNKNOWN, hz)
        assertEquals(shape[0] - shape[1], curve[0] - curve[1], 0.001f)
    }

    @Test fun `it tames the 3 kHz peak this headphone measures and lifts the treble it lacks`() {
        val curve = EqSettings().curveAt(OutputRoute.UNKNOWN, listOf(1_000f, 3_500f, 14_000f))
        assertTrue(curve[1] < curve[0] - 2f)
        assertTrue(curve[2] > curve[0] + 2f)
    }

    @Test fun `a fresh install starts with the default profile and an unknown id means none`() {
        assertEquals(Headphones.DEFAULT_ID, EqSettings().headphoneId)
        assertNotNull(Headphones.byId(Headphones.DEFAULT_ID))
        assertNull(Headphones.byId("nope"))
        assertEquals(flatAt.map { 0f }, EqSettings(headphoneId = "nope").curveAt(OutputRoute.UNKNOWN, flatAt))
    }

    @Test fun `the correction adds its shape on top of the person's filters`() {
        val hz = listOf(62f, 1_000f, 3_500f)
        val with = EqSettings(filters = Genre.DOOM.filters).curveAt(OutputRoute.UNKNOWN, hz)
        val without = EqSettings(filters = Genre.DOOM.filters, headphoneId = null).curveAt(OutputRoute.UNKNOWN, hz)
        val applied = hz.map { p.appliedAt(it) }
        assertEquals((without[0] - without[1]) + (applied[0] - applied[1]), with[0] - with[1], 0.001f)
        assertEquals((without[2] - without[1]) + (applied[2] - applied[1]), with[2] - with[1], 0.001f)
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
    private val speaker = OutputRoute(OutputKind.SPEAKER)

    private fun differences(curve: List<Float>) = listOf(curve[0] - curve[1], curve[2] - curve[1])

    private fun withoutCorrection() = EqSettings(filters = Genre.DOOM.filters, headphoneId = null).curveAt(OutputRoute.UNKNOWN, hz)

    @Test fun `the curve is only the filters when the correction is not active`() {
        val s = EqSettings(filters = Genre.DOOM.filters)
        assertFalse(s.correctionActive(speaker))
        assertEquals(withoutCorrection(), s.curveAt(speaker, hz))
    }

    @Test fun `the correction is added when the headphone is connected`() {
        val s = EqSettings(filters = Genre.DOOM.filters)
        assertTrue(s.correctionActive(bt("JBL Tune 520BT")))
        val plain = differences(withoutCorrection())
        val corrected = differences(s.curveAt(bt("JBL Tune 520BT"), hz))
        assertTrue(corrected[1] < plain[1] - 2f)
    }

    @Test fun `forcing the correction applies it on any output`() {
        val s = EqSettings(filters = Genre.DOOM.filters, headphoneAlways = true)
        assertEquals(s.curveAt(bt("JBL Tune 520BT"), hz), s.curveAt(speaker, hz))
        assertTrue(s.correctionActive(speaker))
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

    @Test fun `the bass knob lifts the low end against the rest of the curve`() {
        val hz = listOf(50f, 5_000f)
        val off = EqSettings(headphoneId = null).curveAt(OutputRoute.UNKNOWN, hz)
        val on = EqSettings(headphoneId = null, bassBoost = 500).curveAt(OutputRoute.UNKNOWN, hz)
        assertTrue((on[0] - on[1]) > (off[0] - off[1]) + 1.5f)
    }

    @Test fun `a very hot curve is brought down to the slack at its peak`() {
        val hot = List(5) { ParametricFilter(FilterType.PEAK.key, 100f, MAX_GAIN_DB, 1f) }
        val s = EqSettings(filters = hot, bassBoost = MAX_EFFECT, headphoneId = null)
        val curve = s.curveAt(OutputRoute.UNKNOWN, Parametric.ENGINE_HZ)
        val above = Parametric.ENGINE_HZ.indices.filter { Parametric.ENGINE_HZ[it] >= HEADROOM_FROM_HZ }
        assertTrue(above.all { curve[it] <= HEADROOM_SLACK_DB + 0.05f })
        assertTrue(s.headroomDb() > MAX_GAIN_DB)
    }

    @Test fun `the limiter's make-up gain is the loudness setting in decibels`() {
        assertEquals(0f, EqSettings(loudnessMb = 0).limiterGainDb(), 0f)
        assertEquals(3f, EqSettings(loudnessMb = 300).limiterGainDb(), 0.001f)
        assertEquals(10f, EqSettings(loudnessMb = 99_999).limiterGainDb(), 0.001f)
        assertEquals(0f, EqSettings(loudnessMb = -5).limiterGainDb(), 0f)
    }
}

class EngineCurveTest {
    @Test fun `the loudness is part of the band gain before the limiter`() {
        val s = EqSettings(filters = Genre.DOOM.filters, loudnessMb = 400, headphoneId = null)
        val hz = Parametric.ENGINE_HZ
        val plain = s.curveAt(OutputRoute.UNKNOWN, hz)
        val engine = s.engineCurve(OutputRoute.UNKNOWN, hz)
        hz.indices.forEach { assertEquals(plain[it] + 4f, engine[it], 0.001f) }
    }

    @Test fun `no loudness leaves the curve as it is`() {
        val s = EqSettings(filters = Genre.KYUSS.filters, headphoneId = null)
        assertEquals(s.curveAt(OutputRoute.UNKNOWN, Parametric.ENGINE_HZ), s.engineCurve(OutputRoute.UNKNOWN, Parametric.ENGINE_HZ))
    }

    @Test fun `the strongest setting still peaks at the slack plus the loudness at most`() {
        val s = EqSettings(filters = Genre.DOOM.filters, bassBoost = MAX_EFFECT, loudnessMb = MAX_LOUDNESS_MB, trimBassDb = 3f)
        val engine = s.engineCurve(OutputRoute.UNKNOWN, Parametric.ENGINE_HZ)
        assertTrue(engine.all { it <= HEADROOM_SLACK_DB + s.limiterGainDb() + 0.001f })
    }

    @Test fun `a narrow peak between two band centres is not paid for in headroom`() {
        val between = EqSettings(filters = listOf(ParametricFilter(FilterType.PEAK.key, 1_130f, 12f, 10f)), headphoneId = null)
        val engine = between.curveAt(OutputRoute.UNKNOWN, Parametric.ENGINE_HZ)
        assertTrue(engine.all { it <= HEADROOM_SLACK_DB + 0.001f })
        assertTrue(between.headroomDb() < 9f)
    }

    @Test fun `a preset is recognised only when everything about it matches`() {
        val s = EqSettings().applying(Genre.DOOM)
        assertTrue(s.isPreset(Genre.DOOM))
        assertFalse(s.copy(loudnessMb = s.loudnessMb + 100).isPreset(Genre.DOOM))
        assertFalse(s.isPreset(Genre.STONER))
    }
}

class SessionsTest {
    private fun session(pkg: String, failed: Boolean = false, path: String = "DynamicsProcessing") = AttachedSession(1, pkg, failed, path)

    @Test fun `only a working player session counts as processing`() {
        val sessions = listOf(
            session("com.spotify.music"),
            session("com.other", failed = true, path = ""),
            session("com.virt", path = ""),
            session(TONE_PACKAGE),
        )
        assertEquals(listOf("com.spotify.music"), sessions.processing().map { it.packageName })
    }

    @Test fun `no sessions means nothing is processing`() {
        assertTrue(emptyList<AttachedSession>().processing().isEmpty())
    }
}
