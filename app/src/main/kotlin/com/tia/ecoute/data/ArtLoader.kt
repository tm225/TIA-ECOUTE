package com.tia.ecoute.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

object ArtLoader {

    private val cache = object : LruCache<Long, Bitmap>(40 * 1024 * 1024) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.byteCount
    }
    private val misses: MutableSet<Long> = Collections.synchronizedSet(HashSet())

    private fun keyOf(t: Track): Long = if (t.albumId > 0) t.albumId else -t.id

    fun peek(t: Track): Bitmap? = cache.get(keyOf(t))

    suspend fun load(context: Context, t: Track, size: Int = 384): Bitmap? {
        val key = keyOf(t)
        cache.get(key)?.let { return it }
        if (misses.contains(key)) return null
        val bmp = withContext(Dispatchers.IO) { decodeUri(context, t.uri, size, t.albumId) }
        if (bmp != null) cache.put(key, bmp) else misses.add(key)
        return bmp
    }

    /** Décode la pochette d'un fichier audio (miniature système, pochette MediaStore ou image intégrée). */
    fun decodeUri(context: Context, uri: Uri, size: Int, albumId: Long = 0L): Bitmap? {
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                return context.contentResolver.loadThumbnail(uri, Size(size, size), null)
            } catch (_: Exception) {
            }
        } else if (albumId > 0) {
            try {
                val u = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
                context.contentResolver.openInputStream(u)?.use { s ->
                    val b = BitmapFactory.decodeStream(s)
                    if (b != null) return b
                }
            } catch (_: Exception) {
            }
        }
        return embedded(context, uri, size)
    }

    private fun embedded(context: Context, uri: Uri, size: Int): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(context, uri)
            val bytes = r.embeddedPicture
            if (bytes == null) null else decodeSampled(bytes, size)
        } catch (_: Exception) {
            null
        } finally {
            try {
                r.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun decodeSampled(bytes: ByteArray, size: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= size && bounds.outHeight / (sample * 2) >= size) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    /** Couleur d'accent vive tirée de la pochette (pour l'ambiance du lecteur). */
    fun accent(bmp: Bitmap, fallback: Color): Color {
        return try {
            val one = Bitmap.createScaledBitmap(bmp, 1, 1, true)
            val px = one.getPixel(0, 0)
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(px, hsv)
            hsv[1] = maxOf(hsv[1], 0.55f)
            hsv[2] = hsv[2].coerceIn(0.55f, 0.95f)
            Color(android.graphics.Color.HSVToColor(hsv))
        } catch (_: Throwable) {
            fallback
        }
    }
}
