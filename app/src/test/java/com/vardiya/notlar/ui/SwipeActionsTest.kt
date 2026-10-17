package com.vardiya.notlar.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import com.vardiya.notlar.ui.components.SwipeAction
import com.vardiya.notlar.ui.components.SwipeActions
import com.vardiya.notlar.ui.theme.NotlarTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SwipeActionsTest {

    @get:Rule
    val compose = createComposeRule()

    /** Undo after a swipe brings the same key back; the card must be at rest, not still swiped away. */
    @Test
    fun itemSwipedAwayComesBackAtRest() {
        val notes = mutableStateListOf("Market")
        compose.setContent {
            NotlarTheme {
                LazyColumn {
                    items(notes, key = { it }) { note ->
                        SwipeActions(
                            startAction = SwipeAction(Icons.Outlined.Archive, "Arşivle", Color.Green, Color.Black) { notes.remove(note) },
                            endAction = null,
                        ) {
                            Text(
                                note,
                                Modifier
                                    .fillMaxWidth()
                                    .height(64.dp),
                            )
                        }
                    }
                }
            }
        }

        compose.onNodeWithText("Market").performTouchInput { swipeRight() }
        compose.waitForIdle()
        assertEquals(0, notes.size)

        compose.runOnIdle { notes.add("Market") }
        compose.waitForIdle()
        val left = compose.onNodeWithText("Market").getUnclippedBoundsInRoot().left
        assertEquals(0f, left.value, 0.5f)
    }
}
