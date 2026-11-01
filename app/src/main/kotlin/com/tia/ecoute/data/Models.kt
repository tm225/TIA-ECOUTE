package com.tia.ecoute.data

import android.net.Uri

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: Uri,
    val dateAdded: Long,
    val folder: String
)

data class AlbumInfo(val id: Long, val name: String, val artist: String, val tracks: List<Track>)
data class ArtistInfo(val name: String, val tracks: List<Track>)
data class FolderInfo(val name: String, val tracks: List<Track>)
data class Playlist(val id: Long, val name: String, val trackIds: List<Long>)

enum class SortMode(val label: String) {
    TITLE("Titre (A-Z)"),
    ARTIST("Artiste (A-Z)"),
    RECENT("Ajouts récents"),
    DURATION("Durée")
}
