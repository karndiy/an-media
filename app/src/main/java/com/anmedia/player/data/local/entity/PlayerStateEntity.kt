package com.anmedia.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_state")
data class PlayerStateEntity(
    @PrimaryKey
    val key: String,
    val value: String
)

object PlayerStateKeys {
    const val EQ_BAND_LEVELS = "eq_band_levels"
    const val EQ_PRESET = "eq_preset"
    const val EQ_ENABLED = "eq_enabled"
    const val PREAMP_LEVEL = "preamp_level"
    const val SHUFFLE_MODE = "shuffle_mode"
    const val REPEAT_MODE = "repeat_mode"
    const val LAST_MEDIA_ID = "last_media_id"
    const val LAST_POSITION_MS = "last_position_ms"
    const val VOLUME = "volume"
    const val BALANCE = "balance"
    const val THEME_MODE = "theme_mode"
}
