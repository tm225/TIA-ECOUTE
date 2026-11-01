package com.tia.ecoute.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import java.io.File

object MediaScanner {

    fun scan(context: Context, minDurationMs: Long): List<Track> {
        val out = ArrayList<Track>()
        val collection = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.DURATION} >= ? AND " +
            "${MediaStore.Audio.Media.IS_RINGTONE} = 0 AND " +
            "${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND " +
            "${MediaStore.Audio.Media.IS_ALARM} = 0"
        val args = arrayOf(minDurationMs.toString())
        try {
            context.contentResolver.query(
                collection, projection, selection, args,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            )?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val iAdded = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val iData = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    val path = c.getString(iData)
                    val fileName = path?.let { File(it).nameWithoutExtension }
                    val rawTitle = c.getString(iTitle)
                    val title = if (rawTitle.isNullOrBlank()) (fileName ?: "Sans titre") else rawTitle
                    val rawArtist = c.getString(iArtist)
                    val artist = if (rawArtist.isNullOrBlank() || rawArtist == "<unknown>") "Artiste inconnu" else rawArtist
                    val rawAlbum = c.getString(iAlbum)
                    val album = if (rawAlbum.isNullOrBlank() || rawAlbum == "<unknown>") "Album inconnu" else rawAlbum
                    val folder = path?.let { File(it).parentFile?.name } ?: "Inconnu"
                    out.add(
                        Track(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = c.getLong(iAlbumId),
                            durationMs = c.getLong(iDur),
                            uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                            dateAdded = c.getLong(iAdded),
                            folder = folder
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        return out
    }
}
