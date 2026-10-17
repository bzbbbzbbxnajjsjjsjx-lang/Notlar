package com.vardiya.notlar.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private const val ARMED_SCALE = 1.3f

/** What a swipe does: shown behind the card while it slides, run when it is let go past the threshold. */
class SwipeAction(
    val icon: ImageVector,
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
    val onSwipe: () -> Unit,
)

/**
 * Lets [content] be swiped right for [startAction] or left for [endAction]. The same actions are
 * offered to TalkBack as custom actions, so they never depend on the gesture alone.
 */
@Composable
fun SwipeActions(
    startAction: SwipeAction?,
    endAction: SwipeAction?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val state = rememberSwipeToDismissBoxState()
    val shape = MaterialTheme.shapes.large
    SwipeToDismissBox(
        state = state,
        modifier = modifier.semantics {
            customActions = listOfNotNull(startAction, endAction).map { action ->
                CustomAccessibilityAction(action.label) {
                    action.onSwipe()
                    true
                }
            }
        },
        enableDismissFromStartToEnd = startAction != null,
        enableDismissFromEndToStart = endAction != null,
        gesturesEnabled = enabled,
        onDismiss = { value ->
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> startAction?.onSwipe?.invoke()
                SwipeToDismissBoxValue.EndToStart -> endAction?.onSwipe?.invoke()
                SwipeToDismissBoxValue.Settled -> Unit
            }
        },
        backgroundContent = {
            val toStart = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
            val action = if (toStart) endAction else startAction
            if (action != null && state.dismissDirection != SwipeToDismissBoxValue.Settled) {
                // The icon grows once letting go would run the action.
                val armed = state.targetValue != SwipeToDismissBoxValue.Settled
                val scale by animateFloatAsState(
                    targetValue = if (armed) ARMED_SCALE else 1f,
                    animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                    label = "swipeIcon",
                )
                Row(
                    Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .background(action.containerColor)
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = if (toStart) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        action.icon,
                        contentDescription = null,
                        tint = action.contentColor,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    )
                }
            }
        },
    ) {
        content()
    }
}
