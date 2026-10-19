package com.vardiya.sludge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import kotlin.math.atan2
import kotlin.math.roundToInt

private const val DISABLED_ALPHA = 0.38f
private const val KNOB_START_DEG = 135f
private const val KNOB_SWEEP_DEG = 270f
private const val KNOB_HALF_SWEEP_DEG = 135f
private const val GAP_MIDDLE_DEG = 315f
private val KNOB_SIZE = 168.dp
private val KNOB_STROKE = 14.dp
private val PILL_HEIGHT = 56.dp

/** The three effect cards share this frame: a shape badge, the name, the percentage and the on/off switch. */
@Composable
fun EffectCard(
    title: String,
    icon: ImageVector,
    polygon: RoundedPolygon,
    value: Int,
    max: Int,
    enabled: Boolean,
    container: Color,
    onValue: (Int) -> Unit,
    defaultOn: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = container)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SwitchRow(value > 0, { on -> onValue(if (on) defaultOn else 0) }, enabled = enabled) {
                CookieBadge(icon, size = 52.dp, polygon = polygon, container = scheme.primary, content = scheme.onPrimary)
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
                    Text(
                        "%${value * PERCENT / max}",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            Box(Modifier.fillMaxWidth().alpha(if (enabled) 1f else DISABLED_ALPHA), contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

private const val PERCENT = 100

/** Maps a pointer angle around [center] to a 0..1 fraction of the knob's 270 degree sweep. */
private fun knobFraction(x: Float, y: Float, centerX: Float, centerY: Float): Float {
    val angle = Math.toDegrees(atan2(y - centerY, x - centerX).toDouble()).toFloat()
    val a = (angle - KNOB_START_DEG + 360f) % 360f
    return when {
        a <= KNOB_SWEEP_DEG -> a / KNOB_SWEEP_DEG
        // The gap at the bottom: the nearer end wins, so the knob sticks at 0 or full instead of flipping.
        a < GAP_MIDDLE_DEG -> 1f
        else -> 0f
    }
}

/** A round knob: an arc for the level and a scalloped face that turns with it. Drag around it to set the value. */
@Composable
fun RotaryKnob(
    label: String,
    value: Int,
    max: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    var size by remember { mutableStateOf(IntSize(1, 1)) }
    val faceShape = MaterialShapes.Cookie9Sided.toShape()
    val fraction = value.toFloat() / max
    val track = scheme.surfaceContainerHighest
    val arc = scheme.primary

    fun set(x: Float, y: Float) {
        val next = (knobFraction(x, y, size.width / 2f, size.height / 2f) * max / STEP).roundToInt() * STEP
        if (next != current) {
            haptics.step()
            change(next.coerceIn(0, max))
        }
    }

    Box(
        modifier
            .size(KNOB_SIZE)
            .onSizeChanged { size = it }
            .pointerInput(enabled) { if (enabled) detectTapGestures { set(it.x, it.y) } }
            .pointerInput(enabled) {
                if (enabled) detectDragGestures(onDragStart = { set(it.x, it.y) }) { c, _ ->
                    c.consume()
                    set(c.position.x, c.position.y)
                }
            }
            .semantics {
                contentDescription = label
                stateDescription = "%${value * PERCENT / max}"
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 0f..max.toFloat(), STEP_COUNT)
                if (enabled) {
                    setProgress { v ->
                        change(v.roundToInt().coerceIn(0, max))
                        true
                    }
                }
            }
            .drawBehind {
                val stroke = KNOB_STROKE.toPx()
                val inset = stroke / 2f
                val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                val style = Stroke(stroke, cap = StrokeCap.Round)
                drawArc(track, KNOB_START_DEG, KNOB_SWEEP_DEG, false, Offset(inset, inset), arcSize, style = style)
                if (fraction > 0f) {
                    drawArc(arc, KNOB_START_DEG, KNOB_SWEEP_DEG * fraction, false, Offset(inset, inset), arcSize, style = style)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // The face turns from the start of the sweep; its dot always points where the arc ends.
        Box(
            Modifier
                .size(KNOB_SIZE - KNOB_STROKE * 3)
                .graphicsLayer { rotationZ = KNOB_SWEEP_DEG * fraction - KNOB_HALF_SWEEP_DEG }
                .clip(faceShape)
                .background(scheme.primaryContainer),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(Modifier.padding(top = 10.dp).size(10.dp).clip(CircleShape).background(scheme.onPrimaryContainer))
        }
        Text(
            "%${value * PERCENT / max}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = scheme.onPrimaryContainer,
        )
    }
}

private const val STEP = 10
private const val STEP_COUNT = 99

/** A thick, fully round slider: the fill is a pill that grows from the left and ends in a small bar handle. */
@Composable
fun PillSlider(
    label: String,
    value: Int,
    max: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    var widthPx by remember { mutableFloatStateOf(1f) }
    val fill = scheme.primary
    val handle = scheme.onPrimary

    fun set(x: Float) {
        val next = ((x / widthPx).coerceIn(0f, 1f) * max / STEP).roundToInt() * STEP
        if (next != current) {
            haptics.step()
            change(next.coerceIn(0, max))
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(PILL_HEIGHT)
            .clip(CircleShape)
            .background(scheme.surfaceContainerHighest)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(enabled) { if (enabled) detectTapGestures { set(it.x) } }
            .pointerInput(enabled) {
                if (enabled) detectHorizontalDragGestures(onDragStart = { set(it.x) }) { c, _ ->
                    c.consume()
                    set(c.position.x)
                }
            }
            .semantics {
                contentDescription = label
                stateDescription = "%${value * PERCENT / max}"
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 0f..max.toFloat(), STEP_COUNT)
                if (enabled) {
                    setProgress { v ->
                        change(v.roundToInt().coerceIn(0, max))
                        true
                    }
                }
            }
            .drawBehind {
                val h = size.height
                // The fill is never narrower than the track is tall, so even 0% shows a round end.
                val w = maxOf(h, size.width * value / max)
                drawRoundRect(fill, Offset.Zero, Size(w, h), CornerRadius(h / 2f))
                val barH = h * 0.5f
                drawRoundRect(handle, Offset(w - h * 0.4f, (h - barH) / 2f), Size(h * 0.1f, barH), CornerRadius(h * 0.05f))
            },
    )
}

/** A wavy bar: the wave grows with the level. Drag across it to set the value. */
@Composable
fun WavyLevel(
    label: String,
    value: Int,
    max: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptics = rememberHaptics()
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    var widthPx by remember { mutableFloatStateOf(1f) }

    fun set(x: Float) {
        val next = ((x / widthPx).coerceIn(0f, 1f) * max / STEP).roundToInt() * STEP
        if (next != current) {
            haptics.step()
            change(next.coerceIn(0, max))
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(PILL_HEIGHT)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(enabled) { if (enabled) detectTapGestures { set(it.x) } }
            .pointerInput(enabled) {
                if (enabled) detectHorizontalDragGestures(onDragStart = { set(it.x) }) { c, _ ->
                    c.consume()
                    set(c.position.x)
                }
            }
            .semantics {
                contentDescription = label
                stateDescription = "%${value * PERCENT / max}"
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 0f..max.toFloat(), STEP_COUNT)
                if (enabled) {
                    setProgress { v ->
                        change(v.roundToInt().coerceIn(0, max))
                        true
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        LinearWavyProgressIndicator(
            progress = { value.toFloat() / max },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
