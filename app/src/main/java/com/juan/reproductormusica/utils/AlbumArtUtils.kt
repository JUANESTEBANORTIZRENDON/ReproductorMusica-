package com.juan.reproductormusica.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Utilidades para manejo de portadas de álbum (Album Art)
 * Proporciona funciones para extraer y cachear artwork de archivos de música
 */
object AlbumArtUtils {
    
    private val artworkCache = mutableMapOf<String, ImageBitmap?>()
    
    /**
     * Extrae la portada del álbum de un archivo de música
     * @param filePath Ruta del archivo de música
     * @return ImageBitmap de la portada o null si no existe
     */
    suspend fun extractAlbumArt(filePath: String): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            // Verificar caché primero
            artworkCache[filePath]?.let { return@withContext it }
            
            val retriever = MediaMetadataRetriever()
            retriever.use {
                it.setDataSource(filePath)
                val art = it.embeddedPicture
                
                if (art != null) {
                    val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                    val imageBitmap = bitmap?.asImageBitmap()
                    
                    // Cachear resultado
                    artworkCache[filePath] = imageBitmap
                    return@withContext imageBitmap
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Cachear null para evitar intentos repetidos
        artworkCache[filePath] = null
        return@withContext null
    }
    
    /**
     * Obtiene la portada del álbum de forma asíncrona para Compose
     * @param filePath Ruta del archivo de música
     * @return State<ImageBitmap?> que se actualiza cuando la imagen está lista
     */
    @Composable
    fun rememberAlbumArt(filePath: String?): State<ImageBitmap?> {
        val albumArt = remember { mutableStateOf<ImageBitmap?>(null) }
        
        LaunchedEffect(filePath) {
            albumArt.value = null // Reset mientras carga
            if (filePath != null && File(filePath).exists()) {
                albumArt.value = extractAlbumArt(filePath)
            }
        }
        
        return albumArt
    }
    
    /**
     * Limpia la caché de artwork
     * Útil para liberar memoria cuando sea necesario
     */
    fun clearCache() {
        artworkCache.clear()
    }
    
    /**
     * Obtiene el tamaño actual de la caché
     */
    fun getCacheSize(): Int = artworkCache.size
    
    /**
     * Crea un bitmap redimensionado para optimizar memoria
     * @param original Bitmap original
     * @param maxSize Tamaño máximo en píxeles
     * @return Bitmap redimensionado
     */
    private fun resizeBitmap(original: Bitmap, maxSize: Int): Bitmap {
        val width = original.width
        val height = original.height
        
        if (width <= maxSize && height <= maxSize) {
            return original
        }
        
        val ratio = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
    }
}

/**
 * Extensión para obtener metadata adicional del archivo
 */
suspend fun String.getAudioMetadata(): AudioMetadata? = withContext(Dispatchers.IO) {
    try {
        val retriever = MediaMetadataRetriever()
        retriever.use {
            it.setDataSource(this@getAudioMetadata)
            
            return@withContext AudioMetadata(
                title = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Desconocido",
                artist = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Artista Desconocido",
                album = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Álbum Desconocido",
                duration = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Clase de datos para metadata de audio
 */
data class AudioMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long
)
