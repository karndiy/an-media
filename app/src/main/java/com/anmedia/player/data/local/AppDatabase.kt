package com.anmedia.player.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.anmedia.player.data.local.dao.MediaItemDao
import com.anmedia.player.data.local.dao.PlayerStateDao
import com.anmedia.player.data.local.dao.PlaylistDao
import com.anmedia.player.data.local.entity.MediaItemEntity
import com.anmedia.player.data.local.entity.PlayerStateEntity
import com.anmedia.player.data.local.entity.PlaylistCrossRefEntity
import com.anmedia.player.data.local.entity.PlaylistEntity

@Database(
    entities = [
        MediaItemEntity::class,
        PlaylistEntity::class,
        PlaylistCrossRefEntity::class,
        PlayerStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mediaItemDao(): MediaItemDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playerStateDao(): PlayerStateDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "an_media.db"
                ).build().also { instance = it }
            }
        }
    }
}
