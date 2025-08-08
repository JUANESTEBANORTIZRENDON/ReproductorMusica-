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
 * Barra compacta de búsqueda y (opcional) filtrado/orden.
 *
 * - Si solo quieres búsqueda (como en FolderSongsScreen), NO pases nada de sort
 *   y opcionalmente pon showSortControls = false.
 * - Si quieres también orden (como en SongListScreen), pasa sortOption,
 *   onSortOptionChange y deja showSortControls = true (default).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,

    // --- Controles de orden opcionales ---
    showSortControls: Boolean = true,
    sortOption: SortOption? = SortOption.DEFAULT,
    onSortOptionChange: (SortOption) -> Unit = {},

    // Limpiar búsqueda (opcional). Si no lo pasas, no hace nada.
    onClearSearch: () -> Unit = {},
) {
    var showSearchField by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Barra compacta con botones
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón de búsqueda (LADO IZQUIERDO)
            Box {
                IconButton(
                    onClick = {
                        showSearchField = !showSearchField
                        if (!showSearchField) onClearSearch()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = if (showSearchField) "Cerrar búsqueda" else "Abrir búsqueda",
                        tint = if (showSearchField || searchQuery.isNotEmpty()) Color(0xFFE53935) else Color(0xFFBBBBBB)
                    )
                }
                
                // Campo de búsqueda como overlay/popup (ancho reducido para no tocar el filtro)
                this@Row.AnimatedVisibility(
                    visible = showSearchField,
                    enter = slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300)),
                    exit = slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                ) {
                    Card(
                        modifier = Modifier
                            .width(180.dp) // Ancho más reducido para evitar superposición
                            .offset(x = 48.dp), // Posicionar al frente del icono
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1010)),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            placeholder = {
                                Text(
                                    text = "Buscar por título, artista...",
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
                            textStyle = TextStyle(fontFamily = MusicFont, fontSize = 16.sp),
                            singleLine = true
                        )
                    }
                }
            }

            // Info de orden actual (CENTRO)
            val canShowOrderInfo = showSortControls && !showSearchField && searchQuery.isEmpty()
            if (canShowOrderInfo) {
                Text(
                    text = "Ordenar: ${sortOption?.displayName}",
                    style = TextStyle(
                        fontFamily = MusicFont,
                        color = Color(0xFFCCCCCC),
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Botón de filtrado/orden (ESQUINA DERECHA)
            if (showSortControls) {
                Box {
                    IconButton(onClick = { showSortMenu = !showSortMenu }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_filter_list),
                            contentDescription = if (showSortMenu) "Cerrar filtros" else "Abrir filtros",
                            tint = Color(0xFFE53935)
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
                                            color = Color.White,
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
    }
}

/**
 * Muestra estadísticas de búsqueda/filtrado
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
    val text = if (isFiltered) {
        "Mostrando $filteredSongs de $totalSongs canciones"
    } else {
        "Total: $totalSongs canciones"
    }
    Text(
        text = text,
        style = TextStyle(fontFamily = MusicFont, color = Color.White, fontSize = 16.sp),
        modifier = modifier.fillMaxWidth()
    )
}
