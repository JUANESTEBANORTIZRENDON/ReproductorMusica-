package com.juan.reproductormusica.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.juan.reproductormusica.R
import com.juan.reproductormusica.data.Song
import com.juan.reproductormusica.presentation.viewmodel.MusicViewModel
import com.juan.reproductormusica.utils.AlbumArtUtils

/**
 * Pantalla que muestra la lista de carpetas con canciones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderListScreen(
    musicViewModel: MusicViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val allSongs by musicViewModel.allSongs.collectAsState()
    val searchQuery by musicViewModel.searchQuery.collectAsState()

    // Agrupar canciones por carpeta y aplicar filtro de búsqueda
    val folders by remember(allSongs, searchQuery) {
        derivedStateOf {
            val allFolders = musicViewModel.getFolders().toList()
            
            if (searchQuery.isBlank()) {
                // Sin búsqueda: mostrar todas las carpetas ordenadas
                allFolders.sortedBy { it.first.lowercase() }
            } else {
                // Con búsqueda: filtrar carpetas por nombre o contenido
                val searchTerm = searchQuery.lowercase().trim()
                allFolders.filter { (folderName, songs) ->
                    // Buscar en nombre de carpeta
                    folderName.lowercase().contains(searchTerm) ||
                    // Buscar en títulos de canciones de la carpeta
                    songs.any { song ->
                        song.title.lowercase().contains(searchTerm) ||
                        song.artist.lowercase().contains(searchTerm)
                    }
                }.sortedBy { it.first.lowercase() }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A0000)) // Fondo consistente con SongListScreen
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "Carpetas de Música" else "Resultados de Búsqueda",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                
                val totalFolders = musicViewModel.getFolders().size
                val resultText = if (searchQuery.isBlank()) {
                    "$totalFolders carpetas encontradas"
                } else {
                    "Mostrando ${folders.size} de $totalFolders carpetas"
                }
                
                Text(
                    text = resultText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                
                // Mostrar término de búsqueda si existe
                if (searchQuery.isNotBlank()) {
                    Text(
                        text = "Buscando: \"$searchQuery\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Lista de carpetas
        if (folders.isEmpty()) {
            // Estado vacío
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Usamos un drawable disponible (no dependemos de material-icons-extended)
                    Icon(
                        painter = painterResource(R.drawable.ic_music_note),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isBlank()) {
                            "No se encontraron carpetas"
                        } else {
                            "No se encontraron carpetas que coincidan con \"$searchQuery\""
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    
                    if (searchQuery.isNotBlank()) {
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), // 2 columnas en la cuadrícula
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(folders) { (folderName, songs) ->
                    FolderItem(
                        folderName = folderName,
                        songs = songs,
                        onClick = { navController.navigate("folder_songs/$folderName") }
                    )
                }
            }
        }
    }
}

/**
 * Ítem de carpeta con miniatura (album art de la primera canción si existe).
 */
@Composable
private fun FolderItem(
    folderName: String,
    songs: List<Song>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Hacer las tarjetas cuadradas
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A0A0A) // Fondo más oscuro para mejor contraste
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Miniatura: portada de la primera canción o icono por defecto (más grande)
            Box(
                modifier = Modifier
                    .size(80.dp) // Más grande para cuadrícula
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF4D1A1A)),
                contentAlignment = Alignment.Center
            ) {
                if (songs.isNotEmpty()) {
                    val firstPath = songs.first().data
                    val albumArt by AlbumArtUtils.rememberAlbumArt(firstPath)

                    val artBitmap = albumArt
                    if (artBitmap != null) {
                        Image(
                            bitmap = artBitmap,
                            contentDescription = "Portada de $folderName",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_music_note),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_music_note),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Información de la carpeta (centrada)
            Text(
                text = folderName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${songs.size} ${if (songs.size == 1) "canción" else "canciones"}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFBBBBBB),
                textAlign = TextAlign.Center
            )
        }
    }
}

