package com.vardiya.notlar.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private const val PRESSED_SCALE = 0.96f
private const val WIGGLE_MS = 420
private const val SQUASH = 0.8f

/** The small animation an icon plays when its destination or action is chosen. */
enum class IconMotion {
    /** Hops up and springs back. */
    BOUNCE,

    /** Drops in from above and squashes on landing, like something put into a box. */
    DROP,

    /** Shakes side to side from its base, like a bin lid. */
    WIGGLE,

    /** Turns a quarter and springs back, like a pin being pushed in. */
    TWIST,
}

/** Shrinks a little while [interactionSource] is pressed and springs back on release. */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESSED_SCALE else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * An icon that plays [motion] each time [play] turns true after the first composition, so a
 * screen that opens already selected stays still.
 */
@Composable
fun AnimatedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    play: Boolean,
    motion: IconMotion,
    modifier: Modifier = Modifier,
) {
    val offsetY = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val squash = remember { Animatable(1f) }
    val spring = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val fast = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val lift = with(LocalDensity.current) { 6.dp.toPx() }
    var settled by remember { mutableStateOf(false) }

    LaunchedEffect(play) {
        if (!settled) {
            settled = true
            return@LaunchedEffect
        }
        if (!play) return@LaunchedEffect
        when (motion) {
            IconMotion.BOUNCE -> {
                offsetY.animateTo(-lift, fast)
                offsetY.animateTo(0f, spring)
            }
            IconMotion.DROP -> {
                offsetY.snapTo(-lift * 2)
                offsetY.animateTo(0f, fast)
                squash.snapTo(SQUASH)
                squash.animateTo(1f, spring)
            }
            IconMotion.WIGGLE -> rotation.animateTo(
                0f,
                keyframes {
                    durationMillis = WIGGLE_MS
                    -14f at 70
                    12f at 160
                    -8f at 250
                    4f at 330
                },
            )
            IconMotion.TWIST -> {
                rotation.animateTo(-45f, fast)
                rotation.animateTo(0f, spring)
            }
        }
    }

    Icon(
        imageVector,
        contentDescription = contentDescription,
        modifier = modifier.graphicsLayer {
            translationY = offsetY.value
            rotationZ = rotation.value
            scaleY = squash.value
            scaleX = 2f - squash.value
            // Wiggle and squash pivot on the icon's base, the way a bin or box sits on a table.
            transformOrigin = TransformOrigin(0.5f, 1f)
        },
    )
}
