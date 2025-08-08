package com.juan.reproductormusica.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones de playlists y canciones
 */
@Dao
interface PlaylistDao {
    
    // PLAYLISTS
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getPlaylists(): Flow<List<PlaylistEntity>>
    
    @Transaction
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getPlaylistsWithSongs(): Flow<List<PlaylistWithSongs>>
    
    @Transaction
    @Query("SELECT * FROM playlists WHERE playlistId = :playlistId")
    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs?>
    
    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long
    
    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)
    
    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)
    
    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)
    
    // SONGS
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSong(song: SongEntity)
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongs(songs: List<SongEntity>)
    
    // PLAYLIST-SONG RELATIONS
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSongCrossRef(crossRef: PlaylistSongCrossRef)
    
    @Delete
    suspend fun deletePlaylistSongCrossRef(crossRef: PlaylistSongCrossRef)
    
    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)
    
    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun removeAllSongsFromPlaylist(playlistId: Long)
    
    // FAVORITES QUERIES
    @Query("SELECT COUNT(*) FROM playlist_song_cross_ref WHERE playlistId = :favoritesPlaylistId AND songId = :songId")
    suspend fun isSongInFavorites(favoritesPlaylistId: Long, songId: Long): Int
    
    @Transaction
    @Query("""
        SELECT s.* FROM songs s 
        INNER JOIN playlist_song_cross_ref ps ON s.songId = ps.songId 
        WHERE ps.playlistId = :playlistId 
        ORDER BY ps.addedAt DESC
    """)
    fun getSongsInPlaylist(playlistId: Long): Flow<List<SongEntity>>
}
