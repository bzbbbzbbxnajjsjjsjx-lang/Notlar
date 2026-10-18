package com.vardiya.notlar.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.vardiya.notlar.domain.ChecklistItem
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.NoteColor
import com.vardiya.notlar.domain.NoteSort
import com.vardiya.notlar.domain.ReminderRepeat
import com.vardiya.notlar.domain.TextSize
import com.vardiya.notlar.domain.Section
import com.vardiya.notlar.domain.Settings
import com.vardiya.notlar.domain.ThemeMode
import com.vardiya.notlar.ui.screens.EditorScreen
import com.vardiya.notlar.ui.screens.LabelManagerContent
import com.vardiya.notlar.ui.screens.LabelPickerContent
import com.vardiya.notlar.ui.screens.ReminderContent
import com.vardiya.notlar.ui.screens.TextSizeContent
import com.vardiya.notlar.domain.LockTimeout
import com.vardiya.notlar.ui.screens.LockScreen
import com.vardiya.notlar.ui.screens.MainScreen
import com.vardiya.notlar.ui.screens.PinSetupScreen
import com.vardiya.notlar.ui.screens.PrivacyActions
import com.vardiya.notlar.ui.screens.SettingsContent
import com.vardiya.notlar.ui.theme.NotlarTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Renders every screen, light and dark, into `app/src/test/screenshots/`.
 * Record with `./gradlew recordRoborazziDebug`, check with `./gradlew verifyRoborazziDebug`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// SDK 34: newer Robolectric images need JDK 21 and extra --add-opens; CI runs JDK 17.
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val nowTime = LocalDateTime.of(2026, 10, 16, 14, 32)
    private val now = nowTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val day = 24L * 60 * 60 * 1000

    private val market = Note(
        id = 1,
        title = "Market",
        isChecklist = true,
        items = listOf(
            ChecklistItem(1, "Süt"),
            ChecklistItem(2, "Ekmek"),
            ChecklistItem(3, "Domates, salatalık"),
            ChecklistItem(4, "Yumurta", checked = true),
        ),
        color = NoteColor.YELLOW,
        pinned = true,
        updatedAt = now - 1_000,
        reminderAt = nowTime.plusDays(1).withHour(9).withMinute(0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        labels = listOf("Alışveriş"),
    )
    private val notes = listOf(
        market,
        Note(
            id = 2,
            title = "Vardiya planı",
            body = "Pazartesi sabah, çarşamba gece.\nCuma izin.",
            color = NoteColor.GREEN,
            pinned = true,
            updatedAt = now - 2_000,
            labels = listOf("İş"),
        ),
        Note(3, title = "Fikirler", body = "Not uygulamasına ana ekran widget'ı eklensin.", updatedAt = now - 3_000),
        Note(
            4,
            title = "Kira",
            body = "Ayın 5'inde ödenecek.",
            color = NoteColor.SAND,
            updatedAt = now - 4_000,
            reminderAt = nowTime.withDayOfMonth(5).plusMonths(1).withHour(10).withMinute(0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            reminderRepeat = ReminderRepeat.MONTHLY,
            labels = listOf("Ev"),
        ),
        Note(5, body = "Akşam annemi ara.", updatedAt = now - 5_000),
        Note(6, title = "Kitaplar", body = "Kürk Mantolu Madonna\nSimyacı\nTutunamayanlar", color = NoteColor.RED, updatedAt = now - 6_000),
        Note(7, title = "Eski adres", body = "Atatürk Cad. No 12", archived = true, updatedAt = now - 7_000),
        Note(8, title = "Toplantı notu", body = "Pazartesi 10:00", trashedAt = now - 2 * day, updatedAt = now - 8_000),
    )

    @Test fun notes() = capture("notes") { Main(Section.NOTES) }

    @Test @Config(qualifiers = "+night")
    fun notesDark() = capture("notes_dark") { Main(Section.NOTES) }

    @Test fun archive() = capture("archive") { Main(Section.ARCHIVE) }

    @Test fun trash() = capture("trash") { Main(Section.TRASH) }

    @Test fun empty() = capture("empty") { Main(Section.NOTES, emptyList()) }

    @Test fun editorChecklist() = capture("editor_checklist") { Editor(market) }

    @Test @Config(qualifiers = "+night")
    fun editorTextDark() = capture("editor_text_dark") { Editor(notes[1]) }

    @Test fun notesList() = capture("notes_list") { Main(Section.NOTES, settings = Settings(listView = true)) }

    @Test fun selection() = capture("selection") { Main(Section.NOTES, selection = setOf(1L, 3L)) }

    @Test fun notesDynamic() = capture("notes_dynamic", dynamicColor = true) { Main(Section.NOTES) }

    @Test fun fabMenu() = capture("fab_menu", interact = { compose.onNodeWithContentDescription("Yeni not menüsü").performClick() }) {
        Main(Section.NOTES)
    }

    // The settings sheet is taller than a phone; a tall screen shows every group.
    @Test @Config(qualifiers = "+h1600dp")
    fun settings() = capture("settings") { Settings(Settings(dynamicColor = true, sort = NoteSort.TITLE)) }

    @Test @Config(qualifiers = "+h1600dp")
    fun settingsPrivacy() = capture("settings_privacy") {
        Settings(Settings(lockEnabled = true, biometricUnlock = true, lockTimeout = LockTimeout.ONE_MINUTE, hideContent = true))
    }

    @Test fun lock() = capture("lock") {
        LockScreen(onPin = { UnlockResult.Opened }, biometricAvailable = true, onForgetPin = {})
    }

    @Test @Config(qualifiers = "+night")
    fun lockWrongDark() = capture("lock_wrong_dark", interact = {
        "1234".forEach { compose.onNodeWithText(it.toString()).performClick() }
        compose.onNodeWithText("Tamam").performClick()
    }) {
        LockScreen(onPin = { UnlockResult.Wrong(triesLeft = 4, lockedOutUntil = 0) }, onForgetPin = {})
    }

    @Test fun editorReading() = capture("editor_reading") {
        val note = Note(
            9,
            title = "Hafta sonu",
            body = "# Yapılacaklar\n- **Market** alışverişi\n- Kitabı *bitir*\n\nAkşam arkadaşlarla yemek.",
            updatedAt = now - 1_000,
        )
        Editor(note, Settings(readingView = true))
    }

    @Test fun notesManualOrder() = capture("notes_manual") {
        Main(Section.NOTES, settings = Settings(sort = NoteSort.MANUAL, manualOrder = listOf(3, 5, 4)))
    }

    @Test @Config(qualifiers = "+h1700dp")
    fun settingsBackup() = capture("settings_backup") {
        Settings(Settings(sort = NoteSort.MANUAL, autoBackup = true, backupFolder = "content://backup", lastBackupAt = now - day))
    }

    @Test fun editorLargeText() = capture("editor_large_text") { Editor(notes[1], Settings(textSize = TextSize.HUGE)) }

    @Test fun reminderSheet() = capture("reminder_sheet") {
        Sheet {
            ReminderContent(
                reminderAt = market.reminderAt,
                repeat = ReminderRepeat.WEEKLY,
                now = nowTime,
                onQuick = {},
                onRepeatChange = {},
                onPickDate = {},
                onRemove = {},
            )
        }
    }

    @Test fun labelPicker() = capture("label_picker") {
        Sheet { LabelPickerContent(labels = listOf("İş", "Ev", "Alışveriş"), noteLabels = listOf("Alışveriş"), onToggle = {}, onCreate = {}) }
    }

    @Test @Config(qualifiers = "+night")
    fun labelManagerDark() = capture("label_manager_dark") {
        Sheet { LabelManagerContent(labels = listOf("İş", "Ev", "Alışveriş"), onCreate = {}, onRename = {}, onDelete = {}) }
    }

    @Test fun textSize() = capture("text_size") { Sheet { TextSizeContent(Settings(textSize = TextSize.LARGE), onSettingsChange = {}) } }

    @Test fun pinSetup() = capture("pin_setup") { PinSetupScreen(onPinConfirm = {}, onCancel = {}) }

    @Test @Config(qualifiers = "+h1600dp-night")
    fun settingsDark() = capture("settings_dark") { Settings(Settings(themeMode = ThemeMode.DARK)) }

    @Composable
    private fun Main(
        section: Section,
        list: List<Note> = notes,
        settings: Settings = Settings(),
        selection: Set<Long> = emptySet(),
    ) {
        MainScreen(
            notes = list,
            loaded = true,
            section = section,
            settings = settings,
            selection = selection,
            onSelectionChange = {},
            onSectionChange = {},
            onOpen = {},
            onNewNote = {},
            onNewChecklist = {},
            onPasteNote = {},
            onRestore = {},
            onDeleteForever = {},
            onEmptyTrash = {},
            onToggleListView = {},
            onOpenSettings = {},
            onPinSelection = {},
            onArchiveSelection = {},
            onTrashSelection = {},
            onSwipeArchive = {},
            onSwipeTrash = {},
            onRememberSearch = {},
            onForgetSearch = {},
            snackbarHostState = SnackbarHostState(),
            nowMillis = now,
        )
    }

    @Composable
    private fun Editor(note: Note, settings: Settings = Settings()) {
        EditorScreen(
            initial = note,
            settings = settings,
            onSave = {},
            onClose = {},
            onShare = {},
            onArchive = {},
            onTrash = {},
            onDuplicate = {},
            today = nowTime.toLocalDate(),
        )
    }

    /** A sheet's content on a sheet-colored surface (a real sheet needs a window). */
    @Composable
    private fun Sheet(content: @Composable () -> Unit) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge) {
            Box(Modifier.padding(top = 24.dp)) { content() }
        }
    }

    /** The settings sheet's content on a sheet-colored surface (a real sheet needs a window). */
    @Composable
    private fun Settings(settings: Settings) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge) {
            SettingsContent(
                settings = settings,
                onSettingsChange = {},
                privacy = PrivacyActions(onLockChange = {}, onChangePin = {}, biometricAvailable = true),
                onExport = {},
                onImport = {},
                modifier = Modifier.padding(top = 24.dp),
                dynamicColorAvailable = true,
            )
        }
    }

    private fun capture(
        name: String,
        dynamicColor: Boolean = false,
        interact: () -> Unit = {},
        screen: @Composable () -> Unit,
    ) {
        compose.mainClock.autoAdvance = false
        compose.setContent { NotlarTheme(dynamicColor = dynamicColor) { screen() } }
        compose.mainClock.advanceTimeBy(1_000)
        // Taps need the clock running to be delivered; it stops again before the capture.
        compose.mainClock.autoAdvance = true
        interact()
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(1_000)
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
