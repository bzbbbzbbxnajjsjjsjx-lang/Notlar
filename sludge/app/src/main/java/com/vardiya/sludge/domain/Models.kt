package com.vardiya.sludge.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Center frequencies of the ten octave bands every curve is written in (Hz). */
val BAND_HZ: List<Float> = listOf(31f, 62f, 125f, 250f, 500f, 1_000f, 2_000f, 4_000f, 8_000f, 16_000f)

const val BAND_COUNT = 10
const val MAX_GAIN_DB = 12f
const val MAX_EFFECT = 1000
const val MAX_LOUDNESS_MB = 1000

/** What the player says is playing. All fields come from the media session metadata. */
data class TrackInfo(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    /** The player's own genre tag, when it provides one. */
    val genreTag: String = "",
    /** Display name of the player app. */
    val source: String = "",
) {
    val isBlank: Boolean get() = title.isBlank() && artist.isBlank()
}

/** Which service answers the AI tuning request. */
enum class AiProvider(val label: String, val defaultModel: String, val defaultBaseUrl: String) {
    CLAUDE("Claude", "claude-haiku-5-5", "https://api.anthropic.com"),
    OPENAI("OpenAI", "gpt-4o-mini", "https://api.openai.com/v1"),
    // One key for many models; model names look like "provider/model" (see openrouter.ai/models).
    OPENROUTER("OpenRouter", "openai/gpt-4o-mini", "https://openrouter.ai/api/v1"),
}

/** Connection settings. [toString] never prints the key. */
data class AiConfig(
    val provider: AiProvider = AiProvider.CLAUDE,
    val model: String = AiProvider.CLAUDE.defaultModel,
    val baseUrl: String = AiProvider.CLAUDE.defaultBaseUrl,
    val apiKey: String = "",
) {
    val isUsable: Boolean
        get() = apiKey.isNotBlank() && apiKey.none { it.isISOControl() || it.isWhitespace() } && model.isNotBlank() &&
            UrlRules.isAcceptable(baseUrl)

    override fun toString() = "AiConfig(provider=$provider, model=$model, baseUrl=$baseUrl, apiKey=<hidden>)"
}

/** What the tuner decided for one track. */
@Serializable
data class AiProfile(
    val genre: String = "",
    val bands: List<Float>,
    @SerialName("bass_boost") val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudness: Int = 0,
    val explanation: String = "",
)

/** Where the current setting came from. */
enum class TuneSource(val label: String) {
    MANUAL("Elle"),
    PRESET("Ön ayar"),
    LOCAL("Yerel tür tanıma"),
    AI("Yapay zekâ"),
}

/** Everything the equalizer needs to sound the way it does. */
data class EqSettings(
    val enabled: Boolean = true,
    val bands: List<Float> = FLAT_BANDS,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudnessMb: Int = 0,
    val genre: String = "",
    val autoTune: Boolean = true,
    val explanation: String = "",
    val source: TuneSource = TuneSource.MANUAL,
    /** The headphone whose measured response is corrected toward the Harman target; null = no correction. */
    val headphoneId: String? = Headphones.DEFAULT_ID,
) {
    companion object {
        val FLAT_BANDS: List<Float> = List(BAND_COUNT) { 0f }
    }
}
