package com.juan.reproductormusica.data

/**
 * Representa una canción que se encuentra en el dispositivo.
 * Se obtiene usando MediaStore.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val data: String,
    val duration: Long
)
