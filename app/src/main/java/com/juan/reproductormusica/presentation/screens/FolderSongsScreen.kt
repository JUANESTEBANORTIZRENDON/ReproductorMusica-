package com.juan.reproductormusica.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.juan.reproductormusica.R
import com.juan.reproductormusica.presentation.components.SearchAndFilterBar
import com.juan.reproductormusica.presentation.components.SongItem
import com.juan.reproductormusica.presentation.viewmodel.MusicViewModel

/**
 * Pantalla que muestra las canciones de una carpeta específica
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderSongsScreen(
    folderName: String,
    musicViewModel: MusicViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    // Estado de búsqueda local para esta pantalla
    var localSearchQuery by remember { mutableStateOf("") }
    
    // Obtener todas las canciones de la carpeta
    val allSongsInFolder by remember(folderName) {
        derivedStateOf { musicViewModel.getSongsByFolder(folderName) }
    }
    
    // Filtrar canciones según búsqueda local
    val filteredSongs by remember(allSongsInFolder, localSearchQuery) {
        derivedStateOf {
            if (localSearchQuery.isBlank()) {
                allSongsInFolder
            } else {
                val searchTerm = localSearchQuery.lowercase().trim()
                allSongsInFolder.filter { song ->
                    song.title.lowercase().contains(searchTerm) ||
                    song.artist.lowercase().contains(searchTerm)
                }
            }
        }
    }

    // Estados del reproductor
    val currentSong by musicViewModel.currentSong.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()

    // Configurar la playlist al entrar en la carpeta
    LaunchedEffect(allSongsInFolder) {
        if (allSongsInFolder.isNotEmpty()) {
            musicViewModel.setPlaylist(allSongsInFolder)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar con botón de retroceso
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = folderName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val totalSongs = allSongsInFolder.size
                    val displayedSongs = filteredSongs.size
                    val countText = if (localSearchQuery.isBlank()) {
                        "$totalSongs ${if (totalSongs == 1) "canción" else "canciones"}"
                    } else {
                        "Mostrando $displayedSongs de $totalSongs canciones"
                    }
                    
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // Barra de búsqueda
        SearchAndFilterBar(
            searchQuery = localSearchQuery,
            onSearchQueryChange = { localSearchQuery = it },
            showSortControls = false, // No mostrar controles de ordenamiento en esta pantalla
            onClearSearch = { localSearchQuery = "" },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Contenido principal
        if (filteredSongs.isEmpty()) {
            // Estado vacío
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.ic_music_note),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (localSearchQuery.isBlank()) {
                            "No se encontraron canciones en esta carpeta"
                        } else if (allSongsInFolder.isEmpty()) {
                            "No se encontraron canciones en esta carpeta"
                        } else {
                            "No se encontraron canciones que coincidan con \"$localSearchQuery\""
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    
                    if (localSearchQuery.isNotBlank() && allSongsInFolder.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Intenta con otros términos de búsqueda",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Lista de canciones
            val listState = rememberLazyListState()

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredSongs) { song ->
                    // SongItem está en este mismo paquete (presentation.screens)
                    SongItem(
                        song = song,
                        isCurrentSong = currentSong?.id == song.id,
                        isPlaying = isPlaying && currentSong?.id == song.id,
                        onClick = {
                            // Buscar el índice en la lista completa (no filtrada) para reproducción correcta
                            val index = allSongsInFolder.indexOfFirst { it.id == song.id }
                            if (index != -1) musicViewModel.playSongAtIndex(index)
                        }
                    )
                }
            }
        }
    }
}
