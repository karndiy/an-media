package com.anmedia.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.anmedia.player.data.local.entity.MediaItemEntity
import com.anmedia.player.domain.model.SourceType
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MediaItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaItemEntity>): List<Long>

    @Update
    suspend fun update(item: MediaItemEntity)

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getById(id: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE source_uri = :sourceUri LIMIT 1")
    suspend fun getBySourceUri(sourceUri: String): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE source_type = :type ORDER BY title COLLATE NOCASE ASC")
    fun observeByType(type: SourceType): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE source_type = 'YOUTUBE' ORDER BY added_at DESC")
    fun observeYoutubeItems(): Flow<List<MediaItemEntity>>

    @Query("UPDATE media_items SET play_count = play_count + 1, last_played_at = :timestamp WHERE id = :id")
    suspend fun incrementPlayCount(id: Long, timestamp: Long)

    @Query("UPDATE media_items SET cached_stream_url = :url, stream_url_expires_at = :expiresAt WHERE id = :id")
    suspend fun updateCachedStream(id: Long, url: String, expiresAt: Long)

    @Query("UPDATE media_items SET preferred_stream_mode = :mode WHERE id = :id")
    suspend fun updateStreamMode(id: Long, mode: com.anmedia.player.domain.model.StreamMode)

    @Query("DELETE FROM media_items WHERE source_type = 'LOCAL_AUDIO' OR source_type = 'LOCAL_VIDEO'")
    suspend fun clearLocalMedia()

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}
