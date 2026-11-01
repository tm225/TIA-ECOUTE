package com.tia.ecoute.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.tia.ecoute.data.Track

fun Track.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(uri)
                .build()
        )
        .build()
