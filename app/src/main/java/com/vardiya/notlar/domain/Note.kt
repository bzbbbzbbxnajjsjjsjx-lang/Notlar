package com.vardiya.notlar.domain

import kotlinx.serialization.Serializable

/** One row of a to-do note. */
@Serializable
data class ChecklistItem(
    val id: Long,
    val text: String = "",
    val checked: Boolean = false,
)

/** Background of a note card; each maps to a container role of the color scheme. */
@Serializable
enum class NoteColor { DEFAULT, YELLOW, SAND, GREEN, RED }

/**
 * A note is either plain text ([body]) or a to-do list ([items]), never both at once.
 * [trashedAt] is set while the note is in the trash; [archived] keeps it out of the main list.
 */
@Serializable
data class Note(
    val id: Long,
    val title: String = "",
    val body: String = "",
    val isChecklist: Boolean = false,
    val items: List<ChecklistItem> = emptyList(),
    val color: NoteColor = NoteColor.DEFAULT,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val trashedAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    /** When to remind about the note, in epoch millis; null when no reminder is set. */
    val reminderAt: Long? = null,
    val reminderRepeat: ReminderRepeat = ReminderRepeat.NONE,
    /** Names of the labels the note carries, in the order they were added. */
    val labels: List<String> = emptyList(),
) {
    val isTrashed: Boolean get() = trashedAt != null

    /** Nothing written yet; such a note is not kept. */
    val isBlank: Boolean
        get() = title.isBlank() && body.isBlank() && items.all { it.text.isBlank() }

    /** Same content, ignoring timestamps; used to skip saves that change nothing. */
    fun sameContentAs(other: Note): Boolean =
        copy(createdAt = 0L, updatedAt = 0L) == other.copy(createdAt = 0L, updatedAt = 0L)
}

/** How a reminder comes back after it fires. */
@Serializable
enum class ReminderRepeat { NONE, DAILY, WEEKLY, MONTHLY }

/** Size of the note text in the editor (and on the cards if asked), as a factor of the body style. */
@Serializable
enum class TextSize(val scale: Float) { SMALL(0.875f), NORMAL(1f), LARGE(1.25f), HUGE(1.5f) }

/** The three lists the bottom bar switches between. */
enum class Section { NOTES, ARCHIVE, TRASH }

/** Chips above the main list. */
enum class NoteFilter { ALL, TEXT, CHECKLIST }

/** Order of the notes and archive lists; pinned notes stay on top in every order. */
@Serializable
enum class NoteSort { EDITED, CREATED, TITLE }

/** Light or dark appearance; [SYSTEM] follows the device. */
@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** App preferences; [dynamicColor] takes the colors from the wallpaper on Android 12 and later. */
@Serializable
data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val listView: Boolean = false,
    val sort: NoteSort = NoteSort.EDITED,
    val recentSearches: List<String> = emptyList(),
    val lockEnabled: Boolean = false,
    val biometricUnlock: Boolean = false,
    val lockTimeout: LockTimeout = LockTimeout.IMMEDIATE,
    val hideContent: Boolean = false,
    /** Labels in the order the user made them; a label can exist before any note carries it. */
    val labels: List<String> = emptyList(),
    val textSize: TextSize = TextSize.NORMAL,
    val textSizeOnCards: Boolean = false,
)
