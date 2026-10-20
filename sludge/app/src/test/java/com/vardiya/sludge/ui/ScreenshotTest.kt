package com.vardiya.sludge.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.vardiya.sludge.domain.AttachedSession
import com.vardiya.sludge.app.TuneStatus
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.ParametricFilter
import com.vardiya.sludge.domain.DayCount
import com.vardiya.sludge.domain.Genre
import com.vardiya.sludge.domain.GenreShare
import com.vardiya.sludge.domain.OutputKind
import com.vardiya.sludge.domain.OutputRoute
import com.vardiya.sludge.domain.ListenSummary
import com.vardiya.sludge.domain.TrackInfo
import com.vardiya.sludge.domain.TuneSource
import com.vardiya.sludge.ui.theme.SludgeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Renders every tab, light and dark, into `app/src/test/screenshots/`.
 * Record with `./gradlew recordRoborazziDebug`, check with `./gradlew verifyRoborazziDebug`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// SDK 34: newer Robolectric images need JDK 21 and extra --add-opens; CI runs JDK 17.
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val track = TrackInfo(title = "Dopesmoker", artist = "Sleep", album = "Dopesmoker", genreTag = "Stoner Metal", source = "Spotify")

    private val playing = UiState(
        settings = EqSettings(
            bands = Genre.STONER.bands,
            filters = Genre.STONER.filters,
            bassBoost = Genre.STONER.bass,
            virtualizer = Genre.STONER.virtualizer,
            loudnessMb = Genre.STONER.loudness,
            genre = "Stoner Rock",
            explanation = "Stoner Rock olarak tanındı. Kalın, sıcak fuzz; alt orta dolgun, tizler yumuşak.",
            source = TuneSource.LOCAL,
        ),
        track = track,
        playing = true,
        accessGranted = true,
        sessions = listOf(AttachedSession(
                1042, "com.spotify.music", failed = false, path = "DynamicsProcessing",
                effects = listOf("31 bantlı ekolayzır", "sınırlayıcı (ses yükseltici)", "sanallaştırıcı"),
            )),
        globalMixWorks = false,
        route = OutputRoute(OutputKind.BLUETOOTH, listOf("JBL Tune 520BT")),
    )

    private val tuned = playing.copy(settings = playing.settings.copy(bassBoost = 700, virtualizer = 400, loudnessMb = 600))

    private val fresh = UiState()

    private val today = LocalDate.of(2026, 10, 19)
    private val withHistory = playing.copy(
        listens = ListenSummary(
            total = 86,
            genres = listOf(
                GenreShare(Genre.STONER, 31, 31f / 86),
                GenreShare(Genre.DOOM, 22, 22f / 86),
                GenreShare(Genre.DRONE, 14, 14f / 86),
                GenreShare(Genre.PSYCH, 10, 10f / 86),
                GenreShare(Genre.HARSH_NOISE, 6, 6f / 86),
                GenreShare(Genre.FLAT, 3, 3f / 86),
            ),
            topArtists = listOf("Sleep" to 14, "Electric Wizard" to 11, "Yellow Swans" to 9, "Earth" to 7, "Melvins" to 5),
            days = listOf(3, 0, 7, 12, 5, 9, 4).mapIndexed { i, n -> DayCount(today.minusDays(6L - i), n) },
        ),
    )

    @Test fun equalizer() = capture("equalizer") { Screen(playing, Tab.EQUALIZER) }

    @Test @Config(qualifiers = "+night")
    fun equalizerDark() = capture("equalizer_dark") { Screen(playing, Tab.EQUALIZER) }

    // A tall screen, so the effect cards below the bands are in the picture too.
    @Test @Config(qualifiers = "w411dp-h2300dp-xxhdpi")
    fun equalizerTall() = capture("equalizer_tall") { Screen(tuned, Tab.EQUALIZER) }

    @Test @Config(qualifiers = "w411dp-h2300dp-night-xxhdpi")
    fun equalizerTallDark() = capture("equalizer_tall_dark") { Screen(tuned, Tab.EQUALIZER) }

    @Test @Config(qualifiers = "w411dp-h2300dp-night-xxhdpi")
    fun equalizerParametricTallDark() = capture("equalizer_parametric_tall_dark") {
        val filters = listOf(
            ParametricFilter("lowshelf", 90f, 4f, 0.7f),
            ParametricFilter("peak", 320f, -3.5f, 1.4f),
            ParametricFilter("peak", 3_400f, -5f, 4f),
            ParametricFilter("highshelf", 9_000f, -2f, 0.7f),
        )
        Screen(tuned.copy(settings = tuned.settings.copy(filters = filters)), Tab.EQUALIZER)
    }

    @Test @Config(qualifiers = "+night")
    fun equalizerSpeaker() = capture("equalizer_speaker_dark") { Screen(playing.copy(route = OutputRoute(OutputKind.SPEAKER)), Tab.EQUALIZER) }

    @Test fun equalizerIdle() = capture("equalizer_idle") { Screen(fresh, Tab.EQUALIZER) }

    @Test fun equalizerWaiting() = capture("equalizer_waiting") {
        Screen(playing.copy(sessions = emptyList(), track = null), Tab.EQUALIZER)
    }

    @Test fun equalizerOff() = capture("equalizer_off") {
        Screen(playing.copy(settings = playing.settings.copy(enabled = false)), Tab.EQUALIZER)
    }

    @Test fun equalizerWorking() = capture("equalizer_working") { Screen(playing.copy(status = TuneStatus.Working), Tab.EQUALIZER) }

    @Test fun stats() = capture("stats") { Screen(withHistory, Tab.STATS) }

    @Test @Config(qualifiers = "+night")
    fun statsDark() = capture("stats_dark") { Screen(withHistory, Tab.STATS) }

    @Test fun statsEmpty() = capture("stats_empty") { Screen(fresh, Tab.STATS) }

    @Test fun setupFresh() = capture("setup_fresh") { Screen(fresh, Tab.SETUP) }

    @Test @Config(qualifiers = "w411dp-h2300dp-night-xxhdpi")
    fun setupDone() = capture("setup_done_dark") { Screen(playing.copy(accessGranted = true), Tab.SETUP) }

    @Composable
    private fun Screen(state: UiState, tab: Tab) {
        SludgeScreen(state = state, tab = tab, onTabChange = {}, actions = NoActions)
    }

    private fun capture(name: String, screen: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { SludgeTheme { screen() } }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
