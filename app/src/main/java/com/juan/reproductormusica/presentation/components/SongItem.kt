package com.juan.reproductormusica.presentation.components

import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juan.reproductormusica.R
import com.juan.reproductormusica.data.Song
import com.juan.reproductormusica.utils.AlbumArtUtils
import java.io.File

private val MusicFont = FontFamily(Font(R.font.montserrat_medium))

/**
 * Componente reutilizable para mostrar una canción en la lista
 */
@Composable
fun SongItem(
    song: Song,
    isCurrentSong: Boolean = false,
    isPlaying: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentSong) Color(0xFF4D1A1A) else Color(0xFF2A0A0A)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            SongAlbumArt(
                filePath = song.data,
                isCurrentSong = isCurrentSong,
                isPlaying = isPlaying,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = TextStyle(
                        fontFamily = MusicFont,
                        color = if (isCurrentSong) Color(0xFFFFFFFF) else Color.White,
                        fontSize = 16.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = song.artist,
                    style = TextStyle(
                        fontFamily = MusicFont,
                        color = if (isCurrentSong) Color(0xFFCCCCCC) else Color(0xFFBBBBBB),
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = try {
                        File(song.data).parentFile?.name ?: "Carpeta Desconocida"
                    } catch (e: Exception) {
                        "Carpeta Desconocida"
                    },
                    style = TextStyle(
                        fontFamily = MusicFont,
                        color = if (isCurrentSong) Color(0xFFCCCCCC) else Color(0xFFBBBBBB),
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (isCurrentSong) {
                Text(
                    text = if (isPlaying) "⏸️" else "▶️",
                    fontSize = 16.sp
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "${File(song.data).parentFile?.name ?: "-"}",
                color = Color(0xFFCCCCCC),
                style = TextStyle(fontFamily = MusicFont, fontSize = 12.sp)
            )
            Text(
                text = formatDuration(song.duration),
                color = Color(0xFFCCCCCC),
                style = TextStyle(fontFamily = MusicFont, fontSize = 12.sp)
            )
        }
    }
}

/**
 * Componente de Album Art para elementos de la lista de canciones
 */
@Composable
private fun SongAlbumArt(
    filePath: String,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val albumArt by AlbumArtUtils.rememberAlbumArt(filePath)
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isCurrentSong) 
                    Color(0xFF6B1F1F).copy(alpha = 0.6f) // Rojo más claro para canción actual
                else 
                    Color(0xFF3D0A0A).copy(alpha = 0.8f) // Rojo oscuro para otras canciones
            ),
        contentAlignment = Alignment.Center
    ) {
        val artBitmap = albumArt
        if (artBitmap != null) {
            Image(
                bitmap = artBitmap,
                contentDescription = "Portada",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_music_note),
                contentDescription = "Sin portada",
                tint = if (isCurrentSong) Color.White else Color(0xFFBBBBBB),
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Indicador de reproducción
        if (isCurrentSong && isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play),
                    contentDescription = "Reproduciendo",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Formatea la duración en milisegundos a formato legible
 */
fun formatDuration(durationMs: Long): String {
    return DateUtils.formatElapsedTime(durationMs / 1000)
}
