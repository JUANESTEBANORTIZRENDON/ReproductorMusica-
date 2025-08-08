package com.juan.reproductormusica.data.database

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Relación que representa una playlist con sus canciones
 */
data class PlaylistWithSongs(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "songId",
        associateBy = Junction(PlaylistSongCrossRef::class)
    )
    val songs: List<SongEntity>
)
