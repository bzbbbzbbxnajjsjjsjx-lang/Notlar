package com.vardiya.sludge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vardiya.sludge.domain.DayCount
import com.vardiya.sludge.domain.Genre
import com.vardiya.sludge.domain.GenreShare
import com.vardiya.sludge.domain.chartLabel
import java.time.format.TextStyle
import kotlin.math.roundToInt
import java.util.Locale

private val TR = Locale.forLanguageTag("tr-TR")

// One colour per genre, the same everywhere a genre is shown. Two sets, so each reads on its own background.
private val DarkSet = mapOf(
    Genre.STONER to Color(0xFFD2A8F7),
    Genre.DOOM to Color(0xFFA9CBFF),
    Genre.PSYCH to Color(0xFFC9EB6E),
    Genre.NOISE_ROCK to Color(0xFFF2877D),
    Genre.HARSH_NOISE to Color(0xFFF5C16C),
    Genre.DRONE to Color(0xFF7FD6C8),
    Genre.SHOEGAZE to Color(0xFFF28DB5),
    Genre.FLAT to Color(0xFF9D98C6),
)
private val LightSet = mapOf(
    Genre.STONER to Color(0xFF6C3CB0),
    Genre.DOOM to Color(0xFF3A5BA0),
    Genre.PSYCH to Color(0xFF4C6700),
    Genre.NOISE_ROCK to Color(0xFFB3261E),
    Genre.HARSH_NOISE to Color(0xFF8A5A00),
    Genre.DRONE to Color(0xFF00695C),
    Genre.SHOEGAZE to Color(0xFFA3225F),
    Genre.FLAT to Color(0xFF6E6A85),
)

@Composable
fun genreColor(genre: Genre): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < DARK_LUMINANCE
    return (if (dark) DarkSet else LightSet).getValue(genre)
}

private const val DARK_LUMINANCE = 0.5f

/** The listening split as a ring of rounded arcs with gaps, the total in the middle. */
@Composable
fun GenreRing(shares: List<GenreShare>, total: Int, modifier: Modifier = Modifier, size: Dp = 200.dp, stroke: Dp = 28.dp) {
    val colors = shares.map { genreColor(it.genre) }
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val width = stroke.toPx()
            val inset = width / 2f
            val arcSize = Size(this.size.width - width, this.size.height - width)
            val radius = arcSize.width / 2f
            drawCircle(track, radius, center, style = Stroke(width))
            // Round caps reach half a stroke past each end, so the arcs are shortened by that and the gap added.
            val capDeg = Math.toDegrees((inset / radius).toDouble()).toFloat()
            val gapDeg = GAP_DEG + 2f * capDeg
            val usable = FULL_DEG - gapDeg * shares.size
            var start = START_DEG
            shares.forEachIndexed { i, share ->
                val sweep = (usable * share.fraction).coerceAtLeast(0.1f)
                drawArc(
                    colors[i], start + gapDeg / 2f, sweep, false, Offset(inset, inset), arcSize,
                    style = Stroke(width, cap = StrokeCap.Round),
                )
                start += sweep + gapDeg
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$total", style = MaterialTheme.typography.displayMediumEmphasized)
            Text("şarkı", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private const val FULL_DEG = 360f
private const val START_DEG = -90f
private const val GAP_DEG = 4f

/** One legend row of the ring: the genre's colour, name, count and share. */
@Composable
fun GenreLegendRow(share: GenreShare, modifier: Modifier = Modifier) {
    val percent = (share.fraction * PERCENT).roundToInt()
    Row(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = "${share.genre.chartLabel}: ${share.count} şarkı, yüzde $percent"
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(genreColor(share.genre)))
        Text(share.genre.chartLabel, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Text("${share.count}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "%$percent",
            Modifier.width(48.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
        )
    }
}

private const val PERCENT = 100
private val DAY_TRACK = 132.dp
private val DAY_PILL = 28.dp

/** Seven days as round-ended pills that grow with the day's plays; today is the lime one, an empty day is a dot. */
@Composable
fun DayPills(days: List<DayCount>, modifier: Modifier = Modifier) {
    val peak = (days.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    val scheme = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEachIndexed { index, day ->
            val label = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, TR)
            Column(
                Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = "$label: ${day.count} şarkı" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("${day.count}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Box(Modifier.height(DAY_TRACK), contentAlignment = Alignment.BottomCenter) {
                    val color = if (index == days.lastIndex) scheme.tertiary else scheme.primary
                    if (day.count > 0) {
                        val height = (DAY_TRACK * day.count / peak).coerceAtLeast(DAY_PILL)
                        Box(Modifier.width(DAY_PILL).height(height).clip(CircleShape).background(color))
                    } else {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(scheme.outlineVariant))
                    }
                }
                Text(
                    label,
                    Modifier.clearAndSetSemantics {},
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}
