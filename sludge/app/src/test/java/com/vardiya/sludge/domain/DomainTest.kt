package com.vardiya.sludge.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurveMathTest {
    @Test fun `device bands at the canonical frequencies read the canonical gains`() {
        val bands = listOf(5f, 4f, 3f, 1f, -1f, -1f, 0f, 1f, 2f, -1f)
        assertEquals(bands, CurveMath.mapToDevice(bands, BAND_HZ))
    }

    @Test fun `in between frequencies interpolate on a log axis`() {
        val bands = listOf(0f, 10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f)
        val mid = CurveMath.mapToDevice(bands, listOf(Math.sqrt(31.0 * 62.0).toFloat())).single()
        assertEquals(5f, mid, 0.05f)
    }

    @Test fun `frequencies outside the range hold the end values`() {
        val bands = listOf(4f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, -6f)
        assertEquals(listOf(4f, -6f), CurveMath.mapToDevice(bands, listOf(20f, 20_000f)))
    }

    @Test fun `a three band device still gets a sensible curve`() {
        val out = CurveMath.mapToDevice(Genre.DOOM.bands, listOf(100f, 1_000f, 10_000f))
        assertTrue(out[0] > out[1] && out[0] > out[2])
    }

    @Test fun `millibels are clamped to the device range`() {
        assertEquals(1200, CurveMath.toMillibels(12f, -1500, 1500))
        assertEquals(1500, CurveMath.toMillibels(20f, -1500, 1500))
        assertEquals(-1500, CurveMath.toMillibels(-20f, -1500, 1500))
        assertEquals(350, CurveMath.toMillibels(3.5f, -1500, 1500))
    }

    @Test fun `snap rounds to half decibels`() {
        assertEquals(2.5f, CurveMath.snap(2.4f), 0f)
        assertEquals(-3f, CurveMath.snap(-3.1f), 0f)
    }

    @Test fun `loudness gives way to a hot curve`() {
        assertEquals(500, CurveMath.safeLoudness(listOf(6f, 0f, 0f, 0f, 0f), 500))
        assertEquals(300, CurveMath.safeLoudness(listOf(10f, 0f, 0f, 0f, 0f), 500))
        assertEquals(0, CurveMath.safeLoudness(listOf(12f, 0f, 0f, 0f, 0f), 100))
    }
}

class GenreDetectorTest {
    private fun detect(artist: String = "", title: String = "", album: String = "", tag: String = "") =
        GenreDetector.detect(TrackInfo(title = title, artist = artist, album = album, genreTag = tag))

    @Test fun `known artists are recognised`() {
        assertEquals(Genre.STONER, detect(artist = "Kyuss").genre)
        assertEquals(Genre.DOOM, detect(artist = "Electric Wizard").genre)
        assertEquals(Genre.DRONE, detect(artist = "Yellow Swans").genre)
        assertEquals(Genre.HARSH_NOISE, detect(artist = "Merzbow").genre)
        assertEquals(Genre.NOISE_ROCK, detect(artist = "The Jesus Lizard").genre)
    }

    @Test fun `upper case Turkish letters do not break the match`() {
        assertEquals(Genre.STONER, detect(artist = "KYUSS").genre)
        assertEquals(Genre.DOOM, detect(artist = "ELECTRIC WIZARD").genre)
    }

    @Test fun `an artist must match exactly, not by containing the name`() {
        assertEquals(Genre.FLAT, detect(artist = "Earth Crisis").genre)
        assertEquals(Genre.FLAT, detect(artist = "Tomorrow").genre)
    }

    @Test fun `credits with several artists are split`() {
        assertEquals(Genre.DRONE, detect(artist = "Boris & Sunn O)))").genre)
        assertEquals(Genre.STONER, detect(artist = "Clutch feat. Someone").genre)
    }

    @Test fun `the genre tag decides when the artist is unknown`() {
        val guess = detect(artist = "Nobody", tag = "Stoner Rock")
        assertEquals(Genre.STONER, guess.genre)
        assertTrue(guess.confident)
        assertEquals(Genre.HARSH_NOISE, detect(artist = "Nobody", tag = "Harsh Noise Wall").genre)
        assertEquals(Genre.NOISE_ROCK, detect(artist = "Nobody", tag = "Noise Rock").genre)
    }

