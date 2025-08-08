package com.juan.reproductormusica.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juan.reproductormusica.R
import com.juan.reproductormusica.data.Song
import com.juan.reproductormusica.data.database.PlaylistWithSongs
import com.juan.reproductormusica.presentation.components.SleepTimerDialog
import com.juan.reproductormusica.presentation.viewmodel.MusicViewModel
import com.juan.reproductormusica.utils.AlbumArtUtils
import java.io.File

val MusicFont = FontFamily(Font(R.font.montserrat_medium))

/**
 * Pantalla "Now Playing" que muestra los detalles de la canción actual
 * y controles de reproducción completos.
 * 
 * Ejemplo de implementación MVVM:
 * - Observa estados reactivos del ViewModel
 * - Delega todos los eventos al ViewModel
 * - UI completamente desacoplada de la lógica de reproducción
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    musicViewModel: MusicViewModel,
    onBackPressed: () -> Unit = {}
) {
    // Estados reactivos desde MusicViewModel (patrón MVVM)
    val currentSong by musicViewModel.currentSong.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val position by musicViewModel.position.collectAsState()
    val duration by musicViewModel.duration.collectAsState()
    val playbackState by musicViewModel.playbackState.collectAsState()
    val favoriteSongIds by musicViewModel.favoriteSongIds.collectAsState()
    val playlistsWithSongs by musicViewModel.playlistsWithSongs.collectAsState()
    
    // Estados del temporizador de suspensión
    val sleepTimerActive by musicViewModel.sleepTimerActive.collectAsState()
    val sleepTimerRemainingTime by musicViewModel.sleepTimerRemainingTime.collectAsState()
    
    // Estados para diálogos
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0000)) // Mismo color que el fondo de la lista
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header con botón de retroceso
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Atrás",
                    tint = Color.White
                )
            }
            
            Text(
                text = "Reproduciendo",
                style = TextStyle(
                    fontFamily = MusicFont,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            
            // Botón del temporizador de suspensión
            IconButton(
                onClick = { showSleepTimerDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Temporizador de suspensión",
                    tint = if (sleepTimerActive) Color(0xFFB71C1C) else Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Album Art con portada real
        currentSong?.let { song ->
            AlbumArtSection(
                filePath = song.data,
                modifier = Modifier.size(280.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Información de la canción
        currentSong?.let { song ->
            SongInfoSection(song = song)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Botones de playlist (favoritos y añadir a playlist)
        currentSong?.let { song ->
            PlaylistActionsSection(
                isFavorite = favoriteSongIds.contains(song.id),
                onToggleFavorite = { musicViewModel.toggleFavorite(song) }
            ) { showAddToPlaylistDialog = true }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Barra de progreso y tiempo
        ProgressSection(
            position = position,
            duration = duration,
            onSeekTo = { newPosition ->
                // Delegar evento al ViewModel (patrón MVVM)
                musicViewModel.seekTo(newPosition)
            }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Controles de reproducción
        PlaybackControlsSection(
            isPlaying = isPlaying,
            onTogglePlayPause = {
                // Delegar evento al ViewModel (patrón MVVM)
                musicViewModel.togglePlayPause()
            },
            onSkipToPrevious = {
                // Delegar evento al ViewModel (patrón MVVM)
                musicViewModel.skipToPrevious()
            },
            onSkipToNext = {
                // Delegar evento al ViewModel (patrón MVVM)
                musicViewModel.skipToNext()
            }
        )
        
        Spacer(modifier = Modifier.weight(1f))
    }
    
    // Diálogo para añadir a playlist
    if (showAddToPlaylistDialog) {
        currentSong?.let { song ->
            AddToPlaylistDialog(
                song = song,
                playlists = playlistsWithSongs,
                onDismiss = { showAddToPlaylistDialog = false },
                onAddToPlaylist = { playlistId ->
                    musicViewModel.addSongToPlaylist(song, playlistId)
                    showAddToPlaylistDialog = false
                },
                onCreateNewPlaylist = { name ->
                    musicViewModel.createPlaylistWithSong(name, song)
                    showAddToPlaylistDialog = false
                }
            )
        }
    }
    
    // Diálogo del temporizador de suspensión
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            isActive = sleepTimerActive,
            remainingTime = sleepTimerRemainingTime,
            onDismiss = { showSleepTimerDialog = false },
            onSetTimer = { hour, minute ->
                musicViewModel.setSleepTimer(hour, minute)
            },
            onSetTimerInMinutes = { minutes ->
                musicViewModel.setSleepTimerInMinutes(minutes)
            },
            onCancelTimer = {
                musicViewModel.cancelSleepTimer()
            },
            formatRemainingTime = { timeMs ->
                musicViewModel.formatSleepTimerRemaining(timeMs)
            }
        )
    }
}

/**
 * Sección que muestra la información de la canción actual
 */
