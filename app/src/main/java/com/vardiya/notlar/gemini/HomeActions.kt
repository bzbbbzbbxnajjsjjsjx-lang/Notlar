package com.vardiya.notlar.gemini

/** What the screen can ask its host to do; the host owns the clipboard, share sheet and file picker. */
data class HomeActions(
    val onLinkChange: (String) -> Unit,
    val onFetch: () -> Unit,
    val onCancel: () -> Unit,
    val onReset: () -> Unit,
    val onCopyMarkdown: () -> Unit,
    val onShareMarkdown: () -> Unit,
    val onSaveJson: () -> Unit,
)