    @Test fun `words in the title or album give an unsure guess`() {
        val guess = detect(artist = "Nobody", album = "Funeral Doom Rituals")
        assertEquals(Genre.DOOM, guess.genre)
        assertFalse(guess.confident)
    }

    @Test fun `nothing known falls back to flat`() {
        assertEquals(Genre.FLAT, detect(artist = "Sezen Aksu", title = "Hadi Bakalım").genre)
    }

    @Test fun `harsh noise never boosts the harsh range or the loudness`() {
        val preset = Genre.HARSH_NOISE
        assertTrue(preset.bands[6] <= 0 && preset.bands[7] < 0 && preset.bands[8] < 0 && preset.bands[9] < 0)
        assertEquals(0, preset.loudness)
    }

    @Test fun `every preset stays inside the safe limits`() {
        for (genre in Genre.entries) {
            assertEquals(BAND_COUNT, genre.bands.size)
            assertTrue(genre.bands.all { it in -MAX_GAIN_DB..8f })
            assertTrue(genre.bass in 0..MAX_EFFECT && genre.virtualizer in 0..MAX_EFFECT && genre.loudness in 0..MAX_LOUDNESS_MB)
        }
    }
}

class TuningTest {
    @Test fun `local tuning explains itself`() {
        val t = localTuning(TrackInfo(title = "x", artist = "Sleep"))
        assertEquals(Genre.STONER.bands, t.bands)
        assertEquals(Genre.STONER.filters, t.filters)
        assertTrue(t.explanation.contains("Stoner"))
    }

    @Test fun `an unknown track is flat and says so`() {
        val t = localTuning(TrackInfo(title = "x", artist = "Nobody"))
        assertEquals(Genre.FLAT.label, t.genre)
        assertTrue(t.explanation.contains("belirlenemedi"))
    }

    @Test fun `applying a tuning keeps the switches`() {
        val s = EqSettings(enabled = false, autoTune = false).applying(Genre.DOOM.toTuning(), TuneSource.LOCAL)
        assertFalse(s.enabled)
        assertFalse(s.autoTune)
        assertEquals(Genre.DOOM.bands, s.bands)
        assertEquals(Genre.DOOM.filters, s.filters)
        assertEquals(TuneSource.LOCAL, s.source)
    }
}

class ListenStatsTest {
    private val zone = java.time.ZoneOffset.UTC
    private val now = java.time.LocalDate.of(2026, 10, 19).atTime(12, 0).toInstant(zone).toEpochMilli()
    private val day = 24L * 60 * 60 * 1000

    private fun entry(daysAgo: Int, artist: String, genre: Genre) = ListenEntry(now - daysAgo * day, artist, "t", genre.name)

    @Test fun `plays are counted per genre, largest first`() {
        val log = listOf(
            entry(0, "Sleep", Genre.STONER), entry(1, "Sleep", Genre.STONER), entry(1, "Earth", Genre.DRONE),
        )
        val s = ListenStats.summarize(log, now, zone)
        assertEquals(3, s.total)
        assertEquals(listOf(Genre.STONER, Genre.DRONE), s.genres.map { it.genre })
        assertEquals(2f / 3, s.genres.first().fraction, 0.001f)
        assertEquals("Sleep" to 2, s.topArtists.first())
    }

    @Test fun `entries older than the window are left out`() {
        val log = listOf(entry(0, "A", Genre.DOOM), entry(45, "B", Genre.DRONE))
        assertEquals(1, ListenStats.summarize(log, now, zone).total)
    }

    @Test fun `the week has seven days ending today`() {
        val s = ListenStats.summarize(listOf(entry(0, "A", Genre.DOOM), entry(0, "A", Genre.DOOM), entry(6, "A", Genre.DOOM)), now, zone)
        assertEquals(7, s.days.size)
        assertEquals(2, s.days.last().count)
        assertEquals(1, s.days.first().count)
        assertEquals(java.time.LocalDate.of(2026, 10, 19), s.days.last().date)
    }

