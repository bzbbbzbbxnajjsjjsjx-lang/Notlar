package com.vardiya.sludge.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** The few vibrations the app uses, named by what happened rather than by the haptic constant. */
@Immutable
class Haptics(private val feedback: HapticFeedback) {
    /** A light tick for a button press or a chosen option. */
    fun tick() = feedback.performHapticFeedback(HapticFeedbackType.VirtualKey)

    /** A switch or toggle button flipped; the vibration tells on from off. */
    fun toggle(on: Boolean) = feedback.performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)

    /** Something was done: saved, pinned, added, dropped in place. */
    fun confirm() = feedback.performHapticFeedback(HapticFeedbackType.Confirm)

    /** Something was refused or went wrong. */
    fun reject() = feedback.performHapticFeedback(HapticFeedbackType.Reject)

    /** A press held long enough, such as picking a card up to drag it. */
    fun longPress() = feedback.performHapticFeedback(HapticFeedbackType.LongPress)

    /** A step along a drag, such as a card passing another one. */
    fun step() = feedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
}

@Composable
fun rememberHaptics(): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { Haptics(feedback) }
}

/** [action] preceded by a tick; for a click handler that has nothing to feel by itself, such as a plain icon button. */
@Composable
fun hapticClick(action: () -> Unit): () -> Unit = hapticAction(confirm = false, action)

/** [action] preceded by the "done" vibration, for a click that finishes something such as a restore. */
@Composable
fun hapticConfirmClick(action: () -> Unit): () -> Unit = hapticAction(confirm = true, action)

@Composable
private fun hapticAction(confirm: Boolean, action: () -> Unit): () -> Unit {
    val haptics = rememberHaptics()
    val current by rememberUpdatedState(action)
    return remember(haptics, confirm) {
        {
            if (confirm) haptics.confirm() else haptics.tick()
            current()
        }
    }
}
