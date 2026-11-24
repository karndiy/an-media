package com.anmedia.player.data.repository

import com.anmedia.player.data.local.dao.MediaItemDao
import com.anmedia.player.data.local.dao.PlaylistDao
import com.anmedia.player.data.local.entity.MediaItemEntity
import com.anmedia.player.data.local.entity.PlaylistCrossRefEntity
import com.anmedia.player.data.local.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

class PlaylistRepository(
    private val playlistDao: PlaylistDao,
    private val mediaItemDao: MediaItemDao
) {
    fun observePlaylists(): Flow<List<PlaylistEntity>> = playlistDao.observePlaylists()

    fun observePlaylistItems(playlistId: Long): Flow<List<MediaItemEntity>> =
        playlistDao.observePlaylistItems(playlistId)

    suspend fun createPlaylist(name: String): Long = playlistDao.insertPlaylist(PlaylistEntity(name = name))

    suspend fun deletePlaylist(playlistId: Long) = playlistDao.deletePlaylist(playlistId)

    suspend fun renamePlaylist(playlistId: Long, name: String) = playlistDao.renamePlaylist(playlistId, name)

    suspend fun addToPlaylist(playlistId: Long, mediaId: Long) {
        val position = playlistDao.nextPosition(playlistId)
        playlistDao.insertCrossRef(PlaylistCrossRefEntity(playlistId, mediaId, position))
    }

    suspend fun removeFromPlaylist(playlistId: Long, mediaId: Long) =
        playlistDao.removeFromPlaylist(playlistId, mediaId)

    suspend fun reorder(playlistId: Long, orderedMediaIds: List<Long>) =
        playlistDao.reorder(playlistId, orderedMediaIds)

    suspend fun getOrCreateYoutubeQueue(): Long {
        val existing = playlistDao.getYoutubeQueuePlaylist()
        if (existing != null) return existing.id
        return playlistDao.insertPlaylist(PlaylistEntity(name = "YouTube Queue", isYoutubeQueue = true))
    }

    suspend fun getPlaylistItemsOnce(playlistId: Long): List<MediaItemEntity> =
        playlistDao.getPlaylistItemsOnce(playlistId)
}