@Composable
private fun SongInfoSection(song: Song) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = song.title,
            style = TextStyle(
                fontFamily = MusicFont,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = File(song.data).parentFile?.name ?: "Artista Desconocido",
            style = TextStyle(
                fontFamily = MusicFont,
                color = Color(0xFFCCCCCC),
                fontSize = 16.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Sección que muestra la barra de progreso y los tiempos
 */
@Composable
private fun ProgressSection(
    position: Long,
    duration: Long,
    onSeekTo: (Long) -> Unit
) {
    Column {
        // Barra de progreso
        val progress = if (duration > 0) {
            (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        
        Slider(
            value = progress,
            onValueChange = { newProgress ->
                val newPosition = (newProgress * duration).toLong()
                onSeekTo(newPosition)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color(0xFF8B1A1A)
            )
        )
        
        // Tiempos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(position),
                style = TextStyle(
                    fontFamily = MusicFont,
                    color = Color(0xFFCCCCCC),
                    fontSize = 14.sp
                )
            )
            
            Text(
                text = formatTime(duration),
                style = TextStyle(
                    fontFamily = MusicFont,
                    color = Color(0xFFCCCCCC),
                    fontSize = 14.sp
                )
            )
        }
    }
}

/**
 * Sección de controles de reproducción principales
 */
@Composable
private fun PlaybackControlsSection(
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipToPrevious: () -> Unit,
    onSkipToNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón anterior
        IconButton(
            onClick = onSkipToPrevious,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_skip_previous),
                contentDescription = "Anterior",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Botón play/pause principal
        IconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(
                painter = painterResource(
                    id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                ),
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = Color(0xFF731912),
                modifier = Modifier.size(40.dp)
            )
        }
        
        // Botón siguiente
        IconButton(
            onClick = onSkipToNext,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_skip_next),
                contentDescription = "Siguiente",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Función utilitaria para formatear tiempo
 */
private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

/**
 * Sección de Album Art con soporte para portadas reales
 */
@Composable
private fun AlbumArtSection(
    filePath: String,
    modifier: Modifier = Modifier
) {
    val albumArt by AlbumArtUtils.rememberAlbumArt(filePath)
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
        contentAlignment = Alignment.Center
    ) {
        val artBitmap = albumArt
        if (artBitmap != null) {
            Image(
                bitmap = artBitmap,
                contentDescription = "Portada del álbum",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder cuando no hay portada
            Icon(
                painter = painterResource(id = R.drawable.ic_music_note),
                contentDescription = "Sin portada",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(120.dp)
            )
        }
    }
}

@Composable
private fun PlaylistActionsSection(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Botón de favoritos
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (isFavorite) Color(0xFFB71C1C) else Color(0xFF2A0A0A),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Botón de agregar a playlist
        IconButton(
            onClick = onAddToPlaylist,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = Color(0xFF2A0A0A),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Agregar a playlist",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun AddToPlaylistDialog(
    song: Song?,
    playlists: List<PlaylistWithSongs>,
    onDismiss: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onCreateNewPlaylist: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Agregar a playlist",
                color = Color.White
            )
        },
        text = {
            LazyColumn {
                // Opción para crear nueva playlist
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { showCreateDialog = true },
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2A0A0A)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Crear nueva playlist",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                // Lista de playlists existentes (excluyendo favoritos)
                items(playlists.filter { it.playlist.playlistId != 1L }) { playlistWithSongs ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { 
                                onAddToPlaylist(playlistWithSongs.playlist.playlistId)
                                onDismiss()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1A0000)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistPlay,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = playlistWithSongs.playlist.name,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${playlistWithSongs.songs.size} canciones",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.White)
            }
        },
        containerColor = Color(0xFF0D0D0D)
    )
    
    // Diálogo para crear nueva playlist
    if (showCreateDialog) {
        var playlistName by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    text = "Nueva Playlist",
                    color = Color.White
                )
            },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Nombre de la playlist", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFB71C1C),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            onCreateNewPlaylist(playlistName.trim())
                            showCreateDialog = false
                            onDismiss()
                        }
                    }
                ) {
                    Text("Crear", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar", color = Color.White)
                }
            },
            containerColor = Color(0xFF0D0D0D)
        )
    }
}
