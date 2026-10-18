package com.vardiya.notlar.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import kotlinx.coroutines.delay

private const val PRESSED_SCALE = 0.96f
private const val ENTRANCE_STEP_MS = 45L
private const val ENTRANCE_MAX_STEPS = 8
private const val ENTRANCE_SCALE = 0.9f
private const val ENTRANCE_LIFT_DP = 28
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
        // A motion cut short by a quick re-tap restarts this effect; settle the icon first so it
        // never stays lifted, tilted or squashed.
        offsetY.snapTo(0f)
        rotation.snapTo(0f)
        squash.snapTo(1f)
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

/** A shape partway between the two ends of [morph]; [progress] 0 is the start, 1 the end. */
class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        // The Material shapes are drawn in a unit square; stretch the path to this size.
        val path = morph.toPath(progress)
        path.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(path)
    }
}

/**
 * False when the user turned animations off (Settings > Accessibility > Remove animations, or the
 * developer animation scale at 0): entrances then show their end state at once.
 */
@Composable
fun rememberMotionEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
}

/**
 * Fades, lifts and scales an item in once, [index] steps after the first one, so a list opens as a
 * quick cascade. Items that are not [play]ing (scrolled into view later, or composed after the
 * cascade ended) simply appear.
 */
@Composable
fun Modifier.entrance(play: Boolean, index: Int): Modifier {
    val enabled = rememberMotionEnabled() && play
    val progress = remember { Animatable(if (enabled) 0f else 1f) }
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val lift = with(LocalDensity.current) { ENTRANCE_LIFT_DP.dp.toPx() }
    if (enabled) {
        LaunchedEffect(Unit) {
            delay(index.coerceAtMost(ENTRANCE_MAX_STEPS) * ENTRANCE_STEP_MS)
            progress.animateTo(1f, spec)
        }
    }
    return graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * lift
        val scale = ENTRANCE_SCALE + (1f - ENTRANCE_SCALE) * p
        scaleX = scale
        scaleY = scale
    }
}
