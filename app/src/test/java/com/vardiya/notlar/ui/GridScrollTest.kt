package com.vardiya.notlar.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.Section
import com.vardiya.notlar.domain.Settings
import com.vardiya.notlar.ui.screens.MainScreen
import com.vardiya.notlar.ui.theme.NotlarTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GridScrollTest {

    @get:Rule
    val compose = createComposeRule()

    /** The trash's large title bar must not swallow the scrolling of a list it is not shown on. */
    @Test
    fun notesScrollOnFirstOpen() {
        val notes = (0 until 40).map { Note(it.toLong(), title = "Not $it", updatedAt = 100L - it) }
        compose.setContent {
            NotlarTheme {
                MainScreen(
                    notes = notes, loaded = true, section = Section.NOTES, settings = Settings(listView = true),
                    selection = emptySet(), onSelectionChange = {}, onSectionChange = {},
                    onOpen = {}, onNewNote = {}, onNewChecklist = {}, onPasteNote = {},
                    onRestore = {}, onDeleteForever = {}, onEmptyTrash = {}, onToggleListView = {}, onOpenSettings = {},
                    onPinSelection = {}, onArchiveSelection = {}, onTrashSelection = {}, onSwipeArchive = {}, onSwipeTrash = {},
                    onRememberSearch = {}, onForgetSearch = {},
                    snackbarHostState = SnackbarHostState(),
                )
            }
        }
        compose.onNodeWithText("Not 0").assertIsDisplayed()
        repeat(3) {
            compose.onRoot().performTouchInput {
                swipeUp(startY = bottom * 0.8f, endY = top + bottom * 0.2f, durationMillis = 600)
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Not 0").assertIsNotDisplayed()
    }
}
