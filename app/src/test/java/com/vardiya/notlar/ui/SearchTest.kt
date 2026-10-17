package com.vardiya.notlar.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.Section
import com.vardiya.notlar.domain.Settings
import com.vardiya.notlar.ui.screens.MainScreen
import com.vardiya.notlar.ui.theme.NotlarTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SearchTest {

    @get:Rule
    val compose = createComposeRule()

    private val opened = mutableListOf<Long>()
    private val remembered = mutableListOf<String>()
    private val forgotten = mutableListOf<String>()

    private fun setUp() {
        val notes = listOf(Note(1, title = "Kira", body = "Ayın 5'i"), Note(2, title = "Market", body = "Süt"))
        compose.setContent {
            NotlarTheme {
                MainScreen(
                    notes = notes, loaded = true, section = Section.NOTES,
                    settings = Settings(recentSearches = listOf("vardiya")),
                    selection = emptySet(), onSelectionChange = {}, onSectionChange = {},
                    onOpen = { opened += it.id }, onNewNote = {}, onNewChecklist = {}, onPasteNote = {},
                    onRestore = {}, onDeleteForever = {}, onEmptyTrash = {}, onToggleListView = {}, onOpenSettings = {},
                    onPinSelection = {}, onArchiveSelection = {}, onTrashSelection = {}, onSwipeArchive = {}, onSwipeTrash = {},
                    onRememberSearch = { remembered += it }, onForgetSearch = { forgotten += it },
                    snackbarHostState = SnackbarHostState(),
                )
            }
        }
    }

    @Test
    fun openingTheSearchShowsRecentSearches() {
        setUp()
        compose.onAllNodes(hasSetTextAction()).onFirst().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Son aramalar").assertExists()
        compose.onNodeWithContentDescription("\"vardiya\" aramasını sil").performClick()
        assertEquals(listOf("vardiya"), forgotten)
    }

    @Test
    fun resultOpensTheNoteAndRemembersTheSearch() {
        setUp()
        compose.onAllNodes(hasSetTextAction()).onFirst().performClick()
        compose.waitForIdle()
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("süt")
        compose.waitForIdle()
        // The result card in the full-screen search is the last "Market" on screen.
        val markets = compose.onAllNodesWithText("Market")
        markets[markets.fetchSemanticsNodes().size - 1].performClick()
        compose.waitForIdle()
        assertEquals(listOf(2L), opened)
        assertEquals(listOf("süt"), remembered)
    }
}
