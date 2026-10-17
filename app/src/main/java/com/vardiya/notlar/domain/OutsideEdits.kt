package com.vardiya.notlar.domain

/** How the open editor's draft and the stored note meet when the note changes elsewhere. */
object OutsideEdits {

    /**
     * Brings changes made outside the editor into its [draft]: a to-do item checked on the widget,
     * a reminder that rang and was cleared or moved. [base] is the stored note the draft last saw,
     * [stored] the note as stored now; whatever the draft itself changed since [base] wins.
     */
    fun merge(draft: Note, base: Note, stored: Note): Note {
        val reminderUntouched = draft.reminderAt == base.reminderAt && draft.reminderRepeat == base.reminderRepeat
        val baseChecked = base.items.associate { it.id to it.checked }
        val storedChecked = stored.items.associate { it.id to it.checked }
        return draft.copy(
            reminderAt = if (reminderUntouched) stored.reminderAt else draft.reminderAt,
            reminderRepeat = if (reminderUntouched) stored.reminderRepeat else draft.reminderRepeat,
            items = draft.items.map { item ->
                val outside = storedChecked[item.id]
                if (outside != null && baseChecked[item.id] == item.checked) item.copy(checked = outside) else item
            },
        )
    }
}
