package com.vardiya.notlar.ui

import com.vardiya.notlar.domain.NoteEdits
import com.vardiya.notlar.domain.Settings

/** The recent searches kept in the settings: remembering one and forgetting one. */
class SearchHistory(private val updateSettings: ((Settings) -> Settings) -> Unit) {

    fun remember(query: String) = updateSettings { it.copy(recentSearches = NoteEdits.rememberSearch(it.recentSearches, query)) }

    fun forget(query: String) = updateSettings { it.copy(recentSearches = it.recentSearches - query) }
}