    @Test fun `an empty log gives an empty summary`() {
        val s = ListenStats.summarize(emptyList(), now, zone)
        assertEquals(0, s.total)
        assertTrue(s.genres.isEmpty())
        assertEquals(7, s.days.size)
    }

    @Test fun `unknown genre names fall into other`() {
        val s = ListenStats.summarize(listOf(ListenEntry(now, "A", "t", "NOPE")), now, zone)
        assertEquals(Genre.FLAT, s.genres.single().genre)
        assertEquals("Diğer", Genre.FLAT.chartLabel)
    }

    @Test fun `the log keeps only the newest entries`() {
        var log = emptyList<ListenEntry>()
        for (i in 1..5) log = ListenStats.append(log, ListenEntry(i.toLong(), "A", "t$i", Genre.DOOM.name), max = 3)
        assertEquals(listOf(3L, 4L, 5L), log.map { it.at })
    }
}

class PresetTest {
    @Test fun `a preset's bands are its filters read on the ten bands`() {
        assertEquals(BAND_COUNT, Genre.DOOM.bands.size)
        assertEquals(CurveMath.snap(Parametric.responseDb(Genre.DOOM.filters, BAND_HZ[1])), Genre.DOOM.bands[1], 0f)
        // The doom low shelf lifts the bottom well above the treble, which is cut.
        assertTrue(Genre.DOOM.bands[1] > 5f)
        assertTrue(Genre.DOOM.bands.last() < 0f)
    }

    @Test fun `flat stays flat`() {
        assertEquals(List(BAND_COUNT) { 0f }, Genre.FLAT.bands)
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

    @Test fun `every preset has a distinct curve`() {
        val curves = Genre.entries.map { it.bands }
        assertEquals(curves.size, curves.toSet().size)
    }
}

class StatusTest {
    @Test fun `the status follows what is really true`() {
        assertEquals(Health.OFF, statusOf(enabled = false, accessGranted = true, sessions = 2).health)
        assertEquals(Health.ACTIVE, statusOf(enabled = true, accessGranted = false, sessions = 1).health)
        assertEquals(Health.ACTIVE, statusOf(enabled = true, accessGranted = true, sessions = 1).health)
        assertEquals(Health.NEEDS_SETUP, statusOf(enabled = true, accessGranted = false, sessions = 0).health)
        assertEquals(Health.WAITING, statusOf(enabled = true, accessGranted = true, sessions = 0).health)
    }
}

class HeadphonesTest {
    private val p = Headphones.JBL_TUNE_520BT

    @Test fun `the 520BT profile keeps AutoEq's ten fixed-band gains as measured`() {
        assertEquals(BAND_COUNT, p.correction.size)
        assertEquals(listOf(6.6f, -2.9f, -0.3f, -0.4f, 0.6f, -1.5f, 2.1f, -1.9f, 2.8f, 5.2f), p.correction)
    }

    @Test fun `boosts are capped but cuts are not`() {
        val applied = p.appliedCorrection()
        assertEquals(3f, applied[0], 0f)
        assertEquals(3f, applied[9], 0f)
        assertEquals(-2.9f, applied[1], 0f)
        assertEquals(2.1f, applied[6], 0f)
        assertTrue(applied.all { it <= HeadphoneProfile.DEFAULT_MAX_BOOST_DB })
    }

    @Test fun `the headroom is the boost that is really applied`() {
        assertEquals(-3f, p.preampDb(), 0f)
        assertEquals(-1.5f, p.preampDb(0.5f), 0.001f)
        assertEquals(0f, p.preampDb(0f), 0f)
    }

    @Test fun `a flat curve through the profile is the capped correction plus the headroom, never above zero`() {
        val out = p.applyTo(EqSettings.FLAT_BANDS)
        assertEquals(0f, out[0], 0.001f)
        assertEquals(-2.9f - 3f, out[1], 0.001f)
        assertEquals(0f, out[9], 0.001f)
        assertTrue(out.all { it <= 0.001f })
    }

