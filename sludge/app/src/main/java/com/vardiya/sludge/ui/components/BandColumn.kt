package com.vardiya.sludge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vardiya.sludge.domain.CurveMath
import com.vardiya.sludge.domain.MAX_GAIN_DB
import java.util.Locale

private val TR = Locale.forLanguageTag("tr-TR")
private const val STEPS = (MAX_GAIN_DB * 2 * 2).toInt() - 1

fun formatDb(db: Float): String = String.format(TR, "%+.1f dB", db).replace("+0,0", "0,0").replace("-0,0", "0,0")

/**
 * One band of the equalizer as a rectangular column: drag or tap anywhere on it to set the gain, which fills
 * from the 0 dB line up (boost) or down (cut). Screen readers get a slider with half-decibel steps.
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
    val current by rememberUpdatedState(db)
    val change by rememberUpdatedState(onChange)
    var heightPx by remember { mutableFloatStateOf(1f) }

    fun set(y: Float) {
        val value = CurveMath.snap(MAX_GAIN_DB - (y / heightPx).coerceIn(0f, 1f) * 2f * MAX_GAIN_DB)
        if (value != current) {
            haptics.step()
            change(value)
        }
    }

    Column(modifier.alpha(if (enabled) 1f else DISABLED_ALPHA), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            formatDb(db),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
        )
        val boost = scheme.primary
        val cut = scheme.tertiary
        val guide = scheme.outline
        val track = scheme.surfaceContainerHighest
        Column(
            Modifier
                .padding(vertical = 6.dp)
                .width(56.dp)
                .height(220.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(track)
                .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
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
                    stateDescription = formatDb(db)
                    progressBarRangeInfo = ProgressBarRangeInfo(db, -MAX_GAIN_DB..MAX_GAIN_DB, STEPS)
                    if (enabled) {
                        setProgress { value ->
                            change(CurveMath.snap(value.coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB)))
                            true
                        }
                    }
                }
                .drawBehind { drawColumn(db, boost, cut, guide) },
        ) {}
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Suppress("MagicNumber") // drawing proportions
private fun DrawScope.drawColumn(db: Float, boost: Color, cut: Color, guide: Color) {
    val mid = size.height / 2f
    val y = mid - db / MAX_GAIN_DB * mid
    // Faint marks every 6 dB.
    for (mark in listOf(-6f, 6f)) {
        val my = mid - mark / MAX_GAIN_DB * mid
        drawLine(guide.copy(alpha = 0.35f), Offset(size.width * 0.3f, my), Offset(size.width * 0.7f, my), strokeWidth = 2f)
    }
    val color = if (db >= 0f) boost else cut
    drawRect(color.copy(alpha = 0.85f), topLeft = Offset(0f, minOf(y, mid)), size = Size(size.width, kotlin.math.abs(y - mid)))
    drawLine(guide, Offset(0f, mid), Offset(size.width, mid), strokeWidth = 3f)
    // The handle: a short, thick bar across the column.
    val handleH = 10.dp.toPx()
    drawRoundRect(
        color,
        topLeft = Offset(4.dp.toPx(), (y - handleH / 2f).coerceIn(0f, size.height - handleH)),
        size = Size(size.width - 8.dp.toPx(), handleH),
        cornerRadius = CornerRadius(handleH / 2f),
    )
}

private const val DISABLED_ALPHA = 0.38f
