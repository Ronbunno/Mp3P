package com.rigel.mp3p.data

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long, // milliseconds
    val contentUri: Uri,
    val albumArtUri: Uri? = null,
    val size: Long = 0L,
    val trackNumber: Int = 0,
    val isFavorite: Boolean = false,
    val isDemo: Boolean = false
)

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val songCount: Int,
    val albumArtUri: Uri? = null,
    val songs: List<Song> = emptyList()
)

data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val songs: List<Song> = emptyList()
)

data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
