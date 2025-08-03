package com.juan.reproductormusica.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.juan.reproductormusica.data.Song
import com.juan.reproductormusica.presentation.components.MiniPlayer
import com.juan.reproductormusica.presentation.screens.NowPlayingScreen
import com.juan.reproductormusica.presentation.screens.SongListScreen
import com.juan.reproductormusica.presentation.viewmodel.MusicViewModel

/**
 * Rutas de navegación de la aplicación
 */
object MusicDestinations {
    const val SONG_LIST = "song_list"
    const val NOW_PLAYING = "now_playing"
}

/**
 * Composable principal que maneja la navegación de la aplicación
 * Incluye el mini-player persistente en todas las pantallas
 */
@Composable
fun MusicNavigation(
    songs: List<Song>,
    musicViewModel: MusicViewModel,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Navegación principal
        NavHost(
            navController = navController,
            startDestination = MusicDestinations.SONG_LIST,
            modifier = Modifier.fillMaxSize()
        ) {
            // Pantalla principal con lista de canciones
            composable(MusicDestinations.SONG_LIST) {
                SongListScreen(
                    canciones = songs,
                    musicViewModel = musicViewModel,
                    // Padding bottom para el mini-player
                    modifier = Modifier.padding(bottom = 80.dp)
                )
            }
            
            // Pantalla "Now Playing" detallada
            composable(MusicDestinations.NOW_PLAYING) {
                NowPlayingScreen(
                    musicViewModel = musicViewModel,
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
            }
        }
        
        // Mini-player persistente en la parte inferior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .align(androidx.compose.ui.Alignment.BottomCenter)
        ) {
            MiniPlayer(
                musicViewModel = musicViewModel,
                onNavigateToNowPlaying = {
                    navController.navigate(MusicDestinations.NOW_PLAYING) {
                        // Evitar múltiples instancias de la misma pantalla
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

/**
 * Extensiones para facilitar la navegación
 */
fun NavHostController.navigateToNowPlaying() {
    navigate(MusicDestinations.NOW_PLAYING) {
        launchSingleTop = true
    }
}

fun NavHostController.navigateToSongList() {
    navigate(MusicDestinations.SONG_LIST) {
        popUpTo(MusicDestinations.SONG_LIST) {
            inclusive = false
        }
        launchSingleTop = true
    }
}
