package com.vardiya.sludge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vardiya.sludge.domain.MAX_GAIN_DB

/** Section title in the primary color, marked as a heading for screen readers. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleLargeEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * A whole-row toggle: the row is the 48dp+ touch target and the Switch inside only shows the state. The thumb
 * carries a check mark when on and a cross when off.
 */
@Composable
fun SwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val haptics = rememberHaptics()
    Row(
        modifier = modifier
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = {
                    haptics.toggle(it)
                    onCheckedChange(it)
                },
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
        CheckSwitch(checked = checked, enabled = enabled, modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
fun CheckSwitch(checked: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = null,
        enabled = enabled,
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
        modifier = modifier,
    )
}

/** A scalloped "cookie" badge, the Material 3 Expressive shape for status marks and avatars. */
@Composable
fun CookieBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    polygon: androidx.graphics.shapes.RoundedPolygon = MaterialShapes.Cookie9Sided,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    contentDescription: String? = null,
) {
    Box(
        modifier
            .size(size)
            .clip(polygon.toShape())
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = content, modifier = Modifier.size(size / 2))
    }
}

/** The EQ curve as a smooth line over a faint 0 dB guide; used big on the main screen and small on presets. */
@Composable
fun CurvePreview(
    bands: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    guideColor: Color = MaterialTheme.colorScheme.outlineVariant,
    lineWidth: Float = 3f,
    /** A second curve drawn dashed on the same scale, e.g. the sound after the headphone correction. */
    overlay: List<Float>? = null,
    overlayColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    Canvas(modifier) {
        val pad = lineWidth * 2
        val mid = size.height / 2f
        val half = mid - pad
        fun path(values: List<Float>): Path {
            val points = values.indices.map { i ->
                Offset(
                    x = pad + (size.width - 2 * pad) * i / (values.size - 1).coerceAtLeast(1),
                    y = mid - values[i].coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB) / MAX_GAIN_DB * half,
                )
            }
            return Path().apply {
                moveTo(points[0].x, points[0].y)
                // Catmull-Rom through the points, written as cubic segments.
                for (i in 0 until points.lastIndex) {
                    val p0 = points[(i - 1).coerceAtLeast(0)]
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val p3 = points[(i + 2).coerceAtMost(points.lastIndex)]
                    cubicTo(
                        p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f,
                        p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f,
                        p2.x, p2.y,
                    )
                }
            }
        }
        drawLine(
            guideColor, Offset(0f, mid), Offset(size.width, mid), strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
        )
        if (bands.size < 2) return@Canvas
        overlay?.takeIf { it.size >= 2 }?.let {
            drawPath(
                path(it),
                overlayColor,
                style = Stroke(width = lineWidth, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
            )
        }
        drawPath(path(bands), lineColor, style = Stroke(width = lineWidth, cap = StrokeCap.Round))
    }
}
