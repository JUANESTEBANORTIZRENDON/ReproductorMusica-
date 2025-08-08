package com.juan.reproductormusica.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa una playlist en la base de datos
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val playlistId: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
