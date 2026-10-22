package com.vardiya.notlar.gemini

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.vardiya.notlar.ui.theme.NotlarTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The Gemini exporter's states, light and dark; recorded with the other screens. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class GeminiScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val link = "https://gemini.google.com/share/abc123def456"
    private val actions = HomeActions({}, {}, {}, {}, {}, {}, {})
    private val turns = listOf(
        ChatTurn(ChatRole.User, "Türkiye'nin başkenti neresidir?"),
        ChatTurn(ChatRole.Model, "Türkiye'nin başkenti Ankara'dır. 1923'te Cumhuriyet ilan edildiğinde İstanbul yerine seçilmiştir."),
        ChatTurn(ChatRole.User, "Nüfusu kaç?"),
        ChatTurn(ChatRole.Model, "Ankara'nın nüfusu yaklaşık 5,8 milyondur.\n\nİlçeler:\n- Çankaya\n- Keçiören\n- Yenimahalle\n- Mamak"),
    )

    @Test fun idle() = capture("gemini_idle", ParserUiState.Idle)

    @Test @Config(qualifiers = "+night")
    fun idleDark() = capture("gemini_idle_dark", ParserUiState.Idle)

    @Test fun loading() = capture("gemini_loading", ParserUiState.Loading(2, 4))

    @Test fun success() = capture("gemini_success", ParserUiState.Success(link, turns))

    @Test @Config(qualifiers = "+night")
    fun successDark() = capture("gemini_success_dark", ParserUiState.Success(link, turns))

    @Test fun error() = capture("gemini_error", ParserUiState.Error(ParseFailure.Network))

    @Test fun invalidLink() = capture("gemini_invalid_link", ParserUiState.Error(ParseFailure.InvalidLink), "bozuk")

    private fun capture(name: String, state: ParserUiState, text: String = link) {
        compose.mainClock.autoAdvance = false
        compose.setContent { Screen(state, text) }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Composable
    private fun Screen(state: ParserUiState, text: String) {
        NotlarTheme { HomeScreen(state = state, link = text, actions = actions) }
    }
}
