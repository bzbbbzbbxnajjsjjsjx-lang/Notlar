package com.vardiya.notlar.ui.screens

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.ui.components.Haptics
import kotlin.math.abs

private const val LIFT_SCALE = 1.05f
private const val EDGE_PX = 140f
private const val SCROLL_STEP_PX = 28f
private const val MOVE_SLOP_PX = 12f

/**
 * Drag to reorder in the notes grid. A card picked up with a long press follows the finger; when
 * its center passes over another card of the same group (pinned or not) they swap places at once
 * in [order]. The new order is only stored on drop, through [onDrop].
 */
internal class ReorderState(
    private val grid: LazyStaggeredGridState,
    private val onDrop: (shownIds: List<Long>, from: Int, to: Int) -> Unit,
) {
    var dragId by mutableStateOf<Long?>(null)
        private set

    private var order by mutableStateOf<List<Long>?>(null)
    private var original = emptyList<Long>()
    private var pinned = emptySet<Long>()
    private var start = Offset.Zero
    private var delta by mutableStateOf(Offset.Zero)
    private var moved = false

    /** [notes] in the order of the drag in progress, or as they are when none is. */
    fun arrange(notes: List<Note>): List<Note> {
        val live = order ?: return notes
        val byId = notes.associateBy { it.id }
        return live.mapNotNull { byId[it] } + notes.filter { it.id !in live }
    }

    /** Where the card has to be drawn relative to where the grid laid it out. */
    fun shift(id: Long): Offset {
        val info = grid.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id }
        return if (id != dragId || info == null) Offset.Zero else start + delta - info.offset.toOffset()
    }

    fun begin(id: Long, shown: List<Note>): Boolean {
        val info = grid.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id } ?: return false
        start = info.offset.toOffset()
        delta = Offset.Zero
        moved = false
        original = shown.map { it.id }
        pinned = shown.filter { it.pinned }.mapTo(mutableSetOf()) { it.id }
        order = original
        dragId = id
        return true
    }

    /** Moves the card by [amount]; returns the scroll the grid needs to keep the card in view, and whether it swapped. */
    fun drag(amount: Offset): Pair<Float, Boolean> {
        val id = dragId
        val me = grid.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id }
        if (id == null || me == null) return 0f to false
        delta += amount
        if (abs(delta.x) + abs(delta.y) > MOVE_SLOP_PX) moved = true
        val center = start + Offset(me.size.width / 2f, me.size.height / 2f) + delta
        return edgeScroll(center.y) to swapWithCardUnder(id, center)
    }

    /** Puts the dragged card in the place of the card of its own group that [center] is over; true if it moved. */
    private fun swapWithCardUnder(id: Long, center: Offset): Boolean {
        val target = grid.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            val key = item.key as? Long
            key != null && key != id && (key in pinned) == (id in pinned) && item.contains(center)
        }?.key as? Long
        val live = order.orEmpty()
        val from = live.indexOf(id)
        val to = target?.let(live::indexOf) ?: -1
        val swap = from >= 0 && to >= 0 && from != to
        if (swap) order = live.toMutableList().apply { add(to, removeAt(from)) }
        return swap
    }

    private fun LazyStaggeredGridItemInfo.contains(point: Offset) =
        point.x >= offset.x && point.x < offset.x + size.width && point.y >= offset.y && point.y < offset.y + size.height

    /** How far to scroll so a card held near the top or bottom edge of the grid keeps coming into view. */
    private fun edgeScroll(centerY: Float): Float = when {
        centerY < EDGE_PX -> -SCROLL_STEP_PX
        centerY > grid.layoutInfo.viewportSize.height - EDGE_PX -> SCROLL_STEP_PX
        else -> 0f
    }

    /** Lets go of the card; true if it was only pressed, not dragged, which then selects it. */
    fun end(): Boolean {
        val id = dragId ?: return false
        val finalOrder = order
        val wasTap = !moved
        dragId = null
        order = null
        if (!wasTap && finalOrder != null && finalOrder != original) {
            onDrop(original, original.indexOf(id), finalOrder.indexOf(id))
        }
        return wasTap
    }

    fun cancel() {
        dragId = null
        order = null
    }
}

/**
 * Makes a grid card draggable with [state] while [enabled]. [onTap] runs when the card was held but
 * not moved, so a long press still selects it as it does in the other orders.
 */
@Composable
internal fun Modifier.reorderable(
    id: Long,
    enabled: Boolean,
    state: ReorderState,
    shown: List<Note>,
    haptics: Haptics,
    scrollBy: (Float) -> Unit,
    onTap: () -> Unit,
): Modifier {
    // The list changes while a card is dragged; the gesture must keep running through that.
    val currentShown by rememberUpdatedState(shown)
    val currentTap by rememberUpdatedState(onTap)
    val lift by animateFloatAsState(
        targetValue = if (state.dragId == id) LIFT_SCALE else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "reorderLift",
    )
    return this
    .pointerInput(enabled, id) {
        if (!enabled) return@pointerInput
        detectDragGesturesAfterLongPress(
            onDragStart = { if (state.begin(id, currentShown)) haptics.longPress() },
            onDrag = { change, amount ->
                change.consume()
                val (scroll, swapped) = state.drag(amount)
                if (swapped) haptics.step()
                if (scroll != 0f) scrollBy(scroll)
            },
            onDragEnd = {
                if (state.end()) currentTap() else haptics.confirm()
            },
            onDragCancel = state::cancel,
        )
    }
    .zIndex(if (state.dragId == id) 1f else 0f)
    .graphicsLayer {
        val shift = state.shift(id)
        translationX = shift.x
        translationY = shift.y
        scaleX = lift
        scaleY = lift
    }
}

private fun androidx.compose.ui.unit.IntOffset.toOffset() = Offset(x.toFloat(), y.toFloat())
