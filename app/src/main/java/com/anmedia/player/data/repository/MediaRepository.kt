package com.anmedia.player.data.repository

import com.anmedia.player.data.local.dao.MediaItemDao
import com.anmedia.player.data.local.entity.MediaItemEntity
import com.anmedia.player.data.mediastore.MediaStoreScanner
import com.anmedia.player.domain.model.SourceType
import kotlinx.coroutines.flow.Flow

class MediaRepository(
    private val mediaItemDao: MediaItemDao,
    private val mediaStoreScanner: MediaStoreScanner
) {
    fun observeLocalAudio(): Flow<List<MediaItemEntity>> = mediaItemDao.observeByType(SourceType.LOCAL_AUDIO)

    fun observeLocalVideo(): Flow<List<MediaItemEntity>> = mediaItemDao.observeByType(SourceType.LOCAL_VIDEO)

    fun observeYoutubeItems(): Flow<List<MediaItemEntity>> = mediaItemDao.observeYoutubeItems()

    suspend fun getById(id: Long): MediaItemEntity? = mediaItemDao.getById(id)

    suspend fun getBySourceUri(sourceUri: String): MediaItemEntity? = mediaItemDao.getBySourceUri(sourceUri)

    suspend fun recordPlay(id: Long) = mediaItemDao.incrementPlayCount(id, System.currentTimeMillis())

    suspend fun updateCachedStream(id: Long, url: String, expiresAtEpochMs: Long) =
        mediaItemDao.updateCachedStream(id, url, expiresAtEpochMs)

    suspend fun updateStreamMode(id: Long, mode: com.anmedia.player.domain.model.StreamMode) =
        mediaItemDao.updateStreamMode(id, mode)

    suspend fun rescanLocalLibrary() {
        mediaItemDao.clearLocalMedia()
        val scanned = mediaStoreScanner.scanAll()
        if (scanned.isNotEmpty()) {
            mediaItemDao.insertAll(scanned)
        }
    }

    suspend fun findOrCreateYoutubeItem(item: MediaItemEntity): Long {
        val existing = mediaItemDao.getBySourceUri(item.sourceUri)
        return if (existing != null) {
            mediaItemDao.update(item.copy(id = existing.id, playCount = existing.playCount, addedAt = existing.addedAt))
            existing.id
        } else {
            mediaItemDao.insert(item)
        }
    }

    suspend fun deleteItem(id: Long) = mediaItemDao.deleteById(id)
}
