package com.juan.reproductormusica.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa una canción en la base de datos
 * Mapea los datos del Song del sistema para persistencia
 */
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val songId: Long,
    val title: String,
    val artist: String,
    val path: String,
    val duration: Long
)
