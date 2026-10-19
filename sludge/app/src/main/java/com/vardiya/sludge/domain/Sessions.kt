package com.vardiya.sludge.domain

/** One audio session the engine is holding effects on. */
data class AttachedSession(val id: Int, val packageName: String, val failed: Boolean)

data class EngineState(
    val sessions: List<AttachedSession> = emptyList(),
    /** Result of the attempt to sit on the global output mix: null = not tried yet. */
    val globalMixWorks: Boolean? = null,
)
