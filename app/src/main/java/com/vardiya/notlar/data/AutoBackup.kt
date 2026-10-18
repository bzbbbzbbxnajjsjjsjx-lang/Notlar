package com.vardiya.notlar.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Writes the daily JSON backup into the folder the user chose, and keeps the last few. */
object AutoBackup {

    private const val PREFIX = "notlar-yedek-"
    private const val KEEP = 7
    const val INTERVAL_MILLIS = 24L * 60 * 60 * 1000

    /** True when a backup is switched on, has a folder and the last one is more than a day old. */
    fun due(enabled: Boolean, folder: String?, lastBackupAt: Long, nowMillis: Long): Boolean =
        enabled && folder != null && nowMillis - lastBackupAt >= INTERVAL_MILLIS

    /** Today's file name, so a second backup the same day replaces the first. */
    fun fileName(today: LocalDate): String = "$PREFIX$today.json"

    /** Writes [json] into [folder]; false if the folder is gone or not writable. */
    suspend fun write(context: Context, folder: String, json: String, today: LocalDate): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val dir = DocumentFile.fromTreeUri(context, Uri.parse(folder))?.takeIf { it.canWrite() } ?: return@runCatching false
            val name = fileName(today)
            // The new file is written whole before the one of the same day goes, so a failed write loses nothing.
            val fresh = dir.createFile("application/json", name.removeSuffix(".json") + ".yeni") ?: return@runCatching false
            val written = context.contentResolver.openOutputStream(fresh.uri)?.bufferedWriter()?.use { it.write(json) } != null
            if (!written) {
                fresh.delete()
                return@runCatching false
            }
            dir.findFile(name)?.delete()
            fresh.renameTo(name)
            // Older backups beyond the last few go; other files in the folder are never touched.
            dir.listFiles().filter { it.name?.startsWith(PREFIX) == true }
                .sortedByDescending { it.name }.drop(KEEP).forEach { it.delete() }
            true
        }.getOrDefault(false)
    }
}
