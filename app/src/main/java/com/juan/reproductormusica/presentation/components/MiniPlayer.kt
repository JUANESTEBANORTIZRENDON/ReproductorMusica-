package com.juan.reproductormusica.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juan.reproductormusica.R
import com.juan.reproductormusica.data.Song
import com.juan.reproductormusica.presentation.viewmodel.MusicViewModel
import com.juan.reproductormusica.utils.AlbumArtUtils
import java.io.File

private val MusicFont = FontFamily(Font(R.font.montserrat_medium))

/**
 * Mini-reproductor persistente que aparece en la parte inferior de la pantalla
 * cuando hay una canción reproduciéndose o pausada.
 * 
 * Características:
 * - Animación de entrada/salida suave
 * - Muestra portada, título, artista y controles básicos
 * - Al tocar navega a la pantalla completa "Now Playing"
 * - Diseño compacto y elegante
 */
@Composable
fun MiniPlayer(
    musicViewModel: MusicViewModel,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by musicViewModel.currentSong.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val position by musicViewModel.position.collectAsState()
    val duration by musicViewModel.duration.collectAsState()
    
    // Solo mostrar el mini-player si hay una canción actual
    AnimatedVisibility(
        visible = currentSong != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        currentSong?.let { song ->
            MiniPlayerContent(
                song = song,
                isPlaying = isPlaying,
                position = position,
                duration = duration,
                onPlayPause = { musicViewModel.togglePlayPause() },
                onNext = { musicViewModel.skipToNext() },
                onPrevious = { musicViewModel.skipToPrevious() },
                onClick = onNavigateToNowPlaying
            )
        }
    }
}

@Composable
private fun MiniPlayerContent(
    song: Song,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Color(0xFF0D0D0D)) // Negro más saturado, forma rectangular
            .clickable { onClick() }
    ) {
        Column {
            // Barra de progreso
            if (duration > 0) {
                val progress = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
            
            // Contenido principal
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Portada del álbum
                AlbumArtSection(
                    filePath = song.data,
                    modifier = Modifier.size(56.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Información de la canción
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song.title,
                        fontFamily = MusicFont,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = try {
                            File(song.data).parentFile?.name ?: "Artista Desconocido"
                        } catch (e: Exception) {
                            "Artista Desconocido"
                        },
                        fontFamily = MusicFont,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Controles de reproducción
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_skip_previous),
                            contentDescription = "Anterior",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                            ),
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_skip_next),
                            contentDescription = "Siguiente",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumArtSection(
    filePath: String,
    modifier: Modifier = Modifier
) {
    val albumArt by AlbumArtUtils.rememberAlbumArt(filePath)
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val artBitmap = albumArt
        if (artBitmap != null) {
            Image(
                bitmap = artBitmap,
                contentDescription = "Portada del álbum",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_music_note),
                contentDescription = "Sin portada",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
