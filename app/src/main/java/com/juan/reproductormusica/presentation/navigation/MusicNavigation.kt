package com.juan.reproductormusica.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.juan.reproductormusica.data.Song
import com.juan.reproductormusica.presentation.components.MiniPlayer
import com.juan.reproductormusica.presentation.screens.FolderSongsScreen
import com.juan.reproductormusica.presentation.screens.MainTabScreen
import com.juan.reproductormusica.presentation.screens.NowPlayingScreen
import com.juan.reproductormusica.presentation.screens.PlaylistSongsScreen
import com.juan.reproductormusica.presentation.viewmodel.MusicViewModel

/**
 * Rutas de navegación de la aplicación
 */
object MusicDestinations {
    const val MAIN_TABS = "main_tabs"
    const val NOW_PLAYING = "now_playing"
    const val FOLDER_SONGS = "folder_songs/{folderName}"
    const val PLAYLIST_SONGS = "playlist_songs/{playlistId}"
    
    fun createFolderSongsRoute(folderName: String) = "folder_songs/$folderName"
    fun createPlaylistSongsRoute(playlistId: Long) = "playlist_songs/$playlistId"
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
    // Obtener la ruta actual para determinar si mostrar el mini reproductor
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    
    // Ocultar mini reproductor cuando estemos en la pantalla de reproducción
    val shouldShowMiniPlayer = currentRoute != MusicDestinations.NOW_PLAYING
    
    Box(modifier = modifier.fillMaxSize()) {
        // Navegación principal
        NavHost(
            navController = navController,
            startDestination = MusicDestinations.MAIN_TABS,
            modifier = Modifier.fillMaxSize()
        ) {
            // Pantalla principal con pestañas (Canciones, Carpetas y Playlists)
            composable(MusicDestinations.MAIN_TABS) {
                MainTabScreen(
                    songs = songs,
                    musicViewModel = musicViewModel,
                    navController = navController,
                    modifier = if (shouldShowMiniPlayer) Modifier.padding(bottom = 80.dp) else Modifier
                )
            }
            
            // Pantalla de reproducción (sin padding inferior porque no hay mini reproductor)
            composable(MusicDestinations.NOW_PLAYING) {
                NowPlayingScreen(
                    musicViewModel = musicViewModel,
                    onBackPressed = { navController.popBackStack() }
                )
            }
            
            // Pantalla de canciones por carpeta
            composable(
                route = MusicDestinations.FOLDER_SONGS,
                arguments = listOf(navArgument("folderName") { type = NavType.StringType })
            ) { backStackEntry ->
                val folderName = backStackEntry.arguments?.getString("folderName")!!
                FolderSongsScreen(
                    folderName = folderName,
                    musicViewModel = musicViewModel,
                    navController = navController,
                    modifier = if (shouldShowMiniPlayer) Modifier.padding(bottom = 80.dp) else Modifier
                )
            }
            
            // Pantalla de canciones por playlist
            composable(
                route = MusicDestinations.PLAYLIST_SONGS,
                arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getLong("playlistId")!!
                PlaylistSongsScreen(
                    playlistId = playlistId,
                    musicViewModel = musicViewModel,
                    navController = navController,
                    modifier = if (shouldShowMiniPlayer) Modifier.padding(bottom = 80.dp) else Modifier
                )
            }
        }
        
        // Mini-player persistente en la parte inferior (solo si no estamos en NowPlayingScreen)
        if (shouldShowMiniPlayer) {
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
}

/**
 * Extensiones para facilitar la navegación
 */
fun NavHostController.navigateToNowPlaying() {
    navigate(MusicDestinations.NOW_PLAYING) {
        launchSingleTop = true
    }
}

fun NavHostController.navigateToMainTabs() {
    navigate(MusicDestinations.MAIN_TABS) {
        popUpTo(MusicDestinations.MAIN_TABS) {
            inclusive = false
        }
        launchSingleTop = true
    }
}

fun NavHostController.navigateToFolderSongs(folderName: String) {
    navigate(MusicDestinations.createFolderSongsRoute(folderName)) {
        launchSingleTop = true
    }
}

fun NavHostController.navigateToPlaylistSongs(playlistId: Long) {
    navigate(MusicDestinations.createPlaylistSongsRoute(playlistId)) {
        launchSingleTop = true
    }
}
