package com.vardiya.sludge.domain

/** Applies a tuning result to the settings; the user's on/off and auto-tune choices are kept. */
fun EqSettings.applying(profile: AiProfile, source: TuneSource): EqSettings = copy(
    bands = profile.bands,
    bassBoost = profile.bassBoost,
    virtualizer = profile.virtualizer,
    loudnessMb = profile.loudness,
    genre = profile.genre,
    explanation = profile.explanation,
    source = source,
)

/** What the offline path decides for [track]: the detected genre's preset and a line saying why. */
fun localProfile(track: TrackInfo): AiProfile {
    val guess = GenreDetector.detect(track)
    val note = if (guess.confident) {
        "${guess.genre.label} olarak tanındı. ${guess.genre.blurb}."
    } else {
        "Tür belirlenemedi; ${guess.genre.label} ayarı kullanıldı. Yapay zekâ anahtarı eklersen daha iyi tahmin eder."
    }
    return guess.genre.toProfile(note)
}
