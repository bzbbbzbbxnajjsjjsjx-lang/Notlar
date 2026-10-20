package com.vardiya.sludge.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.vardiya.sludge.domain.AttachedSession
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.ParametricFilter
import com.vardiya.sludge.domain.Genre
import com.vardiya.sludge.domain.OutputKind
import com.vardiya.sludge.domain.OutputRoute
import com.vardiya.sludge.domain.TuneSource
import com.vardiya.sludge.ui.theme.SludgeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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

    private val playing = UiState(
        settings = EqSettings(
            filters = Genre.STONER.filters,
            bassBoost = Genre.STONER.bass,
            virtualizer = Genre.STONER.virtualizer,
            loudnessMb = Genre.STONER.loudness,
            genre = "Stoner Rock",
            source = TuneSource.PRESET,
        ),
        sessions = listOf(AttachedSession(
                1042, "com.spotify.music", failed = false, path = "DynamicsProcessing",
                effects = listOf("31 bantlı ekolayzır", "sınırlayıcı (ses yükseltici)", "sanallaştırıcı"),
            )),
        globalMixWorks = false,
        route = OutputRoute(OutputKind.BLUETOOTH, listOf("JBL Tune 520BT")),
    )

    private val tuned = playing.copy(settings = playing.settings.copy(bassBoost = 700, virtualizer = 400, loudnessMb = 600))

    private val fresh = UiState()

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
        Screen(playing.copy(sessions = emptyList()), Tab.EQUALIZER)
    }

    @Test fun equalizerOff() = capture("equalizer_off") {
        Screen(playing.copy(settings = playing.settings.copy(enabled = false)), Tab.EQUALIZER)
    }

    @Test fun setupFresh() = capture("setup_fresh") { Screen(fresh, Tab.SETUP) }

    @Test @Config(qualifiers = "w411dp-h2300dp-night-xxhdpi")
    fun setupDone() = capture("setup_done_dark") { Screen(playing, Tab.SETUP) }

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
