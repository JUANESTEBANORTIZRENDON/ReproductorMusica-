package com.juan.reproductormusica.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juan.reproductormusica.R
import com.juan.reproductormusica.data.Song
import com.juan.reproductormusica.presentation.components.SearchResultsInfo
import com.juan.reproductormusica.presentation.components.SongItem
import com.juan.reproductormusica.presentation.viewmodel.MusicViewModel
import com.juan.reproductormusica.presentation.viewmodel.SortOption
import java.io.File

@Composable
fun SongListScreen(
    canciones: List<Song>, // Lista original (para estadísticas)
    musicViewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    // Estados reactivos desde MusicViewModel (patrón MVVM)
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val currentSong by musicViewModel.currentSong.collectAsState()
    
    // Estados de búsqueda y filtrado
    val searchQuery by musicViewModel.searchQuery.collectAsState()
    val sortOption by musicViewModel.sortOption.collectAsState()
    val filteredSongs by musicViewModel.filteredSongs.collectAsState()
    
    // Agrupar canciones filtradas por carpeta (solo para mostrar cuando no hay búsqueda activa)
    val shouldGroupByFolder = searchQuery.isEmpty() && sortOption == SortOption.DEFAULT
    val agrupadas = if (shouldGroupByFolder) {
        filteredSongs.groupBy {
            try {
                File(it.data).parentFile?.name ?: "Carpeta Desconocida"
            } catch (e: Exception) {
                "Carpeta Desconocida"
            }
        }.toSortedMap()
    } else {
        emptyMap()
    }

    // Configurar la playlist completa al entrar en la vista de canciones
    // sin cambiar la canción actual
    LaunchedEffect(canciones) {
        if (canciones.isNotEmpty()) {
            musicViewModel.updatePlaylistWithoutChangingSong(canciones)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A0000)) // Fondo rojo muy oscuro
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Información de resultados
        SearchResultsInfo(
            totalSongs = canciones.size,
            filteredSongs = filteredSongs.size,
            searchQuery = searchQuery,
            sortOption = sortOption,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp), 
            modifier = Modifier.weight(1f)
        ) {
            if (shouldGroupByFolder && agrupadas.isNotEmpty()) {
                // Mostrar agrupado por carpetas (comportamiento original)
                agrupadas.forEach { (carpeta, lista) ->
                    item {
                        Text(
                            text = carpeta.uppercase(),
                            style = TextStyle(
                                fontFamily = MusicFont,
                                color = Color(0xFFFF6B6B), // Rojo claro para contraste
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    items(lista) { song ->
                        SongItem(
                            song = song,
                            isCurrentSong = currentSong?.id == song.id,
                            isPlaying = isPlaying && currentSong?.id == song.id,
                            onClick = {
                                // Buscar el índice en la lista completa (no filtrada) para reproducción correcta
                                val index = canciones.indexOfFirst { it.id == song.id }
                                if (index != -1) musicViewModel.playSongAtIndex(index)
                            }
                        )
                    }
                }
            } else {
                // Mostrar lista filtrada/ordenada sin agrupación
                items(filteredSongs) { song ->
                    SongItem(
                        song = song,
                        isCurrentSong = currentSong?.id == song.id,
                        isPlaying = isPlaying && currentSong?.id == song.id,
                        onClick = {
                            // Buscar el índice en la lista completa (no filtrada) para reproducción correcta
                            val index = canciones.indexOfFirst { it.id == song.id }
                            if (index != -1) musicViewModel.playSongAtIndex(index)
                        }
                    )
                }
            }
            
            // Mensaje cuando no hay resultados
            if (filteredSongs.isEmpty() && canciones.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No se encontraron canciones",
                                style = TextStyle(
                                    fontFamily = MusicFont,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                            )
                            Text(
                                text = "Intenta con otros términos de búsqueda",
                                style = TextStyle(
                                    fontFamily = MusicFont,
                                    color = Color(0xFFBBBBBB),
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // El reproductor ahora es manejado por el MiniPlayer persistente en MusicNavigation
    }
}








