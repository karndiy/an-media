package com.anmedia.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.anmedia.player.data.local.entity.MediaItemEntity
import com.anmedia.player.data.local.entity.PlaylistCrossRefEntity
import com.anmedia.player.data.local.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String)

    @Query("SELECT * FROM playlists WHERE is_youtube_queue = 0 ORDER BY created_at DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE is_youtube_queue = 1 LIMIT 1")
    suspend fun getYoutubeQueuePlaylist(): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: PlaylistCrossRefEntity)

    @Query("DELETE FROM playlist_cross_ref WHERE playlist_id = :playlistId AND media_id = :mediaId")
    suspend fun removeFromPlaylist(playlistId: Long, mediaId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_cross_ref WHERE playlist_id = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    @Query("UPDATE playlist_cross_ref SET position = :position WHERE playlist_id = :playlistId AND media_id = :mediaId")
    suspend fun updatePosition(playlistId: Long, mediaId: Long, position: Int)

    @Transaction
    suspend fun reorder(playlistId: Long, orderedMediaIds: List<Long>) {
        orderedMediaIds.forEachIndexed { index, mediaId ->
            updatePosition(playlistId, mediaId, index)
        }
    }

    @Query(
        """
        SELECT media_items.* FROM media_items
        INNER JOIN playlist_cross_ref ON media_items.id = playlist_cross_ref.media_id
        WHERE playlist_cross_ref.playlist_id = :playlistId
        ORDER BY playlist_cross_ref.position ASC
        """
    )
    fun observePlaylistItems(playlistId: Long): Flow<List<MediaItemEntity>>

    @Query(
        """
        SELECT media_items.* FROM media_items
        INNER JOIN playlist_cross_ref ON media_items.id = playlist_cross_ref.media_id
        WHERE playlist_cross_ref.playlist_id = :playlistId
        ORDER BY playlist_cross_ref.position ASC
        """
    )
    suspend fun getPlaylistItemsOnce(playlistId: Long): List<MediaItemEntity>
}
