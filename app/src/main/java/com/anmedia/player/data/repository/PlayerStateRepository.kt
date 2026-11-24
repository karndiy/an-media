package com.anmedia.player.data.repository

import com.anmedia.player.data.local.dao.PlayerStateDao
import com.anmedia.player.data.local.entity.PlayerStateEntity
import com.anmedia.player.data.local.entity.PlayerStateKeys
import com.anmedia.player.domain.model.EqPreset
import com.anmedia.player.domain.model.RepeatMode
import com.anmedia.player.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlayerStateRepository(private val dao: PlayerStateDao) {

    suspend fun saveEqBandLevels(levels: List<Int>) =
        dao.set(PlayerStateEntity(PlayerStateKeys.EQ_BAND_LEVELS, levels.joinToString(",")))

    suspend fun loadEqBandLevels(): List<Int>? =
        dao.get(PlayerStateKeys.EQ_BAND_LEVELS)?.value
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.takeIf { it.isNotEmpty() }

    suspend fun saveEqPreset(preset: EqPreset) =
        dao.set(PlayerStateEntity(PlayerStateKeys.EQ_PRESET, preset.name))

    suspend fun loadEqPreset(): EqPreset =
        dao.get(PlayerStateKeys.EQ_PRESET)?.value?.let {
            runCatching { EqPreset.valueOf(it) }.getOrNull()
        } ?: EqPreset.FLAT

    suspend fun saveEqEnabled(enabled: Boolean) =
        dao.set(PlayerStateEntity(PlayerStateKeys.EQ_ENABLED, enabled.toString()))

    suspend fun loadEqEnabled(): Boolean =
        dao.get(PlayerStateKeys.EQ_ENABLED)?.value?.toBooleanStrictOrNull() ?: true

    suspend fun savePreampLevel(level: Int) =
        dao.set(PlayerStateEntity(PlayerStateKeys.PREAMP_LEVEL, level.toString()))

    suspend fun loadPreampLevel(): Int =
        dao.get(PlayerStateKeys.PREAMP_LEVEL)?.value?.toIntOrNull() ?: 0

    suspend fun saveShuffleMode(enabled: Boolean) =
        dao.set(PlayerStateEntity(PlayerStateKeys.SHUFFLE_MODE, enabled.toString()))

    suspend fun loadShuffleMode(): Boolean =
        dao.get(PlayerStateKeys.SHUFFLE_MODE)?.value?.toBooleanStrictOrNull() ?: false

    suspend fun saveRepeatMode(mode: RepeatMode) =
        dao.set(PlayerStateEntity(PlayerStateKeys.REPEAT_MODE, mode.name))

    suspend fun loadRepeatMode(): RepeatMode =
        dao.get(PlayerStateKeys.REPEAT_MODE)?.value?.let {
            runCatching { RepeatMode.valueOf(it) }.getOrNull()
        } ?: RepeatMode.OFF

    suspend fun saveLastTrack(mediaId: Long, positionMs: Long) {
        dao.set(PlayerStateEntity(PlayerStateKeys.LAST_MEDIA_ID, mediaId.toString()))
        dao.set(PlayerStateEntity(PlayerStateKeys.LAST_POSITION_MS, positionMs.toString()))
    }

    suspend fun loadLastMediaId(): Long? = dao.get(PlayerStateKeys.LAST_MEDIA_ID)?.value?.toLongOrNull()

    suspend fun loadLastPositionMs(): Long = dao.get(PlayerStateKeys.LAST_POSITION_MS)?.value?.toLongOrNull() ?: 0L

    suspend fun saveVolume(volume: Float) = dao.set(PlayerStateEntity(PlayerStateKeys.VOLUME, volume.toString()))

    suspend fun loadVolume(): Float = dao.get(PlayerStateKeys.VOLUME)?.value?.toFloatOrNull() ?: 1f

    suspend fun saveBalance(balance: Float) = dao.set(PlayerStateEntity(PlayerStateKeys.BALANCE, balance.toString()))

    suspend fun loadBalance(): Float = dao.get(PlayerStateKeys.BALANCE)?.value?.toFloatOrNull() ?: 0f

    suspend fun saveThemeMode(mode: ThemeMode) = dao.set(PlayerStateEntity(PlayerStateKeys.THEME_MODE, mode.name))

    fun observeThemeMode(): Flow<ThemeMode> = dao.observe(PlayerStateKeys.THEME_MODE).map { it.toThemeMode() }

    private fun PlayerStateEntity?.toThemeMode(): ThemeMode =
        this?.value?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
}
