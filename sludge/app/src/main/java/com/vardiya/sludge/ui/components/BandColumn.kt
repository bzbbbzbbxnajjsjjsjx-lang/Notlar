package com.vardiya.sludge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.vardiya.sludge.domain.CurveMath
import com.vardiya.sludge.domain.MAX_GAIN_DB
import java.util.Locale

private val TR = Locale.forLanguageTag("tr-TR")
private const val STEPS = (MAX_GAIN_DB * 2 * 2).toInt() - 1
private const val DISABLED_ALPHA = 0.38f

/** Width of the capsule; the round handle is smaller, so the bar it moves stays visible. */
val BAND_WIDTH = 52.dp
private val HANDLE_RADIUS = 15.dp
private val BAR_MARGIN = 6.dp

/** Height of every band column; the dB scale beside the columns uses the same value so the marks line up. */
val BAND_HEIGHT = 240.dp

fun formatDb(db: Float): String = String.format(TR, "%+.1f", db).replace("+0,0", "0,0").replace("-0,0", "0,0")

/**
 * One band of the equalizer as a flat, square-cornered column that fills the width it is given: drag or tap
 * anywhere on it to set the gain. The bar grows from the 0 dB line up (boost) or down (cut) and ends in a
 * thick edge. Screen readers get a slider with half-decibel steps.
 */
@Composable
fun BandColumn(
    label: String,
    db: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    val insetPx = with(LocalDensity.current) { HANDLE_RADIUS.toPx() }
    val current by rememberUpdatedState(db)
    val change by rememberUpdatedState(onChange)
    var sizePx by remember { mutableStateOf(IntSize(1, 1)) }

    // The handle is a circle, so the gain runs between its centre at the very top and the very bottom.
    fun set(y: Float) {
        val span = (sizePx.height - 2f * insetPx).coerceAtLeast(1f)
        val value = CurveMath.snap(MAX_GAIN_DB - ((y - insetPx) / span).coerceIn(0f, 1f) * 2f * MAX_GAIN_DB)
        if (value != current) {
            haptics.step()
            change(value)
        }
    }

    Column(modifier.alpha(if (enabled) 1f else DISABLED_ALPHA), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            formatDb(db),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
        )
        val boost = scheme.primary
        val cut = scheme.tertiary
        val grid = scheme.outline
        val edge = scheme.onSurface
        Box(
            Modifier
                .width(BAND_WIDTH)
                .height(BAND_HEIGHT)
                .clip(CircleShape)
                .background(scheme.surfaceContainerHighest)
                .onSizeChanged { sizePx = IntSize(it.width.coerceAtLeast(1), it.height.coerceAtLeast(1)) }
                .pointerInput(enabled) {
                    if (enabled) detectTapGestures { set(it.y) }
                }
                .pointerInput(enabled) {
                    if (enabled) {
                        detectVerticalDragGestures(onDragStart = { set(it.y) }) { change, _ ->
                            change.consume()
                            set(change.position.y)
                        }
                    }
                }
                .semantics {
                    contentDescription = "$label bandı"
                    stateDescription = formatDb(db) + " desibel"
                    progressBarRangeInfo = ProgressBarRangeInfo(db, -MAX_GAIN_DB..MAX_GAIN_DB, STEPS)
                    if (enabled) {
                        setProgress { value ->
                            change(CurveMath.snap(value.coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB)))
                            true
                        }
                    }
                }
                .drawBehind { drawBand(db, boost, cut, grid, edge) },
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** The dB scale drawn next to the columns: +12, +6, 0, -6, -12 at the same heights as the grid lines. */
@Composable
fun BandScale(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val style = MaterialTheme.typography.labelSmall
    // Inset by the handle radius: +12 dB is where the handle centre sits at the top, not the capsule's edge.
    Column(
        modifier.height(BAND_HEIGHT).padding(vertical = HANDLE_RADIUS - SCALE_HALF_LINE),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        for (mark in listOf("+12", "+6", "0", "-6", "-12")) {
            Text(mark, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.width(SCALE_WIDTH))
        }
    }
}

private val SCALE_WIDTH = 28.dp
private val SCALE_HALF_LINE = 8.dp

@Suppress("MagicNumber") // drawing proportions
private fun DrawScope.drawBand(db: Float, boost: Color, cut: Color, grid: Color, edge: Color) {
    val inset = HANDLE_RADIUS.toPx()
    val span = size.height - 2f * inset
    val mid = size.height / 2f
    fun yOf(gain: Float) = inset + (MAX_GAIN_DB - gain) / (2f * MAX_GAIN_DB) * span
    val y = yOf(db)
    val color = if (db >= 0f) boost else cut
    // Short guide marks every 6 dB, centred in the capsule.
    for (mark in listOf(-12f, -6f, 6f, 12f)) {
        drawLine(grid.copy(alpha = 0.45f), Offset(size.width * 0.38f, yOf(mark)), Offset(size.width * 0.62f, yOf(mark)), strokeWidth = 1.5.dp.toPx())
    }
    // The bar from the 0 dB line to the handle, rounded at both ends like the capsule around it.
    val barLeft = BAR_MARGIN.toPx()
    val top = minOf(y, mid)
    val bottom = maxOf(y, mid)
    val corner = CornerRadius((size.width - 2f * barLeft) / 2f)
    drawRoundRect(color, Offset(barLeft, top - inset / 2f), Size(size.width - 2f * barLeft, bottom - top + inset), corner)
    drawLine(grid, Offset(size.width * 0.3f, mid), Offset(size.width * 0.7f, mid), strokeWidth = 2.dp.toPx())
    // The round handle: a ring in the text color with a dot of the bar color.
    drawCircle(edge, inset, Offset(size.width / 2f, y))
    drawCircle(color, inset * 0.4f, Offset(size.width / 2f, y))
}
