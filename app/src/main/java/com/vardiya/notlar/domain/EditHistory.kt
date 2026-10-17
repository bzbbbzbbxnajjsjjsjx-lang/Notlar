package com.vardiya.notlar.domain

/**
 * Undo and redo for the editor. Changes recorded within [mergeWindowMillis] of each other count as
 * one step, so a typed word is undone at once rather than letter by letter; a step never spans more
 * than [maxStepMillis], so a long stretch of typing is undone in parts.
 */
class EditHistory<T>(
    initial: T,
    private val mergeWindowMillis: Long = 1_000,
    private val maxStepMillis: Long = 5_000,
    private val limit: Int = 100,
) {

    private val undoStack = ArrayDeque<T>()
    private val redoStack = ArrayDeque<T>()
    private var lastRecordedAt: Long? = null
    private var stepStartedAt: Long = 0

    var current: T = initial
        private set

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** Records [next] as the current state; a new change clears what could be redone. */
    fun record(next: T, nowMillis: Long) {
        if (next == current) return
        val last = lastRecordedAt
        val newStep = last == null || undoStack.isEmpty() ||
            nowMillis - last > mergeWindowMillis || nowMillis - stepStartedAt > maxStepMillis
        if (newStep) {
            undoStack.addLast(current)
            if (undoStack.size > limit) undoStack.removeFirst()
            stepStartedAt = nowMillis
        }
        current = next
        redoStack.clear()
        lastRecordedAt = nowMillis
    }

    /** Steps back and returns the earlier state, or null if there is none. */
    fun undo(): T? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        current = previous
        lastRecordedAt = null
        return previous
    }

    /** Steps forward again after [undo] and returns that state, or null if there is none. */
    fun redo(): T? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        current = next
        lastRecordedAt = null
        return next
    }
}
