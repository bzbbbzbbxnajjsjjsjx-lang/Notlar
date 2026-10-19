package com.vardiya.sludge.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.vardiya.sludge.audio.AttachedSession
import com.vardiya.sludge.audio.TuneStatus
import com.vardiya.sludge.domain.AiConfig
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.Genre
import com.vardiya.sludge.domain.TrackInfo
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

    private val track = TrackInfo(title = "Dopesmoker", artist = "Sleep", album = "Dopesmoker", genreTag = "Stoner Metal", source = "Spotify")

    private val playing = UiState(
        settings = EqSettings(
            bands = Genre.STONER.bands,
            bassBoost = Genre.STONER.bass,
            virtualizer = Genre.STONER.virtualizer,
            loudnessMb = Genre.STONER.loudness,
            genre = "Stoner Rock",
            explanation = "Sleep'in kalın fuzz sesi alt ve alt-orta bölgede yaşıyor; tizi biraz kısıp sıcaklığı korudum.",
            source = TuneSource.AI,
        ),
        track = track,
        accessGranted = true,
        sessions = listOf(AttachedSession(1042, "com.spotify.music", failed = false)),
        globalMixWorks = false,
        ai = AiConfig(apiKey = "sk-ant-demo-key"),
    )

    private val fresh = UiState()

    @Test fun equalizer() = capture("equalizer") { Screen(playing, Tab.EQUALIZER) }

    @Test @Config(qualifiers = "+night")
    fun equalizerDark() = capture("equalizer_dark") { Screen(playing, Tab.EQUALIZER) }

    @Test fun equalizerIdle() = capture("equalizer_idle") { Screen(fresh, Tab.EQUALIZER) }

    @Test fun equalizerWorking() = capture("equalizer_working") { Screen(playing.copy(status = TuneStatus.Working), Tab.EQUALIZER) }

    @Test fun ai() = capture("ai") { Screen(playing, Tab.AI) }

    @Test @Config(qualifiers = "+night")
    fun aiDark() = capture("ai_dark") { Screen(playing, Tab.AI) }

    @Test fun presets() = capture("presets") { Screen(playing, Tab.PRESETS) }

    @Test @Config(qualifiers = "+night")
    fun presetsDark() = capture("presets_dark") { Screen(playing, Tab.PRESETS) }

    @Test fun setupFresh() = capture("setup_fresh") { Screen(fresh, Tab.SETUP) }

    @Test @Config(qualifiers = "+night")
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
