package com.juan.reproductormusica.service

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.juan.reproductormusica.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manager que maneja la conexión y comunicación con el PlaybackService
 * a través de MediaController. Proporciona una interfaz reactiva para la UI.
 */
class MediaControllerManager(private val context: Context) {
    
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var positionUpdateJob: Job? = null
    
    // Estados reactivos para la UI
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    
    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()
    
    /**
     * Establece si las notificaciones están permitidas por el usuario
     */
    fun setNotificationsAllowed(allowed: Boolean) {
        try {
            mediaController?.let { controller ->
                val bundle = android.os.Bundle().apply {
                    putBoolean("notifications_allowed", allowed)
                }
                val command = androidx.media3.session.SessionCommand(
                    "SET_NOTIFICATIONS_ALLOWED", bundle
                )
                controller.sendCustomCommand(command, bundle)
            }
        } catch (e: Exception) {
            // Continuar sin errores si falla la comunicación
        }
    }
    
    /**
     * Inicializa la conexión con el PlaybackService
     */
    fun initialize() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupPlayerListener()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }
    
    /**
     * Configura el listener para observar cambios en el player
     */
    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startPositionUpdates()
                } else {
                    stopPositionUpdates()
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                _playbackState.value = playbackState
                
                // Actualizar duración cuando el player esté preparado
                if (playbackState == Player.STATE_READY) {
                    updateDurationAndPosition()
                }
            }
            
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateCurrentSong()
                // Forzar actualización de duración al cambiar de canción
                updateDurationAndPosition()
            }
            
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                _currentPosition.value = newPosition.positionMs
                // También actualizar duración por si acaso
                updateDurationAndPosition()
            }
        })
        
        // Inicializar estados actuales
        mediaController?.let { controller ->
            _isPlaying.value = controller.isPlaying
            _playbackState.value = controller.playbackState
            _currentPosition.value = controller.currentPosition
            _duration.value = if (controller.duration > 0) controller.duration else 0L
            updateCurrentSong()
            
            // Forzar actualización inicial de duración y posición
            updateDurationAndPosition()
            
            // Si ya está reproduciendo, iniciar actualizaciones
            if (controller.isPlaying) {
                startPositionUpdates()
            }
        }
    }
    
    /**
     * Actualiza la información de la canción actual
     */
    private fun updateCurrentSong() {
        mediaController?.currentMediaItem?.let { mediaItem ->
            val metadata = mediaItem.mediaMetadata
            val duration = mediaController?.duration ?: 0L
            _currentSong.value = Song(
                id = mediaItem.mediaId.toLongOrNull() ?: 0L,
                title = metadata.title?.toString() ?: "Desconocido",
                artist = metadata.artist?.toString() ?: "Desconocido",
                data = mediaItem.localConfiguration?.uri?.toString() ?: "",
                duration = duration
            )
            _duration.value = duration
        }
    }
    
    /**
     * Actualiza la duración y posición actuales desde el MediaController
     */
    private fun updateDurationAndPosition() {
        mediaController?.let { controller ->
            val currentDuration = controller.duration
            val currentPosition = controller.currentPosition
            
            // Solo actualizar si tenemos valores válidos
            if (currentDuration > 0) {
                _duration.value = currentDuration
            }
            
            if (currentPosition >= 0) {
                _currentPosition.value = currentPosition
            }
            
            // También actualizar la canción actual con la nueva duración
            _currentSong.value?.let { song ->
                if (song.duration != currentDuration && currentDuration > 0) {
                    _currentSong.value = song.copy(duration = currentDuration)
                }
            }
        }
    }
    
    /**
     * Inicia las actualizaciones periódicas de posición
     */
    private fun startPositionUpdates() {
        stopPositionUpdates() // Detener cualquier actualización previa
        positionUpdateJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaController?.let { controller ->
                    _currentPosition.value = controller.currentPosition
                    
                    // También actualizar duración si aún no la tenemos o cambió
                    val currentDuration = controller.duration
                    if (currentDuration > 0 && _duration.value != currentDuration) {
                        _duration.value = currentDuration
                        
                        // Actualizar la canción actual con la nueva duración
                        _currentSong.value?.let { song ->
                            if (song.duration != currentDuration) {
                                _currentSong.value = song.copy(duration = currentDuration)
                            }
                        }
                    }
                }
                delay(500) // Actualizar cada 500ms
            }
        }
    }
    
    /**
     * Detiene las actualizaciones periódicas de posición
     */
    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }
    
    /**
     * Configura y reproduce una playlist de canciones
     */
    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.data)
                .setMediaId(song.id.toString())
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setIsPlayable(true)
                        .build()
                )
                .build()
        }
        
        mediaController?.setMediaItems(mediaItems, startIndex, 0)
        mediaController?.prepare()
    }
    
    /**
     * Actualiza la playlist sin cambiar la canción actual
     */
    fun updatePlaylistWithoutChangingSong(songs: List<Song>) {
        mediaController?.let { controller ->
            val currentSongId = controller.currentMediaItem?.mediaId
            val currentPosition = controller.currentPosition
            val wasPlaying = controller.isPlaying
            
            val mediaItems = songs.map { song ->
                MediaItem.Builder()
                    .setUri(song.data)
                    .setMediaId(song.id.toString())
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artist)
                            .setIsPlayable(true)
                            .build()
                    )
                    .build()
            }
            
            // Buscar el índice de la canción actual en la nueva playlist
            val newIndex = if (currentSongId != null) {
                mediaItems.indexOfFirst { it.mediaId == currentSongId }
            } else {
                -1
            }
            
            if (newIndex != -1) {
                // La canción actual existe en la nueva playlist
                controller.setMediaItems(mediaItems, newIndex, currentPosition)
                controller.prepare()
                
                // Restaurar el estado de reproducción
                if (wasPlaying) {
                    controller.play()
                }
            } else {
                // La canción actual no existe en la nueva playlist, usar el método normal
                setPlaylist(songs, 0)
            }
        }
    }
    
    /**
     * Controles de reproducción
     */
    fun play() {
        mediaController?.play()
    }
    
    fun pause() {
        mediaController?.pause()
    }
    
    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }
    
    fun seekToNext() {
        mediaController?.seekToNext()
    }
    
    fun seekToPrevious() {
        mediaController?.seekToPrevious()
    }
    
    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }
    
    fun seekToIndex(index: Int) {
        mediaController?.seekToDefaultPosition(index)
        mediaController?.play()
    }
    
    /**
     * Obtiene la posición actual de reproducción
     */
    fun getCurrentPosition(): Long {
        return mediaController?.currentPosition ?: 0L
    }
    
    /**
     * Obtiene la duración total de la canción actual
     */
    fun getDuration(): Long {
        return mediaController?.duration ?: 0L
    }
    
    /**
     * Libera los recursos del MediaController
     */
    fun release() {
        stopPositionUpdates()
        controllerFuture?.let { future ->
            MediaController.releaseFuture(future)
        }
        mediaController = null
        controllerFuture = null
    }
}
