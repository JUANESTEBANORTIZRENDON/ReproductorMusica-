package com.juan.reproductormusica.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juan.reproductormusica.R
import com.juan.reproductormusica.presentation.viewmodel.SortOption

private val MusicFont = FontFamily(Font(R.font.montserrat_medium))

/**
 * Barra compacta de búsqueda y filtrado con botones desplegables
 * 
 * Características:
 * - Botón de búsqueda (lupa) que despliega campo de búsqueda
 * - Botón de filtrado que despliega opciones lateralmente
 * - Diseño compacto que ahorra espacio
 * - Animaciones suaves de despliegue/cierre
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAndFilterBar(
    searchQuery: String,
    sortOption: SortOption,
    onSearchQueryChange: (String) -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSearchField by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    
    // Barra compacta con botones
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Información del filtro actual (solo cuando no hay búsqueda activa)
        if (!showSearchField && searchQuery.isEmpty()) {
            Text(
                text = "Ordenar: ${sortOption.displayName}",
                style = TextStyle(
                    fontFamily = MusicFont,
                    color = Color(0xFFCCCCCC),
                    fontSize = 14.sp
                ),
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        
        // Botones de acción
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón de búsqueda
            IconButton(
                onClick = { 
                    showSearchField = !showSearchField
                    if (!showSearchField) {
                        onClearSearch()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = if (showSearchField) "Cerrar búsqueda" else "Abrir búsqueda",
                    tint = if (showSearchField || searchQuery.isNotEmpty()) Color(0xFFE53935) else Color(0xFFBBBBBB)
                )
            }
            
            // Botón de filtrado
            Box {
                IconButton(
                    onClick = { showSortMenu = !showSortMenu }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_filter_list),
                        contentDescription = if (showSortMenu) "Cerrar filtros" else "Abrir filtros",
                        tint = if (showSortMenu || sortOption != SortOption.DEFAULT) Color(0xFFE53935) else Color(0xFFBBBBBB)
                    )
                }
                
                // Dropdown menu con opciones de ordenamiento
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(Color(0xFF2C1010))
                ) {
                    SortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.displayName,
                                    style = TextStyle(
                                        fontFamily = MusicFont,
                                        color = if (option == sortOption) Color(0xFFE53935) else Color.White,
                                        fontSize = 14.sp
                                    )
                                )
                            },
                            onClick = {
                                onSortOptionChange(option)
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (option == sortOption) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Seleccionado",
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Campo de búsqueda desplegable
    AnimatedVisibility(
        visible = showSearchField,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2C1010)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = {
                    Text(
                        text = "Buscar por título, artista o álbum...",
                        style = TextStyle(
                            fontFamily = MusicFont,
                            color = Color(0xFFBBBBBB),
                            fontSize = 14.sp
                        )
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar búsqueda",
                                tint = Color(0xFFBBBBBB)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFE53935),
                    unfocusedBorderColor = Color(0xFF616161),
                    cursorColor = Color(0xFFE53935)
                ),
                textStyle = TextStyle(
                    fontFamily = MusicFont,
                    fontSize = 16.sp
                ),
                singleLine = true
            )
        }
    }
}

/**
 * Componente compacto que muestra solo estadísticas de búsqueda/filtrado
 */
@Composable
fun SearchResultsInfo(
    totalSongs: Int,
    filteredSongs: Int,
    searchQuery: String,
    sortOption: SortOption,
    modifier: Modifier = Modifier
) {
    val isFiltered = searchQuery.isNotEmpty() || sortOption != SortOption.DEFAULT
    
    // Solo mostrar cuando hay filtros activos o búsqueda
    if (isFiltered) {
        Text(
            text = "Mostrando $filteredSongs de $totalSongs canciones",
            style = TextStyle(
                fontFamily = MusicFont,
                color = Color.White,
                fontSize = 16.sp
            ),
            modifier = modifier.fillMaxWidth()
        )
    } else {
        Text(
            text = "Total: $totalSongs canciones",
            style = TextStyle(
                fontFamily = MusicFont,
                color = Color.White,
                fontSize = 16.sp
            ),
            modifier = modifier.fillMaxWidth()
        )
    }
}
