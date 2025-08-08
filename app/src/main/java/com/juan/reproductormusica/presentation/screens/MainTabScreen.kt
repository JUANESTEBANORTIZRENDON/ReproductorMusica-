package com.juan.reproductormusica.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.juan.reproductormusica.data.Song
import com.juan.reproductormusica.presentation.components.SearchAndFilterBar
import com.juan.reproductormusica.presentation.components.SleepTimerDialog
import com.juan.reproductormusica.presentation.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla principal con pestañas para Canciones, Carpetas y Playlists usando HorizontalPager
 * 
 * @param songs Lista completa de canciones
 * @param musicViewModel ViewModel con la lógica de reproducción
 * @param navController Controlador de navegación
 * @param modifier Modificador opcional
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabScreen(
    songs: List<Song>,
    musicViewModel: MusicViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 3 }
    )
    val scope = rememberCoroutineScope()
    
    // Estados de búsqueda y filtrado desde el ViewModel
    val searchQuery by musicViewModel.searchQuery.collectAsState()
    val sortOption by musicViewModel.sortOption.collectAsState()
    
    // Estados del temporizador de suspensión
    val sleepTimerActive by musicViewModel.sleepTimerActive.collectAsState()
    val sleepTimerRemainingTime by musicViewModel.sleepTimerRemainingTime.collectAsState()
    
    // Estado para el diálogo del temporizador
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    
    // Estado para mostrar/ocultar la barra de búsqueda
    var showSearchBar by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A0000)) // Fondo rojo muy oscuro
            .padding(16.dp)
    ) {
        // Fila con botones: búsqueda (izquierda) y filtros + temporizador (derecha)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón de búsqueda (lado izquierdo)
            IconButton(
                onClick = { 
                    showSearchBar = !showSearchBar
                    if (!showSearchBar) {
                        MusicViewModel.Companion.updateSearchQuery(musicViewModel, "")
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Búsqueda",
                    tint = if (showSearchBar || searchQuery.isNotEmpty()) Color(0xFFB71C1C) else Color.White
                )
            }
            
            // Botones del lado derecho (filtros + temporizador)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón de filtros
                IconButton(
                    onClick = { /* Mostrar menú de filtros */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filtros",
                        tint = Color.White
                    )
                }
                
                // Botón del temporizador de suspensión
                IconButton(
                    onClick = { showSleepTimerDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (sleepTimerActive) Color(0xFFB71C1C) else Color(0xFF2A0A0A),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Temporizador de suspensión",
                        tint = Color.White
                    )
                }
            }
        }
        
        // Barra de búsqueda delgada (aparece solo cuando se presiona el botón de búsqueda)
        if (showSearchBar) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { MusicViewModel.Companion.updateSearchQuery(musicViewModel, it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
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
        
        // Tab Row
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color(0xFF2A0A0A),
            contentColor = Color.White,
            modifier = Modifier.padding(bottom = 8.dp),
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = Color(0xFFFF6B6B)
                )
            }
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                },
                text = {
                    Text(
                        text = "Canciones",
                        fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (pagerState.currentPage == 0) Color.White else Color(0xFFBBBBBB)
                    )
                }
            )
            
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
                text = {
                    Text(
                        text = "Carpetas",
                        fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (pagerState.currentPage == 1) Color.White else Color(0xFFBBBBBB)
                    )
                }
            )
            
            Tab(
                selected = pagerState.currentPage == 2,
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(2)
                    }
                },
                text = {
                    Text(
                        text = "Playlists",
                        fontWeight = if (pagerState.currentPage == 2) FontWeight.Bold else FontWeight.Normal,
                        color = if (pagerState.currentPage == 2) Color.White else Color(0xFFBBBBBB)
                    )
                }
            )
        }
        
        // HorizontalPager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> {
                    // Página de Canciones
                    SongListScreen(
                        canciones = songs,
                        musicViewModel = musicViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                1 -> {
                    // Página de Carpetas
                    FolderListScreen(
                        musicViewModel = musicViewModel,
                        navController = navController,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                2 -> {
                    // Página de Playlists
                    PlaylistListScreen(
                        musicViewModel = musicViewModel,
                        navController = navController,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
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
