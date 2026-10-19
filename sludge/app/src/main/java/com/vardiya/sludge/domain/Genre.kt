package com.vardiya.sludge.domain

/**
 * The genre presets. Curves are dB at [BAND_HZ] (60 Hz, 230 Hz, 910 Hz, 3.6 kHz, 14 kHz); bass, virtualizer and
 * loudness use the same 0..1000 scale as the Android effects. Harsh noise keeps the treble down on purpose:
 * those recordings are loud and bright already, and boosting them hurts ears and speakers.
 */
enum class Genre(
    val label: String,
    val blurb: String,
    val bands: List<Float>,
    val bass: Int,
    val virtualizer: Int,
    val loudness: Int,
) {
    STONER("Stoner Rock", "Kalın, sıcak fuzz; alt orta dolgun, tizler yumuşak", listOf(5f, 3f, -1f, 1f, -1f), 450, 150, 300),
    DOOM("Doom / Sludge", "Ağır ve yavaş; en alt bölge öne çıkar, tizler kısık", listOf(7f, 4f, -2f, -1f, -3f), 750, 100, 200),
    PSYCH("Psychedelic Rock", "Geniş sahne; sıcak alt, parlak ve havadar üst", listOf(2f, 1f, 1f, 3f, 4f), 200, 600, 200),
    NOISE_ROCK("Noise Rock", "Sert vuruşlar; sıkı bas, keskin orta-tiz", listOf(2f, 1f, 0f, 4f, 1f), 250, 100, 300),
    HARSH_NOISE("Harsh Noise", "Kulak koruması: 2-6 kHz kısık, ses yükseltici kapalı", listOf(2f, 0f, -2f, -4f, -3f), 100, 0, 0),
    DRONE("Drone / Yellow Swans", "Uzayan katmanlar; derin alt, geniş stereo, yumuşak tiz", listOf(4f, 2f, 0f, -1f, 2f), 500, 750, 100),
    SHOEGAZE("Shoegaze", "Ses duvarı; orta çökük, parlak üst", listOf(2f, 1f, -1f, 1f, 3f), 250, 500, 250),
    FLAT("Düz", "Hiçbir şey eklenmez", EqSettings.FLAT_BANDS, 0, 0, 0),
}

/** Turns a curve preset into the profile shape the AI also returns. */
fun Genre.toProfile(explanation: String = blurb) = AiProfile(
    genre = label,
    bands = bands,
    bassBoost = bass,
    virtualizer = virtualizer,
    loudness = loudness,
    explanation = explanation,
)
