package com.vardiya.sludge.domain

/** Applies a tuning to the settings; the user's on/off and auto-tune choices are kept. */
fun EqSettings.applying(tuning: Tuning, source: TuneSource): EqSettings = copy(
    bands = tuning.bands,
    filters = tuning.filters,
    bassBoost = tuning.bassBoost,
    virtualizer = tuning.virtualizer,
    loudnessMb = tuning.loudness,
    genre = tuning.genre,
    explanation = tuning.explanation,
    source = source,
)

/** What is decided for [track]: the detected genre's preset and a line saying why. */
fun localTuning(track: TrackInfo): Tuning {
    val guess = GenreDetector.detect(track)
    val note = if (guess.confident) {
        "${guess.genre.label} olarak tanındı. ${guess.genre.blurb}."
    } else {
        "Tür belirlenemedi, bu yüzden ${guess.genre.label} ayarı kullanıldı. İstediğin ön ayarı elle seçebilirsin."
    }
    return guess.genre.toTuning(note)
}
