package com.juan.reproductormusica.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.juan.reproductormusica.presentation.components.SongItem
import com.juan.reproductormusica.presentation.components.SearchAndFilterBar
import com.juan.reproductormusica.presentation.viewmodel.MusicViewModel
import com.juan.reproductormusica.repository.PlaylistRepository

/**
 * Pantalla que muestra las canciones de una playlist específica
 * Similar a FolderSongsScreen pero con funcionalidades específicas de playlist
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSongsScreen(
    playlistId: Long,
    musicViewModel: MusicViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    // Estados
    val playlistWithSongs by musicViewModel.getPlaylistWithSongs(playlistId).collectAsState(initial = null)
    val currentSong by musicViewModel.currentSong.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    
    // Búsqueda local
    var localSearchQuery by remember { mutableStateOf("") }
    
    // Estado para mostrar/ocultar la barra de búsqueda
    var showSearchBar by remember { mutableStateOf(false) }
    
    // Estados para diálogos
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    
    playlistWithSongs?.let { playlist ->
        val allSongsInPlaylist = playlist.songs.map { entity ->
            with(PlaylistRepository(musicViewModel.playlistRepository.playlistDao)) { entity.toSong() }
        }
        
        // Filtrar canciones según búsqueda local
        val filteredSongs = remember(allSongsInPlaylist, localSearchQuery) {
            if (localSearchQuery.isBlank()) {
                allSongsInPlaylist
            } else {
                val searchTerm = localSearchQuery.lowercase().trim()
                allSongsInPlaylist.filter { song ->
                    song.title.lowercase().contains(searchTerm) ||
                    song.artist.lowercase().contains(searchTerm)
                }
            }
        }
        
        // Configurar playlist al entrar
        LaunchedEffect(allSongsInPlaylist) {
            if (allSongsInPlaylist.isNotEmpty()) {
                musicViewModel.updatePlaylistWithoutChangingSong(allSongsInPlaylist)
            }
        }
        
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0D0D0D))
        ) {
            // TopAppBar
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = playlist.playlist.name,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (localSearchQuery.isBlank()) {
                            Text(
                                text = "${allSongsInPlaylist.size} ${if (allSongsInPlaylist.size == 1) "canción" else "canciones"}",
                                color = Color(0xFFBBBBBB),
                                fontSize = 12.sp
                            )
                        } else {
                            Text(
                                text = "Mostrando ${filteredSongs.size} de ${allSongsInPlaylist.size} canciones",
                                color = Color(0xFFBBBBBB),
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Botón de búsqueda
                    IconButton(
                        onClick = { 
                            showSearchBar = !showSearchBar
                            if (!showSearchBar) {
                                localSearchQuery = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Búsqueda",
                            tint = if (showSearchBar || localSearchQuery.isNotEmpty()) Color(0xFFB71C1C) else Color.White
                        )
                    }
                    
                    // Solo mostrar opciones si no es la playlist de favoritos
                    if (playlist.playlist.playlistId != 1L) {
                        Box {
                            IconButton(onClick = { showOptionsMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Más opciones",
                                    tint = Color.White
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Renombrar") },
                                    onClick = {
                                        showOptionsMenu = false
                                        showRenameDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Eliminar") },
                                    onClick = {
                                        showOptionsMenu = false
                                        showDeleteDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2A0A0A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
            
            // Barra de búsqueda delgada (aparece solo cuando se presiona el botón de búsqueda)
            if (showSearchBar) {
                OutlinedTextField(
                    value = localSearchQuery,
                    onValueChange = { localSearchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { 
                        Text(
                            "Buscar por título, artista...",
                            color = Color.Gray
                        ) 
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFB71C1C),
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color(0xFFB71C1C)
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
            
            // Lista de canciones
            if (filteredSongs.isEmpty()) {
                // Estado vacío
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (localSearchQuery.isBlank()) {
                                "Esta playlist está vacía"
                            } else {
                                "No se encontraron canciones"
                            },
                            color = Color(0xFF999999),
                            fontSize = 16.sp
                        )
                        if (localSearchQuery.isNotBlank()) {
                            Text(
                                text = "Intenta con otros términos de búsqueda",
                                color = Color(0xFF666666),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 80.dp // Espacio para mini-player
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredSongs) { song ->
                        SongItem(
                            song = song,
                            isCurrentSong = currentSong?.id == song.id,
                            isPlaying = isPlaying && currentSong?.id == song.id,
                            onClick = {
                                // Buscar el índice en la lista completa (no filtrada) para reproducción correcta
                                val index = allSongsInPlaylist.indexOfFirst { it.id == song.id }
                                if (index != -1) musicViewModel.playSongAtIndex(index)
                            },
                            onRemoveFromPlaylist = if (playlist.playlist.playlistId != 1L) {
                                { musicViewModel.removeSongFromPlaylist(song.id, playlistId) }
                            } else null // No permitir eliminar de favoritos desde aquí
                        )
                    }
                }
            }
        }
        
        // Diálogos
        if (showRenameDialog) {
            RenamePlaylistDialog(
                currentName = playlist.playlist.name,
                onDismiss = { showRenameDialog = false },
                onConfirm = { newName ->
                    musicViewModel.renamePlaylist(playlistId, newName)
                    showRenameDialog = false
                }
            )
        }
        
        if (showDeleteDialog) {
            DeletePlaylistDialog(
                playlistName = playlist.playlist.name,
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    musicViewModel.deletePlaylist(playlistId)
                    navController.popBackStack()
                    showDeleteDialog = false
                }
            )
        }
    }
}

/**
 * Diálogo para renombrar playlist
 */
@Composable
private fun RenamePlaylistDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Renombrar Playlist",
                color = Color.White
            )
        },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
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
                    if (newName.isNotBlank() && newName != currentName) {
                        onConfirm(newName.trim())
                    }
                },
                enabled = newName.isNotBlank() && newName != currentName
            ) {
                Text(
                    text = "Renombrar",
                    color = if (newName.isNotBlank() && newName != currentName) Color(0xFFFF6B6B) else Color(0xFF666666)
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

/**
 * Diálogo para confirmar eliminación de playlist
 */
@Composable
private fun DeletePlaylistDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Eliminar Playlist",
                color = Color.White
            )
        },
        text = {
            Text(
                text = "¿Estás seguro de que quieres eliminar la playlist \"$playlistName\"? Esta acción no se puede deshacer.",
                color = Color.White
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Eliminar",
                    color = Color(0xFFFF4444)
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
