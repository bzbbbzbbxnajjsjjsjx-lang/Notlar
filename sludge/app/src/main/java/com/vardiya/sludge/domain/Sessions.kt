package com.vardiya.sludge.domain

/** The name the app's own test-tone session is attached under; it is not a player and never counts as one. */
const val TONE_PACKAGE = "Test tonu"

/** One audio session the engine is holding effects on. */
data class AttachedSession(
    val id: Int,
    val packageName: String,
    val failed: Boolean,
    /** Which effect the sound goes through on this session: "DynamicsProcessing", "Ekolayzır" or empty when none works. */
    val path: String = "",
    /** What works on this session, in words. */
    val effects: List<String> = emptyList(),
)

/** The sessions that really process a player's sound: an equalizer path works and it is not the test tone. */
fun List<AttachedSession>.processing(): List<AttachedSession> = filter { !it.failed && it.path.isNotEmpty() && it.packageName != TONE_PACKAGE }

data class EngineState(val sessions: List<AttachedSession> = emptyList())
