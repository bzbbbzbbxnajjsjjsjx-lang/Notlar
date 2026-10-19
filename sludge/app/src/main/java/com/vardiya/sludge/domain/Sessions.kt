package com.vardiya.sludge.domain

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

data class EngineState(
    val sessions: List<AttachedSession> = emptyList(),
    /** Result of the attempt to sit on the global output mix: null = not tried yet. */
    val globalMixWorks: Boolean? = null,
)
