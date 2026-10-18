package com.vardiya.notlar.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.vardiya.notlar.data.ImageStore

/** Where the pictures of notes are read from; null in previews and tests, which then show none. */
val LocalImageStore = compositionLocalOf<ImageStore?> { null }

/** A picture of a note, decoded no larger than [maxSide] pixels, fading in once it is ready. */
@Composable
fun NoteImage(name: String, maxSide: Int, contentDescription: String?, modifier: Modifier = Modifier) {
    val store = LocalImageStore.current
    val bitmap by produceState<Bitmap?>(null, name, store) { value = store?.load(name, maxSide) }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        Crossfade(targetState = bitmap, animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(), label = "noteImage") { shown ->
            if (shown != null) {
                Image(shown.asImageBitmap(), contentDescription, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
    }
}