    @Test fun `the person's curve is kept and the correction added on top`() {
        val bands = Genre.STONER.bands
        val out = p.applyTo(bands)
        bands.indices.forEach { assertEquals(bands[it] + p.appliedCorrection()[it] + p.preampDb(), out[it], 0.001f) }
    }

    @Test fun `a strength of zero leaves the curve alone and a lower strength costs less loudness`() {
        assertEquals(Genre.DOOM.bands, p.applyTo(Genre.DOOM.bands, 0f))
        assertTrue(p.preampDb(0.5f) > p.preampDb(1f))
        assertEquals(p.appliedCorrection()[1] / 2f, p.appliedCorrection(0.5f)[1], 0.001f)
    }

    @Test fun `without a headphone the bands are used as they are`() {
        val s = EqSettings(bands = Genre.DOOM.bands, headphoneId = null)
        assertEquals(Genre.DOOM.bands, s.effectiveBands())
    }

    @Test fun `the strength setting scales what the equalizer is set to`() {
        val full = EqSettings(bands = Genre.DOOM.bands).effectiveBands()
        val half = EqSettings(bands = Genre.DOOM.bands, headphoneStrength = 50).effectiveBands()
        val none = EqSettings(bands = Genre.DOOM.bands, headphoneStrength = 0).effectiveBands()
        assertEquals(Genre.DOOM.bands, none)
        assertTrue(half[1] > full[1])
    }

    @Test fun `a fresh install starts with the default profile and an unknown id means none`() {
        assertEquals(Headphones.DEFAULT_ID, EqSettings().headphoneId)
        assertNotNull(Headphones.byId(Headphones.DEFAULT_ID))
        assertNull(Headphones.byId("nope"))
        assertEquals(EqSettings.FLAT_BANDS, EqSettings(headphoneId = "nope").effectiveBands())
    }

