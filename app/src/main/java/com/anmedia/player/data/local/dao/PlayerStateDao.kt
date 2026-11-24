package com.anmedia.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anmedia.player.data.local.entity.PlayerStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(state: PlayerStateEntity)

    @Query("SELECT * FROM player_state WHERE `key` = :key")
    suspend fun get(key: String): PlayerStateEntity?

    @Query("SELECT * FROM player_state WHERE `key` = :key")
    fun observe(key: String): Flow<PlayerStateEntity?>

    @Query("SELECT * FROM player_state")
    suspend fun getAll(): List<PlayerStateEntity>
}
