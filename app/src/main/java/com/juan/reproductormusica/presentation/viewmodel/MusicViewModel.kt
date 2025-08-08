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
    // GESTIÓN DE RECURSOS
    // ========================================

    override fun onCleared() {
        super.onCleared()
        // El MediaControllerManager se libera en MainActivity.onDestroy()
    }

    // ========================================
    // FACTORY PARA INYECCIÓN DE DEPENDENCIAS
    // ========================================

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
