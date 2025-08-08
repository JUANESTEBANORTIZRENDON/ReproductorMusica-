package com.juan.reproductormusica.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.juan.reproductormusica.data.Song
import com.juan.reproductormusica.presentation.components.SearchAndFilterBar
import com.juan.reproductormusica.presentation.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla principal con pestañas para Canciones y Carpetas usando HorizontalPager
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
        pageCount = { 2 }
    )
    val scope = rememberCoroutineScope()
    
    // Estados de búsqueda y filtrado desde el ViewModel
    val searchQuery by musicViewModel.searchQuery.collectAsState()
    val sortOption by musicViewModel.sortOption.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A0000)) // Fondo rojo muy oscuro
            .padding(16.dp)
    ) {
        // Barra de búsqueda y filtrado (ARRIBA de las pestañas)
        SearchAndFilterBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { MusicViewModel.Companion.updateSearchQuery(musicViewModel, it) },
            showSortControls = true,
            sortOption = sortOption,
            onSortOptionChange = { musicViewModel.updateSortOption(it) },
            onClearSearch = { MusicViewModel.Companion.updateSearchQuery(musicViewModel, "") },
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
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
            }
        }
    }
}
