package com.vardiya.sludge.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.vardiya.sludge.domain.AttachedSession
import com.vardiya.sludge.app.TuneStatus
import com.vardiya.sludge.domain.AiConfig
import com.vardiya.sludge.domain.AiProvider
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.DayCount
import com.vardiya.sludge.domain.Genre
import com.vardiya.sludge.domain.GenreShare
import com.vardiya.sludge.domain.LastFmConfig
import com.vardiya.sludge.domain.ModelOption
import com.vardiya.sludge.domain.OutputKind
import com.vardiya.sludge.domain.OutputRoute
import com.vardiya.sludge.domain.ListenSummary
import com.vardiya.sludge.domain.TrackInfo
import com.vardiya.sludge.domain.TuneSource
import com.vardiya.sludge.ui.screens.ModelListState
import com.vardiya.sludge.ui.screens.ModelPickerContent
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
            bassBoost = Genre.STONER.bass,
            virtualizer = Genre.STONER.virtualizer,
            loudnessMb = Genre.STONER.loudness,
            genre = "Stoner Rock",
            explanation = "Sleep'in kalın fuzz sesi alt ve alt-orta bölgede yaşıyor; tizi biraz kısıp sıcaklığı korudum.",
            source = TuneSource.AI,
        ),
        track = track,
        playing = true,
        accessGranted = true,
        sessions = listOf(AttachedSession(
                1042, "com.spotify.music", failed = false, path = "DynamicsProcessing",
                effects = listOf("10 bantlı ekolayzır", "sınırlayıcı (ses yükseltici)", "sanallaştırıcı"),
            )),
        globalMixWorks = false,
        ai = AiConfig(apiKey = "sk-ant-demo-key"),
        lastFm = LastFmConfig("0123456789abcdef0123456789abcdef"),
        tags = listOf("stoner rock", "doom metal", "sludge"),
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

    @Test fun ai() = capture("ai") { Screen(playing, Tab.AI) }

    @Test @Config(qualifiers = "w411dp-h2300dp-night-xxhdpi")
    fun aiOpenRouter() = capture("ai_openrouter_dark") {
        val config = AiConfig(AiProvider.OPENROUTER, "openai/gpt-4o-mini", "https://openrouter.ai/api/v1", "sk-or-demo")
        Screen(playing.copy(ai = config), Tab.AI)
    }

    @Test @Config(qualifiers = "+night")
    fun modelPicker() = capture("model_picker_dark") {
        ModelPickerContent(
            state = ModelListState.Loaded(
                listOf(
                    ModelOption("anthropic/claude-sonnet", "Anthropic: Claude Sonnet", 3.0, 15.0, 200_000),
                    ModelOption("openai/gpt-4o-mini", "OpenAI: GPT-4o mini", 0.15, 0.6, 128_000),
                    ModelOption("meta-llama/llama-3.3-70b-instruct:free", "Meta: Llama 3.3 70B Instruct (free)", 0.0, 0.0, 131_072),
                    ModelOption("openrouter/auto", "Auto Router", null, null, null),
                ),
            ),
            selectedId = "openai/gpt-4o-mini",
            onPick = {},
            onRetry = {},
        )
    }

    // A tall screen, so the Last.fm card below the provider card is in the picture too.
    @Test @Config(qualifiers = "w411dp-h2300dp-night-xxhdpi")
    fun aiTallDark() = capture("ai_tall_dark") { Screen(playing, Tab.AI) }

    @Test @Config(qualifiers = "+night")
    fun aiDark() = capture("ai_dark") { Screen(playing, Tab.AI) }

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