    @Test fun `a boosted curve through the profile still maps to the device bands`() {
        val effective = EqSettings(bands = Genre.STONER.bands).effectiveBands()
        val device = CurveMath.mapToDevice(effective, listOf(60f, 230f, 910f, 3_600f, 14_000f))
        assertEquals(5, device.size)
        assertTrue(device.all { it.isFinite() })
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

    @Test fun `the bands pass through unchanged when the correction is not active`() {
        val s = EqSettings(bands = Genre.DOOM.bands)
        assertEquals(Genre.DOOM.bands, s.effectiveBands(OutputRoute(OutputKind.SPEAKER)))
        assertFalse(s.correctionActive(OutputRoute(OutputKind.SPEAKER)))
    }

    @Test fun `the correction is added when the headphone is connected`() {
        val s = EqSettings(bands = Genre.DOOM.bands)
        assertTrue(s.correctionActive(bt("JBL Tune 520BT")))
        assertEquals(profile.applyTo(Genre.DOOM.bands), s.effectiveBands(bt("JBL Tune 520BT")))
    }

    @Test fun `forcing the correction applies it on any output`() {
        val s = EqSettings(bands = Genre.DOOM.bands, headphoneAlways = true)
        assertEquals(profile.applyTo(Genre.DOOM.bands), s.effectiveBands(OutputRoute(OutputKind.SPEAKER)))
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

class GenreEvidenceTest {
    private fun tags(vararg t: String, artist: String = "Nobody") = GenreDetector.detect(TrackInfo(artist = artist, genreTag = t.joinToString(", ")))

    @Test fun `another scene's tag does not name a metal genre`() {
        assertEquals(Genre.FLAT, tags("noise pop").genre)
        assertEquals(Genre.FLAT, tags("ambient pop").genre)
        assertEquals(Genre.FLAT, tags("psychedelic pop").genre)
        assertEquals(Genre.FLAT, tags("ambient house", "dream pop").genre)
    }

    @Test fun `a whole noise or ambient tag still counts`() {
        assertEquals(Genre.HARSH_NOISE, tags("noise").genre)
        assertEquals(Genre.DRONE, tags("ambient", "experimental").genre)
    }

    @Test fun `a pop tag first does not hide a doom tag after it`() {
        assertEquals(Genre.DOOM, tags("noise pop", "doom metal").genre)
    }

    @Test fun `the tags come before the artist list`() {
        assertEquals(Genre.DOOM, tags("doom metal", artist = "Kyuss").genre)
        assertEquals(Genre.STONER, GenreDetector.detect(TrackInfo(artist = "Kyuss", genreTag = "rock, seen live")).genre)
    }

    @Test fun `words in the album still help when there are no tags`() {
        val g = GenreDetector.detect(TrackInfo(artist = "Nobody", album = "Funeral Doom Rituals"))
        assertEquals(Genre.DOOM, g.genre)
        assertFalse(g.confident)
    }
}

class ShapingTest {
    @Test fun `the bass shelf is empty at zero and full at the lowest bands at the top`() {
        assertEquals(List(BAND_COUNT) { 0f }, Bass.shelf(0))
        val full = Bass.shelf(MAX_EFFECT)
        assertEquals(Bass.MAX_SHELF_DB, full[0], 0.001f)
        assertEquals(Bass.MAX_SHELF_DB, full[1], 0.001f)
        assertEquals(0f, full[5], 0f)
        assertEquals(0f, full[9], 0f)
    }

    @Test fun `the shelf tapers from the lowest band upward and never goes negative`() {
        val shelf = Bass.shelf(600)
        for (i in 1 until shelf.size) assertTrue(shelf[i] <= shelf[i - 1])
        assertTrue(shelf.all { it >= 0f })
    }

    @Test fun `the shelf scales with the knob and ignores out of range values`() {
        assertEquals(Bass.shelf(MAX_EFFECT)[0] / 2f, Bass.shelf(MAX_EFFECT / 2)[0], 0.001f)
        assertEquals(Bass.shelf(MAX_EFFECT), Bass.shelf(5_000))
        assertEquals(Bass.shelf(0), Bass.shelf(-50))
    }

    @Test fun `the band edges sit between neighbours and end at 20 kHz`() {
        assertEquals(BAND_COUNT, BAND_EDGES_HZ.size)
        assertEquals(20_000f, BAND_EDGES_HZ.last(), 0f)
        for (i in 0 until BAND_COUNT - 1) assertTrue(BAND_EDGES_HZ[i] > BAND_HZ[i] && BAND_EDGES_HZ[i] < BAND_HZ[i + 1])
        assertEquals(Math.sqrt(31.0 * 62.0).toFloat(), BAND_EDGES_HZ[0], 0.01f)
        assertEquals(BAND_EDGES_HZ.sorted(), BAND_EDGES_HZ)
    }

    @Test fun `the final bands add the shelf to the headphone-corrected curve`() {
        val s = EqSettings(bands = Genre.DOOM.bands, bassBoost = 500, headphoneId = null)
        val shelf = Bass.shelf(500)
        val final = s.finalBands()
        Genre.DOOM.bands.indices.forEach { assertEquals(Genre.DOOM.bands[it] + shelf[it], final[it], 0.001f) }
    }

    @Test fun `with the bass knob down the final bands are the effective bands`() {
        val s = EqSettings(bands = Genre.STONER.bands, bassBoost = 0)
        assertEquals(s.effectiveBands(), s.finalBands())
    }

    @Test fun `a bass boost on an already boosted curve is capped at the top of the range`() {
        val s = EqSettings(bands = List(BAND_COUNT) { MAX_GAIN_DB }, bassBoost = MAX_EFFECT, headphoneId = null)
        assertTrue(s.finalBands().all { it <= MAX_GAIN_DB })
    }

    @Test fun `the limiter's make-up gain is the loudness setting in decibels`() {
        assertEquals(0f, EqSettings(loudnessMb = 0).limiterGainDb(), 0f)
        assertEquals(3f, EqSettings(loudnessMb = 300).limiterGainDb(), 0.001f)
        assertEquals(10f, EqSettings(loudnessMb = 99_999).limiterGainDb(), 0.001f)
        assertEquals(0f, EqSettings(loudnessMb = -5).limiterGainDb(), 0f)
    }
}
