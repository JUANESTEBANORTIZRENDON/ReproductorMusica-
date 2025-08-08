package com.juan.reproductormusica.repository

import com.juan.reproductormusica.data.Song
import com.juan.reproductormusica.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repositorio para operaciones de playlists
 */
class PlaylistRepository(val playlistDao: PlaylistDao) {
    
    // Playlist especial de favoritos (ID = 1)
    private val FAVORITES_PLAYLIST_ID = 1L
    private val FAVORITES_PLAYLIST_NAME = "Favoritos"
    
    val playlists: Flow<List<PlaylistEntity>> = playlistDao.getPlaylists()
    val playlistsWithSongs: Flow<List<PlaylistWithSongs>> = playlistDao.getPlaylistsWithSongs()
    
    suspend fun initializeFavoritesPlaylist() {
        // Crear playlist de favoritos si no existe
        val existingPlaylists = playlistDao.getPlaylists().first()
        val favoritesExists = existingPlaylists.any { it.playlistId == FAVORITES_PLAYLIST_ID }
        
        if (!favoritesExists) {
            val favoritesPlaylist = PlaylistEntity(
                playlistId = FAVORITES_PLAYLIST_ID,
                name = FAVORITES_PLAYLIST_NAME
            )
            playlistDao.insertPlaylist(favoritesPlaylist)
        }
    }
    
    suspend fun createPlaylist(name: String): Long {
        val playlist = PlaylistEntity(name = name)
        return playlistDao.insertPlaylist(playlist)
    }
    
    suspend fun deletePlaylist(playlistId: Long) {
        // No permitir eliminar la playlist de favoritos
        if (playlistId != FAVORITES_PLAYLIST_ID) {
            playlistDao.deletePlaylistById(playlistId)
        }
    }
    
    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        // No permitir renombrar la playlist de favoritos
        if (playlistId != FAVORITES_PLAYLIST_ID) {
            val playlists = playlistDao.getPlaylists().first()
            val playlist = playlists.find { it.playlistId == playlistId }
            playlist?.let {
                val updatedPlaylist = it.copy(name = newName)
                playlistDao.updatePlaylist(updatedPlaylist)
            }
        }
    }
    
    suspend fun addSongToPlaylist(song: Song, playlistId: Long) {
        // Primero insertar la canción en la tabla songs (si no existe)
        val songEntity = SongEntity(
            songId = song.id,
            title = song.title,
            artist = song.artist,
            path = song.data,
            duration = song.duration
        )
        playlistDao.insertSong(songEntity)
        
        // Luego crear la relación
        val crossRef = PlaylistSongCrossRef(
            playlistId = playlistId,
            songId = song.id
        )
        playlistDao.insertPlaylistSongCrossRef(crossRef)
    }
    
    suspend fun removeSongFromPlaylist(songId: Long, playlistId: Long) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }
    
    suspend fun toggleFavorite(song: Song) {
        val isFavorite = playlistDao.isSongInFavorites(FAVORITES_PLAYLIST_ID, song.id) > 0
        
        if (isFavorite) {
            // Quitar de favoritos
            playlistDao.removeSongFromPlaylist(FAVORITES_PLAYLIST_ID, song.id)
        } else {
            // Añadir a favoritos
            addSongToPlaylist(song, FAVORITES_PLAYLIST_ID)
        }
    }
    
    suspend fun isSongFavorite(songId: Long): Boolean {
        return playlistDao.isSongInFavorites(FAVORITES_PLAYLIST_ID, songId) > 0
    }
    
    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs?> {
        return playlistDao.getPlaylistWithSongs(playlistId)
    }
    
    fun getSongsInPlaylist(playlistId: Long): Flow<List<SongEntity>> {
        return playlistDao.getSongsInPlaylist(playlistId)
    }
    
    // Función para obtener Set de IDs de canciones favoritas (para el ViewModel)
    fun getFavoriteSongIds(): Flow<Set<Long>> {
        return playlistDao.getSongsInPlaylist(FAVORITES_PLAYLIST_ID).map { songs ->
            songs.map { it.songId }.toSet()
        }
    }
    
    // Convertir SongEntity a Song para compatibilidad
    fun SongEntity.toSong(): Song {
        return Song(
            id = songId,
            title = title,
            artist = artist,
            data = path,
            duration = duration
        )
    }
}
