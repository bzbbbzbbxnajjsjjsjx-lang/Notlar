package com.vardiya.notlar.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.vardiya.notlar.domain.Markdown
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.NoteColor
import com.vardiya.notlar.domain.NoteEdits
import com.vardiya.notlar.ui.theme.container
import com.vardiya.notlar.ui.theme.notePalette
import com.vardiya.notlar.ui.theme.onContainer

private const val PREVIEW_ITEMS = 6
private const val PREVIEW_LINES = 10
private const val THUMB_SIDE = 480
private val THUMB_HEIGHT = 120.dp
private const val HIGHLIGHT_ALPHA = 0.22f
private const val BADGE_START_SCALE = 0.4f
private val BADGE_SIZE = 24.dp
private val RESTING_CORNER = 16.dp
private val PRESSED_CORNER = 24.dp
private val SELECTED_CORNER = 32.dp

/**
 * A note in the grid or list: title, then the start of its text or its first to-do items.
 * A long press starts selection; a [selected] card gets a thick outline and a check badge.
 */
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    highlight: String = "",
    textScale: Float = 1f,
    nowMillis: Long = System.currentTimeMillis(),
) {
    // A locked note shows nothing of itself but a lock; its colour, pin and reminder time stay.
    val note = if (note.locked) {
        note.copy(title = "", body = "", isChecklist = false, items = emptyList(), images = emptyList(), labels = emptyList())
    } else {
        note
    }
    val palette = notePalette
    val interactions = remember { MutableInteractionSource() }
    // Expressive shape motion: corners round out while pressed and more when selected.
    val pressed by interactions.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = when {
            selected -> SELECTED_CORNER
            pressed -> PRESSED_CORNER
            else -> RESTING_CORNER
        },
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "cardCorner",
    )
    val shape = RoundedCornerShape(corner)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactions)
            .clip(shape)
            .semantics { this.selected = selected }
            .combinedClickable(
                interactionSource = interactions,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = "Seç",
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = palette.container(note.color),
            contentColor = palette.onContainer(note.color),
        ),
        border = when {
            selected -> BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
            note.color == NoteColor.DEFAULT -> BorderStroke(1.dp, palette.outlineVariant)
            else -> null
        },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            note.images.firstOrNull()?.let { first ->
                Box {
                    NoteImage(
                        first,
                        maxSide = THUMB_SIDE,
                        contentDescription = "Notun resmi",
                        modifier = Modifier.fillMaxWidth().height(THUMB_HEIGHT).clip(RoundedCornerShape(12.dp)),
                    )
                    if (note.images.size > 1) {
                        Text(
                            "+${note.images.size - 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            if (note.title.isNotBlank() || note.pinned || selected) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        highlighted(note.title, highlight),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    // While selected, the check badge takes the pin's place.
                    if (selected) {
                        SelectedBadge()
                    } else if (note.pinned) {
                        Icon(Icons.Filled.PushPin, contentDescription = "Sabitlendi", modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (note.locked) LockedPreview()
            when {
                note.isChecklist -> ChecklistPreview(note, highlight, textScale)
                note.body.isNotBlank() -> Text(
                    highlighted(Markdown.strip(note.body.trim()), highlight),
                    style = MaterialTheme.typography.bodyMedium.scaled(textScale),
                    maxLines = PREVIEW_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            NoteChips(note, nowMillis = nowMillis)
        }
    }
}

/** What a locked note shows in place of its text. */
@Composable
private fun LockedPreview() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
        Text("Kilitli not", style = MaterialTheme.typography.titleMedium)
    }
}

/** A check on a shape that grows out of a circle into a nine-sided cookie as it appears. */
@Composable
private fun SelectedBadge() {
    val progress = remember { Animatable(0f) }
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    LaunchedEffect(Unit) { progress.animateTo(1f, spec) }
    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
    Box(
        Modifier
            .size(BADGE_SIZE)
            .graphicsLayer {
                scaleX = BADGE_START_SCALE + (1 - BADGE_START_SCALE) * progress.value
                scaleY = scaleX
            }
            .background(MaterialTheme.colorScheme.primary, MorphShape(morph, progress.value)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Check,
            contentDescription = "Seçildi",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun ChecklistPreview(note: Note, highlight: String, textScale: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NoteEdits.progress(note)?.let { (done, total) ->
            val content = LocalContentColor.current
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val progress by animateFloatAsState(
                    targetValue = done.toFloat() / total,
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    label = "progress",
                )
                // Expressive wavy bar, held still so a grid of cards does not shimmer.
                LinearWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f),
                    color = content,
                    trackColor = content.copy(alpha = 0.2f),
                    stopSize = 0.dp,
                    waveSpeed = 0.dp,
                )
                Text(
                    "$done/$total",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.semantics { contentDescription = "$total öğeden $done tamamlandı" },
                )
            }
        }
        val items = note.items.filter { it.text.isNotBlank() }.sortedBy { it.checked }
        items.take(PREVIEW_ITEMS).forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (item.checked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                    contentDescription = if (item.checked) "Tamamlandı" else "Yapılacak",
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(18.dp),
                )
                Text(
                    highlighted(item.text, highlight),
                    style = MaterialTheme.typography.bodyMedium.scaled(textScale),
                    textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (items.size > PREVIEW_ITEMS) {
            Text("+${items.size - PREVIEW_ITEMS} öğe daha", style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** [text] with every match of the search marked on a tinted background. */
@Composable
private fun highlighted(text: String, query: String): AnnotatedString {
    val ranges = NoteEdits.matchRanges(text, query)
    if (ranges.isEmpty()) return AnnotatedString(text)
    val mark = SpanStyle(
        background = LocalContentColor.current.copy(alpha = HIGHLIGHT_ALPHA),
        fontWeight = FontWeight.Bold,
    )
    return buildAnnotatedString {
        append(text)
        ranges.forEach { addStyle(mark, it.first, it.last + 1) }
    }
}
