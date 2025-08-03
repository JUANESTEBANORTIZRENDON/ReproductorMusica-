package com.juan.reproductormusica.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.juan.reproductormusica.data.Song
import com.juan.reproductormusica.service.MediaControllerManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

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
    private val mediaControllerManager: MediaControllerManager
) : ViewModel() {
    
    // ========================================
    // ESTADOS DE BÚSQUEDA Y FILTRADO
    // ========================================
    
    // Lista original de canciones (sin filtrar)
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    
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
    
    /**
     * Canción actualmente en reproducción o seleccionada
     */
    val currentSong: StateFlow<Song?> = mediaControllerManager.currentSong
    
    /**
     * Estado de reproducción (true = reproduciendo, false = pausado)
     */
    val isPlaying: StateFlow<Boolean> = mediaControllerManager.isPlaying
    
    /**
     * Posición actual de reproducción en milisegundos
     */
    val position: StateFlow<Long> = mediaControllerManager.currentPosition
    
    /**
     * Duración total de la canción actual en milisegundos
     */
    val duration: StateFlow<Long> = mediaControllerManager.duration
    
    /**
     * Estado del reproductor (IDLE, BUFFERING, READY, ENDED)
     */
    val playbackState: StateFlow<Int> = mediaControllerManager.playbackState
    
    // ========================================
    // EVENTOS DE USUARIO - BÚSQUEDA Y FILTRADO
    // ========================================
    
    /**
     * Actualiza el término de búsqueda
     * 
     * @param query Nuevo término de búsqueda
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Actualiza la opción de ordenamiento
     * 
     * @param option Nueva opción de ordenamiento
     */
    fun updateSortOption(option: SortOption) {
        _sortOption.value = option
    }
    
    /**
     * Limpia el término de búsqueda
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }
    
    /**
     * Configura la lista completa de canciones (llamado desde MainActivity)
     * 
     * @param songs Lista completa de canciones
     */
    fun setAllSongs(songs: List<Song>) {
        _allSongs.value = songs
        // También configurar la playlist en el MediaController
        setPlaylist(songs)
    }
    
    // ========================================
    // EVENTOS DE USUARIO - CONTROLES DE REPRODUCCIÓN
    // ========================================
    
    /**
     * Alterna entre reproducir y pausar la canción actual
     */
    fun togglePlayPause() {
        viewModelScope.launch {
            mediaControllerManager.togglePlayPause()
        }
    }
    
    /**
     * Reproduce la canción actual (si está pausada)
     */
    fun play() {
        viewModelScope.launch {
            mediaControllerManager.play()
        }
    }
    
    /**
     * Pausa la reproducción actual
     */
    fun pause() {
        viewModelScope.launch {
            mediaControllerManager.pause()
        }
    }
    
    /**
     * Avanza a la siguiente canción en la playlist
     */
    fun skipToNext() {
        viewModelScope.launch {
            mediaControllerManager.seekToNext()
        }
    }
    
    /**
     * Retrocede a la canción anterior en la playlist
     */
    fun skipToPrevious() {
        viewModelScope.launch {
            mediaControllerManager.seekToPrevious()
        }
    }
    
    /**
     * Busca a una posición específica en la canción actual
     * 
     * @param positionMs Posición en milisegundos donde buscar
     */
    fun seekTo(positionMs: Long) {
        viewModelScope.launch {
            mediaControllerManager.seekTo(positionMs)
        }
    }
    
    // ========================================
    // EVENTOS DE USUARIO - GESTIÓN DE PLAYLIST
    // ========================================
    
    /**
     * Configura una nueva playlist y opcionalmente inicia la reproducción
     * 
     * @param songs Lista de canciones para la playlist
     * @param startIndex Índice de la canción inicial (por defecto 0)
     */
    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        viewModelScope.launch {
            mediaControllerManager.setPlaylist(songs, startIndex)
        }
    }
    
    /**
     * Reproduce una canción específica de la playlist actual
     * 
     * @param songIndex Índice de la canción en la playlist
     */
    fun playSongAtIndex(songIndex: Int) {
        viewModelScope.launch {
            mediaControllerManager.seekToIndex(songIndex)
        }
    }
    
    /**
     * Reproduce una canción específica buscándola en la playlist
     * Ahora busca en la lista original completa, no en la filtrada
     * 
     * @param song Canción a reproducir
     */
    fun playSong(song: Song) {
        viewModelScope.launch {
            val allSongsList = _allSongs.value
            val index = allSongsList.indexOfFirst { it.id == song.id }
            if (index != -1) {
                mediaControllerManager.seekToIndex(index)
            }
        }
    }
    
    // ========================================
    // MÉTODOS DE UTILIDAD
    // ========================================
    
    /**
     * Obtiene el progreso de reproducción como porcentaje (0.0 - 1.0)
     */
    fun getPlaybackProgress(): Float {
        val currentPos = position.value
        val totalDuration = duration.value
        return if (totalDuration > 0) {
            (currentPos.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    
    /**
     * Verifica si hay una canción siguiente disponible
     */
    fun hasNext(): Boolean {
        // Esta lógica podría expandirse para verificar el estado real de la playlist
        return playbackState.value != Player.STATE_ENDED
    }
    
    /**
     * Verifica si hay una canción anterior disponible
     */
    fun hasPrevious(): Boolean {
        // Esta lógica podría expandirse para verificar la posición en la playlist
        return currentSong.value != null
    }
    
    /**
     * Formatea el tiempo en milisegundos a formato MM:SS
     */
    fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    // ========================================
    // GESTIÓN DE RECURSOS
    // ========================================
    
    override fun onCleared() {
        super.onCleared()
        // El MediaControllerManager se libera en MainActivity.onDestroy()
        // No necesitamos hacer nada aquí ya que el ViewModel no posee el MediaController
    }
    
    // ========================================
    // FACTORY PARA INYECCIÓN DE DEPENDENCIAS
    // ========================================
    
    /**
     * Factory para crear instancias del MusicViewModel con dependencias
     */
    class Factory(
        private val mediaControllerManager: MediaControllerManager
    ) : ViewModelProvider.Factory {
        
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
                return MusicViewModel(mediaControllerManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

/**
 * Opciones de ordenamiento para la lista de canciones
 */
enum class SortOption(val displayName: String) {
    TITLE_ASC("Título A-Z"),
    TITLE_DESC("Título Z-A"),
    ARTIST_ASC("Artista A-Z"),
    ARTIST_DESC("Artista Z-A"),
    DURATION_ASC("Duración ↑"),
    DURATION_DESC("Duración ↓"),
    DEFAULT("Por Carpeta") // Ordenamiento original por carpeta
}

/**
 * Extensión para aplicar el ordenamiento a una lista de canciones
 */
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

/**
 * Estados de reproducción para facilitar el manejo en la UI
 */
object PlaybackStates {
    const val STATE_IDLE = Player.STATE_IDLE
    const val STATE_BUFFERING = Player.STATE_BUFFERING
    const val STATE_READY = Player.STATE_READY
    const val STATE_ENDED = Player.STATE_ENDED
}

/**
 * Extensiones útiles para el ViewModel en Compose
 */

/**
 * Verifica si el reproductor está en estado de carga
 */
val MusicViewModel.isBuffering: Boolean
    get() = playbackState.value == Player.STATE_BUFFERING

/**
 * Verifica si el reproductor está listo para reproducir
 */
val MusicViewModel.isReady: Boolean
    get() = playbackState.value == Player.STATE_READY

/**
 * Verifica si la reproducción ha terminado
 */
val MusicViewModel.isEnded: Boolean
    get() = playbackState.value == Player.STATE_ENDED

/**
 * Extensión para aplicar búsqueda a una lista de canciones
 * Busca en título, artista y nombre de carpeta
 */
private fun List<Song>.applySearch(query: String): List<Song> {
    if (query.isBlank()) return this
    
    val searchTerm = query.lowercase().trim()
    return filter { song ->
        song.title.lowercase().contains(searchTerm) ||
        song.artist.lowercase().contains(searchTerm) ||
        try {
            File(song.data).parentFile?.name?.lowercase()?.contains(searchTerm) == true
        } catch (e: Exception) {
            false
        }
    }
}
