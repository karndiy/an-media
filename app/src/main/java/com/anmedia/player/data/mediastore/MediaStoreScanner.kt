package com.anmedia.player.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.anmedia.player.data.local.entity.MediaItemEntity
import com.anmedia.player.domain.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreScanner(private val context: Context) {

    suspend fun scanAll(): List<MediaItemEntity> = withContext(Dispatchers.IO) {
        scanAudio() + scanVideo()
    }

    suspend fun scanAudio(): List<MediaItemEntity> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItemEntity>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.DURATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.BITRATE)
            }
        }.toTypedArray()
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val bitrateCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
            } else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                items += MediaItemEntity(
                    title = cursor.getString(titleCol) ?: "Unknown",
                    artist = cursor.getString(artistCol),
                    album = cursor.getString(albumCol),
                    durationMs = cursor.getLong(durationCol),
                    sourceUri = uri.toString(),
                    sourceType = SourceType.LOCAL_AUDIO,
                    bitrate = if (bitrateCol >= 0) cursor.getInt(bitrateCol) / 1000 else 0
                )
            }
        }
        items
    }

    suspend fun scanVideo(): List<MediaItemEntity> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItemEntity>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.ARTIST
        )

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                items += MediaItemEntity(
                    title = cursor.getString(titleCol) ?: "Unknown",
                    artist = cursor.getString(artistCol),
                    durationMs = cursor.getLong(durationCol),
                    sourceUri = uri.toString(),
                    sourceType = SourceType.LOCAL_VIDEO
                )
            }
        }
        items
    }
}
