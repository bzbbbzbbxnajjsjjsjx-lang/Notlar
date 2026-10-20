package com.vardiya.sludge.domain

/**
 * The genre presets, each a real parametric equalizer: a few filters (a shelf for the low end, peaks where the genre
 * lives, a shelf for the top), plus bass, virtualizer and loudness on the same 0..1000 scale as the Android effects.
 * Boosts stay tasteful; harsh noise keeps the treble down on purpose, because those recordings are loud and bright
 * already and boosting them hurts ears and speakers.
 */
enum class Genre(
    val label: String,
    val blurb: String,
    val filters: List<ParametricFilter>,
    val bass: Int,
    val virtualizer: Int,
    val loudness: Int,
) {
    STONER(
        "Stoner Rock", "Kalın, sıcak fuzz; alt orta dolgun, tizler yumuşak",
        listOf(lowShelf(160f, 5f), peak(250f, 1.8f), peak(900f, -1.5f), peak(3_600f, 1.5f, 1.2f), highShelf(9_000f, -1.5f)),
        450, 150, 300,
    ),
    DOOM(
        "Doom / Sludge", "Ağır ve yavaş; en alt bölge öne çıkar, tizler kısık",
        listOf(lowShelf(150f, 7f), peak(230f, 2.3f), peak(900f, -2.5f, 0.9f), peak(3_600f, -1f), highShelf(6_000f, -3f)),
        750, 100, 200,
    ),
    PSYCH(
        "Psychedelic Rock", "Geniş sahne; sıcak alt, parlak ve havadar üst",
        listOf(lowShelf(160f, 2.2f), peak(900f, 1f, 0.8f), peak(3_600f, 2f), highShelf(8_000f, 3.5f)),
        200, 600, 200,
    ),
    NOISE_ROCK(
        "Noise Rock", "Sert vuruşlar; sıkı bas, keskin orta-tiz",
        listOf(lowShelf(140f, 2f), peak(400f, -1.5f), peak(3_500f, 3.5f, 1.2f), highShelf(10_000f, 1f)),
        250, 100, 300,
    ),
    HARSH_NOISE(
        "Harsh Noise", "Kulak koruması: 2-6 kHz kısık, ses yükseltici kapalı",
        listOf(lowShelf(120f, 2f), peak(800f, -1.5f, 0.8f), peak(3_500f, -4f), peak(6_000f, -2f, 2f), highShelf(8_000f, -3f)),
        100, 0, 0,
    ),
    DRONE(
        "Drone / Yellow Swans", "Uzayan katmanlar; derin alt, geniş stereo, yumuşak tiz",
        listOf(lowShelf(140f, 4f), peak(250f, 1.3f), peak(3_000f, -1.5f, 0.9f), highShelf(9_000f, 2f)),
        500, 750, 100,
    ),
    SHOEGAZE(
        "Shoegaze", "Ses duvarı; orta çökük, parlak üst",
        listOf(lowShelf(180f, 2.2f), peak(700f, -2.5f, 0.9f), peak(3_000f, 1.5f), highShelf(8_000f, 3f)),
        250, 500, 250,
    ),
    FLAT("Düz", "Hiçbir şey eklenmez", emptyList(), 0, 0, 0),
    ;
}

private fun peak(hz: Float, gain: Float, q: Float = 1f) = ParametricFilter(FilterType.PEAK.key, hz, gain, q)

private fun lowShelf(hz: Float, gain: Float) = ParametricFilter(FilterType.LOW_SHELF.key, hz, gain, SHELF_Q)

private fun highShelf(hz: Float, gain: Float) = ParametricFilter(FilterType.HIGH_SHELF.key, hz, gain, SHELF_Q)

private const val SHELF_Q = 0.7f
