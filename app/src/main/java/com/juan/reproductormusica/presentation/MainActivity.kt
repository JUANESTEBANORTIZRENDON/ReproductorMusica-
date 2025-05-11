package com.juan.reproductormusica.presentation

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.juan.reproductormusica.data.Song
import com.juan.reproductormusica.repository.MusicRepository
import com.juan.reproductormusica.ui.theme.ReproductorMusicaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ReproductorMusicaTheme(darkTheme = true) {
                Surface(color = Color(0xFF731912)) {
                    var canciones by remember { mutableStateOf<List<Song>>(emptyList()) }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            canciones = MusicRepository.getAllSongs(this)
                        }
                    }

                    LaunchedEffect(Unit) {
                        val permiso = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_AUDIO
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                        permissionLauncher.launch(permiso)
                    }

                    com.juan.reproductormusica.presentation.screens.SongListScreen(canciones)
                }
            }
        }
    }
}



