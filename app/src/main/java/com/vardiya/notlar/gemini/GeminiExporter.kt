package com.vardiya.notlar.gemini

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Clipboard, share sheet and file output for an exported chat. */
object GeminiExporter {

    private const val FOLDER = "exports"
    const val MARKDOWN_NAME = "gemini-sohbet.md"
    const val JSON_NAME = "gemini-sohbet.json"

    fun copy(context: Context, text: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("Gemini sohbeti", text))
    }

    /**
     * Writes [text] to the app's cache and returns a share intent for it. Only `cache/exports/` is
     * shared through the FileProvider, and the receiver gets read access to that one file.
     */
    fun shareIntent(context: Context, text: String, fileName: String, mimeType: String): Intent {
        val folder = File(context.cacheDir, FOLDER).apply { mkdirs() }
        folder.listFiles()?.forEach { it.delete() }
        val file = File(folder, fileName).apply { writeText(text) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(fileName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Markdown dosyasını paylaş")
    }

    /** Removes the shared copy of the chat from the cache. */
    fun clear(context: Context) {
        File(context.cacheDir, FOLDER).listFiles()?.forEach { it.delete() }
    }

    /** Writes [text] to a document the user picked with the system file picker. */
    fun write(context: Context, uri: Uri, text: String): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } != null
    }.getOrDefault(false)
}
