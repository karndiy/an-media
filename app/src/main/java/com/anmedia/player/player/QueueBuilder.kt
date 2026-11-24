package com.anmedia.player.player

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.anmedia.player.data.local.entity.MediaItemEntity
import com.anmedia.player.domain.model.SourceType

/**
 * Maps a persisted [MediaItemEntity] to a Media3 [MediaItem]. For YouTube items the URI is
 * the original watch URL; [YoutubeResolvingDataSource] resolves it to a playable CDN URL
 * lazily on the loader thread, re-resolving automatically once the cached stream expires.
 */
object QueueBuilder {

    const val EXTRA_MEDIA_DB_ID = "media_db_id"
    const val EXTRA_SOURCE_TYPE = "source_type"
    const val EXTRA_BITRATE_KBPS = "bitrate_kbps"

    fun build(entity: MediaItemEntity): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(entity.title)
            .setArtist(entity.channelName ?: entity.artist)
            .setAlbumTitle(entity.album)
            .setArtworkUri(entity.thumbnailUrl?.let { Uri.parse(it) })
            .setExtras(
                Bundle().apply {
                    putLong(EXTRA_MEDIA_DB_ID, entity.id)
                    putString(EXTRA_SOURCE_TYPE, entity.sourceType.name)
                    putInt(EXTRA_BITRATE_KBPS, entity.bitrate)
                }
            )
            .build()

        return MediaItem.Builder()
            .setMediaId(entity.id.toString())
            .setUri(entity.sourceUri)
            .setMediaMetadata(metadata)
            .build()
    }

    fun buildAll(entities: List<MediaItemEntity>): List<MediaItem> = entities.map(::build)

    fun dbIdOf(mediaItem: MediaItem): Long? =
        mediaItem.mediaId.toLongOrNull() ?: mediaItem.mediaMetadata.extras?.getLong(EXTRA_MEDIA_DB_ID)

    fun sourceTypeOf(mediaItem: MediaItem): SourceType? =
        mediaItem.mediaMetadata.extras?.getString(EXTRA_SOURCE_TYPE)?.let {
            runCatching { SourceType.valueOf(it) }.getOrNull()
        }
}
