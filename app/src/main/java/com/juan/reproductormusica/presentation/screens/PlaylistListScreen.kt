package com.juan.reproductormusica.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.juan.reproductormusica.data.database.PlaylistWithSongs
import com.juan.reproductormusica.presentation.navigation.MusicDestinations
import com.juan.reproductormusica.presentation.viewmodel.MusicViewModel

/**
 * Pantalla que muestra la lista de playlists en cuadrícula
 * Similar a FolderListScreen pero con diseño específico para playlists
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistListScreen(
    musicViewModel: MusicViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val playlistsWithSongs by musicViewModel.playlistsWithSongs.collectAsState()
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A0000))
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Mis Playlists",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (playlistsWithSongs.isEmpty()) {
            // Estado vacío
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay playlists",
                        color = Color(0xFF999999),
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Crea tu primera playlist",
                        color = Color(0xFF666666),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // Cuadrícula de playlists
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp) // Espacio para mini-player
            ) {
                // Botón "Nueva Playlist" siempre primero
                item {
                    NewPlaylistCard(
                        onClick = { showCreatePlaylistDialog = true }
                    )
                }
                
                // Playlists existentes
                items(playlistsWithSongs) { playlistWithSongs ->
                    PlaylistCard(
                        playlistWithSongs = playlistWithSongs,
                        onClick = {
                            navController.navigate(
                                MusicDestinations.createPlaylistSongsRoute(playlistWithSongs.playlist.playlistId)
                            )
                        }
                    )
                }
            }
        }
    }
    
    // Diálogo para crear nueva playlist
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                musicViewModel.createPlaylist(name)
                showCreatePlaylistDialog = false
            }
        )
    }
}

/**
 * Tarjeta para crear nueva playlist
 */
@Composable
private fun NewPlaylistCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A0A0A)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Nueva Playlist",
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Nueva\nPlaylist",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Tarjeta individual de playlist
 */
@Composable
private fun PlaylistCard(
    playlistWithSongs: PlaylistWithSongs,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playlist = playlistWithSongs.playlist
    val songCount = playlistWithSongs.songs.size
    
    // Icono específico para favoritos
    val icon: ImageVector = if (playlist.playlistId == 1L) {
        Icons.Default.Favorite
    } else {
        Icons.Default.PlaylistPlay
    }
    
    // Color específico para favoritos
    val cardColor = if (playlist.playlistId == 1L) {
        Color(0xFF4A0A0A) // Rojo más intenso para favoritos
    } else {
        Color(0xFF2A0A0A) // Color normal
    }
    
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = playlist.name,
                tint = if (playlist.playlistId == 1L) Color(0xFFFF6B6B) else Color(0xFFBBBBBB),
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = playlist.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "$songCount ${if (songCount == 1) "canción" else "canciones"}",
                color = Color(0xFF999999),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Diálogo para crear nueva playlist
 */
@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
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
                label = { Text("Nombre de la playlist") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFF6B6B),
                    unfocusedBorderColor = Color(0xFF666666),
                    focusedLabelColor = Color(0xFFFF6B6B),
                    unfocusedLabelColor = Color(0xFF999999)
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (playlistName.isNotBlank()) {
                        onConfirm(playlistName.trim())
                    }
                },
                enabled = playlistName.isNotBlank()
            ) {
                Text(
                    text = "Crear",
                    color = if (playlistName.isNotBlank()) Color(0xFFFF6B6B) else Color(0xFF666666)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancelar",
                    color = Color(0xFF999999)
                )
            }
        },
        containerColor = Color(0xFF2A0A0A),
        textContentColor = Color.White
    )
}
