package com.juan.reproductormusica.presentation.screens

import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.juan.reproductormusica.R
import com.juan.reproductormusica.data.Song
import com.juan.reproductormusica.presentation.components.SearchAndFilterBar
import com.juan.reproductormusica.presentation.components.SearchResultsInfo
import com.juan.reproductormusica.presentation.viewmodel.MusicViewModel
import com.juan.reproductormusica.presentation.viewmodel.SortOption
import com.juan.reproductormusica.utils.AlbumArtUtils
import kotlinx.coroutines.delay
import java.io.File

private val MusicFont = FontFamily(Font(R.font.montserrat_medium))

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A0000)) // Fondo rojo muy oscuro
            .padding(16.dp)
    ) {
        // Barra de búsqueda y filtrado
        SearchAndFilterBar(
            searchQuery = searchQuery,
            sortOption = sortOption,
            onSearchQueryChange = { musicViewModel.updateSearchQuery(it) },
            onSortOptionChange = { musicViewModel.updateSortOption(it) },
            onClearSearch = { musicViewModel.clearSearch() },
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Información de resultados
        SearchResultsInfo(
            totalSongs = canciones.size,
            filteredSongs = filteredSongs.size,
            searchQuery = searchQuery,
            sortOption = sortOption,
            modifier = Modifier.padding(bottom = 12.dp)
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
                                // Delegar evento al ViewModel (patrón MVVM)
                                musicViewModel.playSong(song)
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
                            // Delegar evento al ViewModel (patrón MVVM)
                            musicViewModel.playSong(song)
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

@Composable
fun SongItem(
    song: Song,
    isCurrentSong: Boolean = false,
    isPlaying: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor = if (isCurrentSong) Color(0xFF4A0E0E) else Color(0xFF2D0808)
    val borderColor = if (isCurrentSong) Color(0xFFFF6B6B) else Color(0xFF1A0404)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor)
            .padding(1.dp)
            .background(backgroundColor)
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art miniatura
            SongAlbumArt(
                filePath = song.data,
                isCurrentSong = isCurrentSong,
                isPlaying = isPlaying,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Información de la canción
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
            modifier = Modifier.fillMaxWidth(),
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



fun formatDuration(durationMs: Long): String {
    return DateUtils.formatElapsedTime(durationMs / 1000)
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






