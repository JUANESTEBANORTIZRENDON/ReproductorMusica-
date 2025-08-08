package com.juan.reproductormusica.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.juan.reproductormusica.data.Song
import com.juan.reproductormusica.data.database.PlaylistEntity
import com.juan.reproductormusica.data.database.PlaylistWithSongs
import com.juan.reproductormusica.repository.PlaylistRepository
import com.juan.reproductormusica.service.MediaControllerManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import java.io.File
import java.util.Calendar
import kotlin.system.exitProcess

/**
 * ViewModel que gestiona el estado de reproducción de música siguiendo el patrón MVVM.
 *
 * Actúa como intermediario entre la UI (Compose) y la lógica de reproducción (MediaControllerManager),
 * proporcionando estados reactivos y manejando eventos del usuario de manera centralizada.
 *
 * Características principales:
 * - Estados reactivos con StateFlow
 * - Delegación de comandos al MediaControllerManager
 * - UI completamente desacoplada de la lógica de reproducción
 * - Sincronización automática con controles del sistema
 */
@OptIn(FlowPreview::class)
class MusicViewModel(
    private val mediaControllerManager: MediaControllerManager,
    val playlistRepository: PlaylistRepository
) : ViewModel() {

    // ========================================
    // ESTADOS DE PLAYLISTS
    // ========================================

    /** Lista de todas las playlists con sus canciones */
    val playlistsWithSongs: StateFlow<List<PlaylistWithSongs>> = playlistRepository.playlistsWithSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /** Set de IDs de canciones favoritas para verificación rápida */
    val favoriteSongIds: StateFlow<Set<Long>> = playlistRepository.getFavoriteSongIds().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    // ========================================
    // ESTADOS DE SHUFFLE Y REPEAT
    // ========================================

    /** Estado de reproducción aleatoria (shuffle) */
    val isShuffleEnabled: StateFlow<Boolean> = mediaControllerManager.isShuffleEnabled
    /** Estado del modo de repetición (OFF, ALL) */
    val repeatMode: StateFlow<Int> = mediaControllerManager.repeatMode

    // ========================================
    // ESTADOS DE BÚSQUEDA Y FILTRADO
    // ========================================

    // Lista original de canciones (sin filtrar)
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    // Playlist actual (fuente de verdad en el VM)
    private val _currentPlaylist = MutableStateFlow<List<Song>>(emptyList())
    val currentPlaylist: StateFlow<List<Song>> = _currentPlaylist.asStateFlow()

    // Término de búsqueda actual
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Opción de ordenamiento actual
    private val _sortOption = MutableStateFlow(SortOption.DEFAULT)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    // Lista filtrada y ordenada (resultado final)
    val filteredSongs: StateFlow<List<Song>> = combine(
        _allSongs,
        _searchQuery.debounce(300), // Debounce para optimizar rendimiento
        _sortOption
    ) { songs, query, sort ->
        songs
            .applySearch(query)
            .applySorting(sort)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ========================================
    // ESTADOS REACTIVOS PARA LA UI
    // ========================================

    /** Canción actualmente en reproducción o seleccionada */
    val currentSong: StateFlow<Song?> = mediaControllerManager.currentSong

    /** Estado de reproducción (true = reproduciendo, false = pausado) */
    val isPlaying: StateFlow<Boolean> = mediaControllerManager.isPlaying

    /** Posición actual de reproducción en milisegundos */
    val position: StateFlow<Long> = mediaControllerManager.currentPosition

    /** Duración total de la canción actual en milisegundos */
    val duration: StateFlow<Long> = mediaControllerManager.duration

    /** Estado del reproductor (IDLE, BUFFERING, READY, ENDED) */
    val playbackState: StateFlow<Int> = mediaControllerManager.playbackState

    // ========================================
    // TEMPORIZADOR DE SUSPENSIÓN (SLEEP TIMER)
    // ========================================

    private var sleepTimerJob: Job? = null
    private val _sleepTimerActive = MutableStateFlow(false)
    private val _sleepTimerEndTime = MutableStateFlow<Long?>(null)
    private val _sleepTimerRemainingTime = MutableStateFlow(0L)

    /** Indica si el temporizador de suspensión está activo */
    val sleepTimerActive: StateFlow<Boolean> = _sleepTimerActive.asStateFlow()

    /** Hora de finalización del temporizador en milisegundos */
    val sleepTimerEndTime: StateFlow<Long?> = _sleepTimerEndTime.asStateFlow()

    /** Tiempo restante del temporizador en milisegundos */
    val sleepTimerRemainingTime: StateFlow<Long> = _sleepTimerRemainingTime.asStateFlow()

    // ========================================
    // EVENTOS DE USUARIO - BÚSQUEDA Y FILTRADO
    // ========================================

    /** Actualiza la opción de ordenamiento */
    fun updateSortOption(option: SortOption) {
        _sortOption.value = option
    }

    /** Limpia el término de búsqueda */
    fun clearSearch() {
        _searchQuery.value = ""
    }

    /**
     * Configura la lista completa de canciones (llamado desde MainActivity)
     */
    fun setAllSongs(songs: List<Song>) {
        _allSongs.value = songs

        // Si aún no hay playlist activa en el VM, usar todas las canciones
        if (_currentPlaylist.value.isEmpty()) {
            setPlaylist(songs)
        }
    }

    /**
     * Obtiene las canciones agrupadas por carpeta
     */
    fun getFolders(): Map<String, List<Song>> {
        return _allSongs.value.groupBy { song ->
            try {
                File(song.data).parentFile?.name ?: "Desconocida"
            } catch (_: Exception) {
                "Desconocida"
            }
        }
    }

    /**
     * Obtiene las canciones de una carpeta específica
     */
    fun getSongsByFolder(folderName: String): List<Song> {
        return _allSongs.value.filter { song ->
            try {
                File(song.data).parentFile?.name == folderName
            } catch (_: Exception) {
                false
            }
        }
    }

    // ========================================
    // EVENTOS DE USUARIO - CONTROLES DE REPRODUCCIÓN
    // ========================================

    /** Alterna entre reproducir y pausar la canción actual */
    fun togglePlayPause() {
        viewModelScope.launch { mediaControllerManager.togglePlayPause() }
    }

    /** Reproduce la canción actual (si está pausada) */
    fun play() {
        viewModelScope.launch { mediaControllerManager.play() }
    }

    /** Pausa la reproducción actual */
    fun pause() {
        viewModelScope.launch { mediaControllerManager.pause() }
    }

    /** Avanza a la siguiente canción en la playlist */
    fun skipToNext() {
        viewModelScope.launch { mediaControllerManager.seekToNext() }
    }

    /** Retrocede a la canción anterior en la playlist */
    fun skipToPrevious() {
        viewModelScope.launch { mediaControllerManager.seekToPrevious() }
    }

    /**
     * Busca a una posición específica en la canción actual
     */
    fun seekTo(positionMs: Long) {
        viewModelScope.launch { mediaControllerManager.seekTo(positionMs) }
    }

    // ========================================
    // EVENTOS DE USUARIO - GESTIÓN DE PLAYLIST
    // ========================================

    /**
     * Alterna el modo aleatorio (shuffle) del reproductor
     */
    fun toggleShuffle() {
        viewModelScope.launch { mediaControllerManager.toggleShuffle() }
    }

    /**
     * Alterna el modo de repetición (repeat) del reproductor
     */
    fun toggleRepeat() {
        viewModelScope.launch { mediaControllerManager.toggleRepeat() }
    }

    /**
     * Inicia reproducción aleatoria de la lista filtrada (Shuffle Play)
     */
    fun shuffleAndPlay() {
        val base = filteredSongs.value
        if (base.isEmpty()) return
        // Forzar modo shuffle ON antes de reproducir
        viewModelScope.launch { mediaControllerManager.setShuffleEnabled(true) }
        val shuffled = base.shuffled()
        setPlaylist(shuffled, startIndex = 0)
        play()
    }

    /**
     * Configura una nueva playlist y opcionalmente inicia la reproducción
     */
    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        viewModelScope.launch {
            _currentPlaylist.value = songs  // mantener estado local
            mediaControllerManager.setPlaylist(songs, startIndex)
        }
    }
    
    /**
     * Actualiza la playlist sin cambiar la canción actual
     */
    fun updatePlaylistWithoutChangingSong(songs: List<Song>) {
        viewModelScope.launch {
            _currentPlaylist.value = songs  // mantener estado local
            mediaControllerManager.updatePlaylistWithoutChangingSong(songs)
        }
    }

    /**
     * Reproduce una canción específica de la playlist actual por índice
     */
    fun playSongAtIndex(songIndex: Int) {
        viewModelScope.launch { mediaControllerManager.seekToIndex(songIndex) }
    }

    /**
     * Reproduce una canción específica buscándola en la lista completa
     */
    fun playSong(song: Song) {
        viewModelScope.launch {
            val list = _allSongs.value
            val index = list.indexOfFirst { it.id == song.id }
            if (index != -1) {
                mediaControllerManager.seekToIndex(index)
            }
        }
    }

    // ========================================
    // EVENTOS DE USUARIO - GESTIÓN DE PLAYLISTS
    // ========================================

    /**
     * Inicializa la playlist de favoritos (llamado desde MainActivity)
     */
    fun initializePlaylists() {
        viewModelScope.launch {
            playlistRepository.initializeFavoritesPlaylist()
        }
    }

    /**
     * Crea una nueva playlist
     */
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name)
        }
    }

    /**
     * Crea una nueva playlist y agrega automáticamente la canción especificada
     */
    fun createPlaylistWithSong(name: String, song: Song) {
        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(name)
            if (playlistId > 0) {
                playlistRepository.addSongToPlaylist(song, playlistId)
            }
        }
    }

    /**
     * Elimina una playlist
     */
    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
        }
    }

    /**
     * Renombra una playlist
     */
    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            playlistRepository.renamePlaylist(playlistId, newName)
        }
    }

    /**
     * Añade una canción a una playlist específica
     */
    fun addSongToPlaylist(song: Song, playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.addSongToPlaylist(song, playlistId)
        }
    }

    /**
     * Quita una canción de una playlist específica
     */
    fun removeSongFromPlaylist(songId: Long, playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.removeSongFromPlaylist(songId, playlistId)
        }
    }

    /**
     * Alterna el estado de favorito de una canción
     */
    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            playlistRepository.toggleFavorite(song)
        }
    }

    /**
     * Verifica si una canción está en favoritos
     */
    fun isSongFavorite(songId: Long): Boolean {
        return favoriteSongIds.value.contains(songId)
    }

    /**
     * Obtiene una playlist con sus canciones
     */
    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs?> {
        return playlistRepository.getPlaylistWithSongs(playlistId)
    }

    /**
     * Obtiene las canciones de una playlist como Song (para reproducción)
     */
    fun getSongsFromPlaylist(playlistId: Long): Flow<List<Song>> {
        return playlistRepository.getSongsInPlaylist(playlistId).map { songEntities ->
            songEntities.map { entity ->
                with(playlistRepository) { entity.toSong() }
            }
        }
    }

    // ========================================
    // MÉTODOS DE UTILIDAD
    // ========================================

    /** Progreso de reproducción (0.0 - 1.0) */
    fun getPlaybackProgress(): Float {
        val currentPos = position.value
        val totalDuration = duration.value
        return if (totalDuration > 0) {
            (currentPos.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
        } else 0f
    }

    /** ¿Hay siguiente? */
    fun hasNext(): Boolean = playbackState.value != Player.STATE_ENDED

    /** ¿Hay anterior? */
    fun hasPrevious(): Boolean = currentSong.value != null

    /** Formatea milisegundos a MM:SS */
    fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    // ========================================
    // EVENTOS DE USUARIO - BÚSQUEDA Y FILTRADO
    // ========================================

    /**
     * Actualiza el término de búsqueda
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // ========================================
    // MÉTODOS DEL TEMPORIZADOR DE SUSPENSIÓN
    // ========================================

    /**
     * Configura el temporizador de suspensión para una hora específica
     */
    fun setSleepTimer(hour: Int, minute: Int) {
        // Cancelar temporizador anterior si existe
        cancelSleepTimer()
        
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()
        
        // Configurar la hora objetivo
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        var targetTime = calendar.timeInMillis
        
        // Si la hora ya pasó hoy, programar para mañana
        if (targetTime <= now) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            targetTime = calendar.timeInMillis
        }
        
        val delayMs = targetTime - now
        
        _sleepTimerActive.value = true
        _sleepTimerEndTime.value = targetTime
        
        sleepTimerJob = viewModelScope.launch {
            // Actualizar tiempo restante cada segundo
            while (_sleepTimerActive.value && System.currentTimeMillis() < targetTime) {
                val remaining = targetTime - System.currentTimeMillis()
                _sleepTimerRemainingTime.value = remaining.coerceAtLeast(0)
                delay(1000)
            }
            
            // Ejecutar suspensión si el temporizador no fue cancelado
            if (_sleepTimerActive.value) {
                executeSleepAction()
            }
        }
    }

    /**
     * Configura el temporizador de suspensión para un tiempo específico en minutos
     */
    fun setSleepTimerInMinutes(minutes: Int) {
        // Cancelar temporizador anterior si existe
        cancelSleepTimer()
        
        val now = System.currentTimeMillis()
        val targetTime = now + (minutes * 60 * 1000L)
        
        _sleepTimerActive.value = true
        _sleepTimerEndTime.value = targetTime
        
        sleepTimerJob = viewModelScope.launch {
            // Actualizar tiempo restante cada segundo
            while (_sleepTimerActive.value && System.currentTimeMillis() < targetTime) {
                val remaining = targetTime - System.currentTimeMillis()
                _sleepTimerRemainingTime.value = remaining.coerceAtLeast(0)
                delay(1000)
            }
            
            // Ejecutar suspensión si el temporizador no fue cancelado
            if (_sleepTimerActive.value) {
                executeSleepAction()
            }
        }
    }

    /**
     * Cancela el temporizador de suspensión activo
     */
    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerActive.value = false
        _sleepTimerEndTime.value = null
        _sleepTimerRemainingTime.value = 0L
    }

    /**
     * Ejecuta la acción de suspensión: detiene la reproducción y cierra la app
     */
    private fun executeSleepAction() {
        viewModelScope.launch {
            // Detener la reproducción
            pause()
            
            // Resetear estados del temporizador
            _sleepTimerActive.value = false
            _sleepTimerEndTime.value = null
            _sleepTimerRemainingTime.value = 0L
            
            // Cerrar la aplicación después de un breve delay
            delay(500)
            exitProcess(0)
        }
    }

    /**
     * Formatea el tiempo restante del temporizador a formato legible
     */
    fun formatSleepTimerRemaining(timeMs: Long): String {
        if (timeMs <= 0) return "00:00:00"
        
        val totalSeconds = timeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    // ========================================
    // GESTIÓN DE RECURSOS
    // ========================================

    override fun onCleared() {
        super.onCleared()
        // Cancelar temporizador de suspensión si está activo
        cancelSleepTimer()
        // El MediaControllerManager se libera en MainActivity.onDestroy()
    }

    // ========================================
    // FACTORY PARA INYECCIÓN DE DEPENDENCIAS
    // ========================================

    class Factory(
        private val mediaControllerManager: MediaControllerManager,
        private val playlistRepository: PlaylistRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
                return MusicViewModel(mediaControllerManager, playlistRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        /** Actualiza el término de búsqueda */
        fun updateSearchQuery(musicViewModel: MusicViewModel, query: String) {
            musicViewModel._searchQuery.value = query
        }

        /**
         * Actualiza la opción de ordenamiento
         */
        fun SortOption.updateSortOption(musicViewModel: MusicViewModel) {
            musicViewModel._sortOption.value = this
        }
    }
}

/** Opciones de ordenamiento para la lista de canciones */
enum class SortOption(val displayName: String) {
    TITLE_ASC("Título A-Z"),
    TITLE_DESC("Título Z-A"),
    ARTIST_ASC("Artista A-Z"),
    ARTIST_DESC("Artista Z-A"),
    DURATION_ASC("Duración ↑"),
    DURATION_DESC("Duración ↓"),
    DEFAULT("Por Carpeta") // Ordenamiento original por carpeta
}

/** Aplica el ordenamiento seleccionado a una lista de canciones */
fun List<Song>.applySorting(sortOption: SortOption): List<Song> {
    return when (sortOption) {
        SortOption.TITLE_ASC -> this.sortedBy { it.title.lowercase() }
        SortOption.TITLE_DESC -> this.sortedByDescending { it.title.lowercase() }
        SortOption.ARTIST_ASC -> this.sortedBy { it.artist.lowercase() }
        SortOption.ARTIST_DESC -> this.sortedByDescending { it.artist.lowercase() }
        SortOption.DURATION_ASC -> this.sortedBy { it.duration }
        SortOption.DURATION_DESC -> this.sortedByDescending { it.duration }
        SortOption.DEFAULT -> this.sortedBy { it.artist.lowercase() } // Mantener orden original por carpeta
    }
}

/** Estados del reproductor (helper) */
object PlaybackStates {
    const val STATE_IDLE = Player.STATE_IDLE
    const val STATE_BUFFERING = Player.STATE_BUFFERING
    const val STATE_READY = Player.STATE_READY
    const val STATE_ENDED = Player.STATE_ENDED
}

/** Helpers de estado útiles en Compose */
val MusicViewModel.isBuffering: Boolean
    get() = playbackState.value == Player.STATE_BUFFERING

val MusicViewModel.isReady: Boolean
    get() = playbackState.value == Player.STATE_READY

val MusicViewModel.isEnded: Boolean
    get() = playbackState.value == Player.STATE_ENDED

/**
 * Aplica búsqueda por título, “artista” (carpeta) y nombre de carpeta real.
 */
private fun List<Song>.applySearch(query: String): List<Song> {
    if (query.isBlank()) return this
    val searchTerm = query.lowercase().trim()
    return filter { song ->
        song.title.lowercase().contains(searchTerm) ||
                song.artist.lowercase().contains(searchTerm) ||
                runCatching { File(song.data).parentFile?.name?.lowercase()?.contains(searchTerm) == true }
                    .getOrDefault(false)
    }
}
