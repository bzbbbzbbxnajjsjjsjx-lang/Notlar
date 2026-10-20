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

/** Whether these settings are exactly [genre]'s preset: its filters and its bass, virtualizer and loudness. */
fun EqSettings.isPreset(genre: Genre): Boolean =
    filters == genre.filters && bassBoost == genre.bass && virtualizer == genre.virtualizer && loudnessMb == genre.loudness
