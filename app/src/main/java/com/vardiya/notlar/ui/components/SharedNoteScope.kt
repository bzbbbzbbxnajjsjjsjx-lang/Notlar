package com.vardiya.notlar.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/** The scopes a note card and its editor share, so the card grows into the editor and back. */
@OptIn(ExperimentalSharedTransitionApi::class)
class SharedNoteScope(
    val transition: SharedTransitionScope,
    val visibility: AnimatedVisibilityScope,
)

/** Set by the app around the list/editor switch; null in previews and screenshot tests. */
val LocalSharedNoteScope = staticCompositionLocalOf<SharedNoteScope?> { null }

/** Marks the bounds of note [id]; the same mark on its card and its editor links the two. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedNoteBounds(id: Long): Modifier {
    val scope = LocalSharedNoteScope.current ?: return this
    return with(scope.transition) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(key = "note-$id"),
            animatedVisibilityScope = scope.visibility,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
        )
    }
}
