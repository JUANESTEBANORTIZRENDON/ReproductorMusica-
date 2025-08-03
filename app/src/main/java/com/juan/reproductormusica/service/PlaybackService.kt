package com.juan.reproductormusica.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.juan.reproductormusica.R
import com.juan.reproductormusica.data.Song
import com.juan.reproductormusica.presentation.MainActivity

/**
 * Servicio centralizado de reproducción que maneja toda la lógica de audio
 * usando ExoPlayer y MediaSession para sincronización completa entre UI y controles del sistema.
 */
class PlaybackService : MediaSessionService() {
    
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    
    // Lista de canciones actual
    private var currentPlaylist: List<Song> = emptyList()
    private var currentSongIndex: Int = 0
    
    // Control de notificaciones
    private var notificationsAllowed = false // Por defecto habilitado
    
    companion object {
        // Comandos personalizados para acciones específicas
        private const val CUSTOM_COMMAND_TOGGLE_SHUFFLE = "TOGGLE_SHUFFLE"
        private const val CUSTOM_COMMAND_TOGGLE_REPEAT = "TOGGLE_REPEAT"
        private const val CUSTOM_COMMAND_SET_NOTIFICATIONS = "SET_NOTIFICATIONS_ALLOWED"
    }
    
    override fun onCreate() {
        super.onCreate()
        initializeSessionAndPlayer()
    }
    
    /**
     * Inicializa ExoPlayer y MediaSession con configuración optimizada para música
     */
    private fun initializeSessionAndPlayer() {
        // Configurar ExoPlayer con atributos de audio para música
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // maneja el foco de audio automáticamente
            )
            .setHandleAudioBecomingNoisy(true) // pausa cuando se desconectan auriculares
            .build()
        
        // Agregar listener para actualizar el índice actual y controlar notificaciones
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentSongIndex = player.currentMediaItemIndex
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                // Controlar notificaciones según estado y permisos
                updateSystemNotifications()
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // Controlar notificaciones cuando inicia/pausa reproducción
                updateSystemNotifications()
            }
        })
        
        // Configurar MediaSession con intent para abrir la app
        val sessionActivityPendingIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
            PendingIntent.getActivity(this, 0, sessionIntent, PendingIntent.FLAG_IMMUTABLE)
        }
        
        mediaSession = sessionActivityPendingIntent?.let {
            MediaSession.Builder(this, player)
                .setCallback(MediaSessionCallback())
                .setSessionActivity(it)
                .build()
        }
    }
    
    /**
     * Callback que maneja las acciones de la MediaSession
     */
    private inner class MediaSessionCallback : MediaSession.Callback {
        
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
            
            // Agregar comandos personalizados
            availableSessionCommands.add(SessionCommand(CUSTOM_COMMAND_TOGGLE_SHUFFLE, Bundle.EMPTY))
            availableSessionCommands.add(SessionCommand(CUSTOM_COMMAND_TOGGLE_REPEAT, Bundle.EMPTY))
            availableSessionCommands.add(SessionCommand(CUSTOM_COMMAND_SET_NOTIFICATIONS, Bundle.EMPTY))
            
            return MediaSession.ConnectionResult.accept(
                availableSessionCommands.build(),
                connectionResult.availablePlayerCommands
            )
        }
        
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CUSTOM_COMMAND_TOGGLE_SHUFFLE -> {
                    player.shuffleModeEnabled = !player.shuffleModeEnabled
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                CUSTOM_COMMAND_TOGGLE_REPEAT -> {
                    player.repeatMode = when (player.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                CUSTOM_COMMAND_SET_NOTIFICATIONS -> {
                    notificationsAllowed = args.getBoolean("notifications_allowed", false)
                    
                    // Actualizar notificaciones del sistema sin afectar funcionalidad interna
                    updateSystemNotifications()
                    
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
        
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            // Procesar los MediaItems para asegurar que tengan metadatos completos
            val updatedMediaItems = mediaItems.map { mediaItem ->
                mediaItem.buildUpon()
                    .setMediaMetadata(
                        mediaItem.mediaMetadata.buildUpon()
                            .setIsPlayable(true)
                            .build()
                    )
                    .build()
            }.toMutableList()
            
            return Futures.immediateFuture(updatedMediaItems)
        }
    }
    
    /**
     * Controla las notificaciones del sistema sin afectar la funcionalidad interna
     */
    private fun updateSystemNotifications() {
        // Usar el método agresivo para suprimir notificaciones de ExoPlayer
        suppressExoPlayerNotifications()
    }
    
    /**
     * Control agresivo de notificaciones de ExoPlayer
     */
    private fun suppressExoPlayerNotifications() {
        try {
            if (!notificationsAllowed) {
                // Detener servicio foreground
                stopForeground(STOP_FOREGROUND_REMOVE)
                
                // Cancelar todas las notificaciones del sistema
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancelAll()
                
                // Usar un handler para cancelar notificaciones que aparezcan después
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        notificationManager.cancelAll()
                    } catch (e: Exception) {
                        // Ignorar errores
                    }
                }, 100) // Cancelar después de 100ms
            }
        } catch (e: Exception) {
            // Ignorar errores
        }
    }
    
    /**
     * Fuerza la eliminación de la notificación del sistema (NO afecta funcionalidad interna)
     */
    private fun forceRemoveNotification() {
        suppressExoPlayerNotifications()
    }
    
    /**
     * Configura la playlist desde una lista de canciones
     */
    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        currentPlaylist = songs
        currentSongIndex = startIndex
        
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.data)
                .setMediaId(song.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setIsPlayable(true)
                        .build()
                )
                .build()
        }
        
        player.setMediaItems(mediaItems, startIndex, 0)
        player.prepare()
    }
    
    /**
     * Reproduce una canción específica de la playlist
     */
    fun playSong(songIndex: Int) {
        if (songIndex in currentPlaylist.indices) {
            currentSongIndex = songIndex
            player.seekToDefaultPosition(songIndex)
            player.play()
        }
    }
    
    /**
     * Obtiene la canción actualmente en reproducción
     */
    fun getCurrentSong(): Song? {
        return if (currentSongIndex in currentPlaylist.indices) {
            currentPlaylist[currentSongIndex]
        } else null
    }
    
    // Implementación requerida de MediaSessionService
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        // SIEMPRE permitir conexiones de la app (para MiniPlayer interno)
        // Solo controlar notificaciones del sistema por separado
        return mediaSession
    }
    
    override fun onDestroy() {
        // Liberar recursos correctamente
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
