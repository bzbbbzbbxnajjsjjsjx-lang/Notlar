package com.vardiya.notlar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditHistoryTest {

    @Test
    fun quickChangesUndoAsOneStep() {
        val history = EditHistory("")
        history.record("K", nowMillis = 0)
        history.record("Ki", nowMillis = 300)
        history.record("Kira", nowMillis = 600)
        history.record("Kira ödendi", nowMillis = 5_000)
        assertEquals("Kira", history.undo())
        assertEquals("", history.undo())
        assertNull(history.undo())
        assertFalse(history.canUndo)
    }

    @Test
    fun redoReturnsUndoneStatesUntilANewChange() {
        val history = EditHistory("a")
        history.record("b", nowMillis = 0)
        history.record("c", nowMillis = 5_000)
        history.undo()
        history.undo()
        assertEquals("b", history.redo())
        assertTrue(history.canRedo)
        history.record("x", nowMillis = 10_000)
        assertFalse(history.canRedo)
        assertEquals("b", history.undo())
    }

    @Test
    fun sameStateIsNotAStep() {
        val history = EditHistory("a")
        history.record("a", nowMillis = 0)
        assertFalse(history.canUndo)
    }

    @Test
    fun keepsAtMostTheLimit() {
        val history = EditHistory(0, mergeWindowMillis = 0, limit = 3)
        (1..10).forEach { history.record(it, nowMillis = it * 10L) }
        repeat(3) { history.undo() }
        assertFalse(history.canUndo)
        assertEquals(7, history.current)
    }

    @Test
    fun longTypingIsSplitIntoSteps() {
        val history = EditHistory("", mergeWindowMillis = 1_000, maxStepMillis = 5_000)
        var text = ""
        (0..20).forEach { second ->
            text += "a"
            history.record(text, nowMillis = second * 500L)
        }
        // 10 seconds of steady typing: undone in parts, not all at once.
        assertTrue(history.undo()!!.isNotEmpty())
    }
}
