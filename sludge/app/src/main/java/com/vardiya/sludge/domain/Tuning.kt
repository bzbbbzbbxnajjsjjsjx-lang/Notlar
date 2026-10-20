package com.vardiya.sludge.domain

/** Applies a genre preset to the settings; the user's on/off and headphone choices are kept. */
fun EqSettings.applying(genre: Genre): EqSettings = copy(
    filters = genre.filters,
    bassBoost = genre.bass,
    virtualizer = genre.virtualizer,
    loudnessMb = genre.loudness,
    genre = genre.label,
    source = TuneSource.PRESET,
)
