package com.juan.reproductormusica.repository

import android.content.Context
import android.provider.MediaStore
import com.juan.reproductormusica.data.Song
import java.io.File

object MusicRepository {
    fun getAllSongs(context: Context): List<Song> {
        val songList = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION
        )

        val cursor = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (it.moveToNext()) {
                val path = it.getString(dataCol)
                if (path.endsWith(".mp3", ignoreCase = true)) {
                    val folder = File(path).parentFile?.name ?: "Desconocida"
                    songList.add(
                        Song(
                            id = it.getLong(idCol),
                            title = it.getString(titleCol),
                            artist = folder,
                            data = path,
                            duration = it.getLong(durationCol)
                        )
                    )
                }
            }
        }

        return songList.sortedBy { it.artist.lowercase() }
    }
}

