package com.vardiya.notlar.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vardiya.notlar.domain.Markdown
import com.vardiya.notlar.domain.TextEdit
import com.vardiya.notlar.ui.components.Haptics
import com.vardiya.notlar.ui.components.NoteImage

private val IMAGE_SIZE = 140.dp
private const val REMOVE_DELAY_MS = 260L

/** The text field of a text note; a [TextFieldValue] so the formatting row knows the selection. */
@Composable
internal fun BodyField(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, style: TextStyle, modifier: Modifier = Modifier) {
    val content = LocalContentColor.current
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = style,
        placeholder = { Text("Not", style = style, color = content.copy(alpha = 0.6f)) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = content,
            unfocusedTextColor = content,
        ),
    )
}

/** Bold, italic, heading and bullet for the text note, and the way to add a picture. */
@Composable
internal fun FormatRow(
    body: TextFieldValue,
    haptics: Haptics,
    onEdit: (TextEdit) -> Unit,
    onAddImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val edit = TextEdit(body.text, body.selection.start, body.selection.end)
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 3.dp) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            FormatButton(Icons.Filled.FormatBold, "Kalın", Markdown.isBold(edit), haptics) { onEdit(Markdown.bold(edit)) }
            FormatButton(Icons.Filled.FormatItalic, "İtalik", Markdown.isItalic(edit), haptics) { onEdit(Markdown.italic(edit)) }
            FormatButton(Icons.Filled.Title, "Başlık", Markdown.isHeading(edit), haptics) { onEdit(Markdown.heading(edit)) }
            FormatButton(Icons.Filled.FormatListBulleted, "Madde işareti", Markdown.isBullet(edit), haptics) { onEdit(Markdown.bullet(edit)) }
            IconButton(
                onClick = {
                    haptics.tick()
                    onAddImage()
                },
                shapes = IconButtonDefaults.shapes(),
            ) { Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Resim ekle") }
        }
    }
}

@Composable
private fun FormatButton(icon: ImageVector, description: String, on: Boolean, haptics: Haptics, onClick: () -> Unit) {
    IconToggleButton(
        checked = on,
        onCheckedChange = {
            haptics.toggle(it)
            onClick()
        },
        shapes = IconButtonDefaults.toggleableShapes(),
    ) { Icon(icon, contentDescription = description) }
}

/** The pictures of the note in a row under the text; each can be opened or removed. */
@Composable
internal fun ImageStrip(images: List<String>, haptics: Haptics, onOpen: (String) -> Unit, onRemove: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        images.forEach { name -> key(name) { ImageThumb(name, haptics, onOpen, onRemove) } }
    }
}

@Composable
private fun ImageThumb(name: String, haptics: Haptics, onOpen: (String) -> Unit, onRemove: (String) -> Unit) {
    // Fades and shrinks sideways in on arrival and out before the picture is really removed.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val currentRemove by rememberUpdatedState(onRemove)
    AnimatedVisibility(
        visible = shown,
        enter = expandHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        exit = shrinkHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
    ) {
        Box(Modifier.size(IMAGE_SIZE)) {
            NoteImage(
                name,
                maxSide = 480,
                contentDescription = "Notun resmi, büyütmek için dokun",
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).clickable {
                    haptics.tick()
                    onOpen(name)
                },
            )
            IconButton(
                onClick = {
                    haptics.tick()
                    shown = false
                },
                modifier = Modifier.align(Alignment.TopEnd),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) { Icon(Icons.Filled.Close, contentDescription = "Resmi kaldır", Modifier.size(18.dp)) }
        }
    }
    // Once the exit has played, the picture leaves the note.
    var left by remember { mutableStateOf(false) }
    LaunchedEffect(shown) {
        if (shown) {
            left = true
        } else if (left) {
            delay(REMOVE_DELAY_MS)
            currentRemove(name)
        }
    }
}

/** A picture shown large; a tap anywhere closes it. */
@Composable
internal fun ImageDialog(name: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onDismiss)) {
            NoteImage(
                name,
                maxSide = 1600,
                contentDescription = "Notun resmi",
                modifier = Modifier.fillMaxWidth().padding(4.dp).clip(RoundedCornerShape(24.dp)).size(width = 400.dp, height = 480.dp),
            )
        }
    }
}

/** The two ways to add a picture. */
@Composable
internal fun AddImageSheet(onGallery: () -> Unit, onCamera: () -> Unit, onDismissRequest: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 24.dp)) {
            SheetRow(Icons.Outlined.PhotoLibrary, "Galeriden seç", onGallery)
            SheetRow(Icons.Outlined.CameraAlt, "Fotoğraf çek", onCamera)
        }
    }
}

@Composable
private fun SheetRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
