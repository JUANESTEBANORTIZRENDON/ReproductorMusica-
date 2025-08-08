package com.juan.reproductormusica.data.database

import androidx.room.Entity

/**
 * Tabla intermedia para la relación muchos-a-muchos entre playlists y canciones
 */
@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "songId"]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val addedAt: Long = System.currentTimeMillis()
)
