package com.vardiya.sludge.app

/** What the tuning request is doing right now, for the screen to show. */
sealed interface TuneStatus {
    data object Idle : TuneStatus
    data object Working : TuneStatus
    data class Failed(val message: String) : TuneStatus
}
