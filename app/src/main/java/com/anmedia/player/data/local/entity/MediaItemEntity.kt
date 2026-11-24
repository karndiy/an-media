package com.anmedia.player.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anmedia.player.domain.model.SourceType
import com.anmedia.player.domain.model.StreamMode

@Entity(
    tableName = "media_items",
    indices = [Index(value = ["source_uri"], unique = true)]
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    @androidx.room.ColumnInfo(name = "duration_ms")
    val durationMs: Long = 0,
    @androidx.room.ColumnInfo(name = "source_uri")
    val sourceUri: String,
    @androidx.room.ColumnInfo(name = "source_type")
    val sourceType: SourceType,
    @androidx.room.ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String? = null,
    @androidx.room.ColumnInfo(name = "channel_name")
    val channelName: String? = null,
    @androidx.room.ColumnInfo(name = "bitrate")
    val bitrate: Int = 0,
    @androidx.room.ColumnInfo(name = "sample_rate")
    val sampleRate: Int = 0,
    @androidx.room.ColumnInfo(name = "play_count")
    val playCount: Int = 0,
    @androidx.room.ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis(),
    @androidx.room.ColumnInfo(name = "last_played_at")
    val lastPlayedAt: Long? = null,
    @androidx.room.ColumnInfo(name = "cached_stream_url")
    val cachedStreamUrl: String? = null,
    @androidx.room.ColumnInfo(name = "stream_url_expires_at")
    val streamUrlExpiresAt: Long? = null,
    @androidx.room.ColumnInfo(name = "preferred_stream_mode", defaultValue = "AUDIO_ONLY")
    val preferredStreamMode: StreamMode = StreamMode.AUDIO_ONLY
)
