package com.vardiya.notlar.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import androidx.core.content.FileProvider
import java.io.File
import java.util.Base64
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pictures of notes, kept as JPEG files in the app's private storage; a note holds only the file
 * names. Pictures are shrunk to [MAX_SIDE] pixels when they are added.
 */
class ImageStore(context: Context) {

    private val appContext = context.applicationContext
    private val dir = File(appContext.filesDir, "images").apply { mkdirs() }
    private val cameraDir = File(appContext.cacheDir, "camera").apply { mkdirs() }

    /** Copies the picture at [source] into the store and returns its file name, or null if it cannot be read. */
    suspend fun add(source: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = appContext.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return@runCatching null
            // Photos are stored turned the way the phone was held; the JPEG written below drops that note.
            val turn = resolver.openInputStream(source)?.use { orientationDegrees(ExifInterface(it)) } ?: 0f
            val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(bitmap.width, bitmap.height))
            val fitted = if (scale < 1f || turn != 0f) {
                val matrix = Matrix().apply {
                    postScale(scale, scale)
                    postRotate(turn)
                }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
            val name = "${UUID.randomUUID()}.jpg"
            File(dir, name).outputStream().use { fitted.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            name
        }.getOrNull()
    }

    /** A decoded picture no wider than [maxSide] pixels; null if the file is gone. Small ones are cached. */
    suspend fun load(name: String, maxSide: Int): Bitmap? {
        cache.get("$name@$maxSide")?.let { return it }
        return withContext(Dispatchers.IO) {
            val file = File(dir, name).takeIf { it.exists() } ?: return@withContext null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
                ?.also { cache.put("$name@$maxSide", it) }
        }
    }

    /** An empty file in the cache for the camera to write to, and the content URI it is shared under. */
    fun newCameraTarget(): Uri {
        val file = File(cameraDir, "foto-${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(appContext, "${appContext.packageName}.files", file)
    }

    /** A picture as base64 for a backup file; null if the file is gone. */
    suspend fun encode(name: String): String? = withContext(Dispatchers.IO) {
        File(dir, name).takeIf { it.isFile }?.let { Base64.getEncoder().encodeToString(it.readBytes()) }
    }

    /** Writes a picture from a backup back into the store, under the name the notes use. */
    suspend fun restore(name: String, base64: String) = withContext(Dispatchers.IO) {
        // Names come from a file the user opened; only plain file names are written, never paths.
        if (name == File(name).name && name.endsWith(".jpg")) {
            runCatching { File(dir, name).writeBytes(Base64.getDecoder().decode(base64)) }
        }
    }

    /** Removes the camera's temporary files once the pictures are copied. */
    fun clearCamera() {
        cameraDir.listFiles()?.forEach { it.delete() }
    }

    /** Deletes the pictures that no note refers to any more (a removed picture, an emptied trash). */
    suspend fun deleteUnused(inUse: Set<String>) = withContext(Dispatchers.IO) {
        dir.listFiles()?.filter { it.name !in inUse }?.forEach { it.delete() }
    }

    private fun orientationDegrees(exif: ExifInterface): Float =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> QUARTER_TURN
            ExifInterface.ORIENTATION_ROTATE_180 -> HALF_TURN
            ExifInterface.ORIENTATION_ROTATE_270 -> THREE_QUARTER_TURN
            else -> 0f
        }

    private companion object {
        const val QUARTER_TURN = 90f
        const val HALF_TURN = 180f
        const val THREE_QUARTER_TURN = 270f
        const val MAX_SIDE = 1600
        const val JPEG_QUALITY = 85
        const val CACHE_BYTES = 24 * 1024 * 1024
        val cache = object : LruCache<String, Bitmap>(CACHE_BYTES) {
            override fun sizeOf(key: String, value: Bitmap) = value.byteCount
        }
    }
}
