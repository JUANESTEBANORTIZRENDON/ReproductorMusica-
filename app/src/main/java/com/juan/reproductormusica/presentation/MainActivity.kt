package com.juan.reproductormusica.presentation

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.juan.reproductormusica.data.Song
import com.juan.reproductormusica.repository.MusicRepository
import com.juan.reproductormusica.service.MediaControllerManager
import com.juan.reproductormusica.presentation.viewmodel.MusicViewModel
import com.juan.reproductormusica.presentation.navigation.MusicNavigation
import com.juan.reproductormusica.ui.theme.ReproductorMusicaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var mediaControllerManager: MediaControllerManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar MediaControllerManager
        mediaControllerManager = MediaControllerManager(this)
        mediaControllerManager.initialize()

        setContent {
            ReproductorMusicaTheme(darkTheme = true) {
                var canciones by remember { mutableStateOf<List<Song>>(emptyList()) }
                
                // Crear ViewModel con Factory para inyección de dependencias
                val musicViewModel: MusicViewModel = viewModel(
                    factory = MusicViewModel.Factory(mediaControllerManager)
                )
                
                // Crear NavController
                val navController = rememberNavController()
                
                // Crear CoroutineScope para operaciones asíncronas
                val coroutineScope = rememberCoroutineScope()

                // Launcher para permisos de almacenamiento
                val storagePermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        canciones = MusicRepository.getAllSongs(this@MainActivity)
                    }
                }
                
                // SharedPreferences para controlar estado de permisos
                val sharedPrefs = remember { 
                    getSharedPreferences("music_app_prefs", Context.MODE_PRIVATE) 
                }
                
                // Estado para controlar permisos
                var notificationPermissionAsked by remember { 
                    mutableStateOf(sharedPrefs.getBoolean("notification_permission_asked", false)) 
                }
                var storagePermissionGranted by remember { mutableStateOf(false) }
                var notificationsConfigured by remember { mutableStateOf(false) }
                
                // Launcher para permisos de notificaciones (PRIMERO)
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    // Guardar resultado
                    sharedPrefs.edit()
                        .putBoolean("notification_permission_asked", true)
                        .putBoolean("notification_permission_granted", isGranted)
                        .apply()
                    
                    notificationPermissionAsked = true
                    notificationsConfigured = true
                    
                    // Configurar notificaciones con delay para asegurar que el servicio esté listo
                    coroutineScope.launch {
                        delay(500) // Esperar medio segundo
                        mediaControllerManager.setNotificationsAllowed(isGranted)
                    }
                    
                    // Ahora solicitar permisos de almacenamiento
                    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    storagePermissionLauncher.launch(storagePermission)
                }

                LaunchedEffect(Unit) {
                    // PASO 1: Configurar notificaciones según historial
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (!notificationPermissionAsked) {
                            // PRIMERA VEZ - Solicitar permisos de notificaciones PRIMERO
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            // Ya se pidió antes, configurar y continuar
                            val granted = sharedPrefs.getBoolean("notification_permission_granted", false)
                            coroutineScope.launch {
                                delay(500) // Esperar para que el servicio esté listo
                                mediaControllerManager.setNotificationsAllowed(granted)
                            }
                            notificationsConfigured = true
                            
                            // Solicitar almacenamiento
                            val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.READ_MEDIA_AUDIO
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                            storagePermissionLauncher.launch(storagePermission)
                        }
                    } else {
                        // Android 12 y anteriores - habilitar notificaciones y continuar
                        mediaControllerManager.setNotificationsAllowed(true)
                        notificationsConfigured = true
                        
                        // Solicitar almacenamiento
                        storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
                
                // Configurar playlist cuando se cargan las canciones
                LaunchedEffect(canciones) {
                    if (canciones.isNotEmpty()) {
                        musicViewModel.setAllSongs(canciones)
                    }
                }

                // Sistema de navegación con mini-player persistente
                MusicNavigation(
                    songs = canciones,
                    musicViewModel = musicViewModel,
                    navController = navController
                )
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Liberar recursos del MediaController
        mediaControllerManager.release()
    }
}



