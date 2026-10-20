package com.vardiya.sludge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vardiya.sludge.domain.FilterScale
import com.vardiya.sludge.domain.FilterType
import com.vardiya.sludge.domain.MAX_GAIN_DB
import com.vardiya.sludge.domain.Parametric
import com.vardiya.sludge.domain.ParametricFilter
import com.vardiya.sludge.ui.SludgeActions
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

private const val CHART_POINTS = 96
private val TURKISH: java.util.Locale = java.util.Locale.forLanguageTag("tr-TR")
private val DECADES = listOf(100f, 1_000f, 10_000f)

/**
 * A response on a log frequency axis, ±12 dB, with a dashed 0 dB guide and a dot per filter. By default it draws the
 * filters' combined response; [curve] draws any other.
 */
@Composable
fun ResponseChart(
    filters: List<ParametricFilter>,
    modifier: Modifier = Modifier,
    curve: (Float) -> Float = { Parametric.responseDb(filters, it) },
    lineColor: Color = MaterialTheme.colorScheme.primary,
    guideColor: Color = MaterialTheme.colorScheme.outlineVariant,
    dotColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    Canvas(modifier) {
        val lo = ln(ParametricFilter.MIN_HZ)
        val hi = ln(ParametricFilter.MAX_HZ)
        val mid = size.height / 2f
        val half = mid - 8f
        fun x(hz: Float) = size.width * (ln(hz) - lo) / (hi - lo)
        fun y(db: Float) = mid - db.coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB) / MAX_GAIN_DB * half
        // Decade guides at 100 Hz, 1 kHz and 10 kHz.
        DECADES.forEach { hz ->
            drawLine(guideColor.copy(alpha = 0.5f), Offset(x(hz), 0f), Offset(x(hz), size.height), strokeWidth = 1f)
        }
        val dashes = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        drawLine(guideColor, Offset(0f, mid), Offset(size.width, mid), strokeWidth = 2f, pathEffect = dashes)
        val ratio = ParametricFilter.MAX_HZ / ParametricFilter.MIN_HZ
        val hz = List(CHART_POINTS) { i -> ParametricFilter.MIN_HZ * ratio.pow(i / (CHART_POINTS - 1f)) }
        val values = hz.map(curve)
        val path = Path().apply {
            hz.indices.forEach { i -> if (i == 0) moveTo(x(hz[i]), y(values[i])) else lineTo(x(hz[i]), y(values[i])) }
        }
        drawPath(path, lineColor, style = Stroke(width = 4f))
        filters.forEach { drawCircle(dotColor, radius = 7f, center = Offset(x(it.freqHz), y(it.gainDb))) }
    }
}

/** The parametric side of the equalizer: the response, one card per filter and a button for another. */
@Composable
fun ParametricEditor(filters: List<ParametricFilter>, enabled: Boolean, actions: SludgeActions, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ResponseChart(filters, Modifier.fillMaxWidth().height(120.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("20 Hz", "100", "1k", "10k", "20k").forEach { Text(it, style = MaterialTheme.typography.labelSmall) }
        }
        filters.forEachIndexed { index, filter ->
            FilterCard(index, filter, enabled, { actions.updateFilter(index, it) }, { actions.removeFilter(index) })
        }
        if (filters.isEmpty()) {
            Text(
                "Henüz filtre yok. Bir ön ayar seç ya da kendin filtre ekle.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        FilledTonalButton(onClick = actions::addFilter, enabled = enabled && filters.size < Parametric.MAX_FILTERS) {
            Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(18.dp))
            Text("Filtre ekle (${filters.size}/${Parametric.MAX_FILTERS})", Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun FilterCard(index: Int, filter: ParametricFilter, enabled: Boolean, onChange: (ParametricFilter) -> Unit, onRemove: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Filtre ${index + 1}: ${formatFreq(filter.freqHz)}, ${formatGain(filter.gainDb)}",
                    Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.titleSmall,
                )
                IconButton(onClick = onRemove, enabled = enabled) { Icon(Icons.Filled.Close, contentDescription = "Filtre ${index + 1} sil") }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterType.entries.forEach { type ->
                    val selected = filter.kind == type
                    FilterChip(
                        selected = selected,
                        enabled = enabled,
                        onClick = { onChange(filter.copy(type = type.key)) },
                        label = { Text(type.label) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
                        } else {
                            null
                        },
                        shape = CircleShape,
                    )
                }
            }
            LabeledSlider("Frekans", formatFreq(filter.freqHz), FilterScale.freqToSlider(filter.freqHz), FilterScale.MAX, enabled) {
                onChange(filter.copy(freqHz = FilterScale.sliderToFreq(it)))
            }
            val gainSlider = FilterScale.gainToSlider(filter.gainDb)
            LabeledSlider("Kazanç", formatGain(filter.gainDb), gainSlider, FilterScale.GAIN_MAX, enabled) {
                onChange(filter.copy(gainDb = FilterScale.sliderToGain(it)))
            }
            val qText = "%.1f".format(TURKISH, filter.q)
            LabeledSlider("Q (darlık)", qText, FilterScale.qToSlider(filter.q), FilterScale.MAX, enabled) {
                onChange(filter.copy(q = FilterScale.sliderToQ(it)))
            }
        }
    }
}

@Composable
private fun LabeledSlider(label: String, shown: String, value: Int, max: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(shown, style = MaterialTheme.typography.labelLarge)
        }
        PillSlider("$label: $shown", value, max, onChange, enabled = enabled)
    }
}

private fun formatFreq(hz: Float): String =
    if (hz >= FilterScale.KILO) "${(hz / FilterScale.HUNDRED).roundToInt() / FilterScale.TEN} kHz".replace('.', ',') else "${hz.roundToInt()} Hz"

private fun formatGain(db: Float): String = "%+.1f dB".format(TURKISH, db)
