package com.rigel.mp3p.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreScanner(private val context: Context) {

    suspend fun scanAudioFiles(): List<Song> = withContext(Dispatchers.IO) {
        val songsList = mutableListOf<Song>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val dataColumn = try { cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA) } catch (_: Exception) { -1 }

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown Title"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown Album"
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val track = cursor.getInt(trackColumn)
                    val dataPath = if (dataColumn != -1) cursor.getString(dataColumn) else null

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))

                    val albumArtUri: Uri? = try {
                        val retriever = android.media.MediaMetadataRetriever()
                        retriever.setDataSource(context, contentUri)
                        val artBytes = retriever.embeddedPicture
                        retriever.release()

                        if (artBytes != null && artBytes.isNotEmpty()) {
                            val cacheFile = java.io.File(context.cacheDir, "mp3_art_$id.jpg")
                            if (!cacheFile.exists()) {
                                cacheFile.writeBytes(artBytes)
                            }
                            Uri.fromFile(cacheFile)
                        } else {
                            val storeUri = ContentUris.withAppendedId(
                                Uri.parse("content://media/external/audio/albumart"),
                                albumId
                            )
                            val hasStoreArt = context.contentResolver.openInputStream(storeUri)?.use { true } ?: false
                            if (hasStoreArt) {
                                storeUri
                            } else {
                                null
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }

                    val folderName = if (!dataPath.isNullOrBlank()) {
                        java.io.File(dataPath).parentFile?.name
                    } else {
                        null
                    }

                    val cleanArtist = if (!folderName.isNullOrBlank() && folderName != "<unknown>") {
                        folderName
                    } else if (artist != "<unknown>" && artist.isNotBlank()) {
                        artist
                    } else {
                        album
                    }
                    val cleanAlbum = if (album == "<unknown>") "Unknown Album" else album

                    songsList.add(
                        Song(
                            id = id,
                            title = title,
                            artist = cleanArtist,
                            album = cleanAlbum,
                            duration = duration,
                            contentUri = contentUri,
                            albumArtUri = albumArtUri,
                            size = size,
                            trackNumber = track,
                            isDemo = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (songsList.isEmpty()) {
            // Return high quality demo songs if no local songs found
            DemoSongProvider.demoSongs
        } else {
            songsList
        }
    }
}
