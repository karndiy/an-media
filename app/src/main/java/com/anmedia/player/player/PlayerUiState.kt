package com.anmedia.player.player

import com.anmedia.player.domain.model.RepeatMode
import com.anmedia.player.domain.model.SourceType

data class PlayerUiState(
    val connected: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentMediaDbId: Long? = null,
    val title: String = "",
    val artist: String = "",
    val artworkUri: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val bufferedPositionMs: Long = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val volume: Float = 1f,
    val balance: Float = 0f,
    val bitrateKbps: Int = 0,
    val sampleRateHz: Int = 0,
    val sourceType: SourceType? = null,
    val hasVideo: Boolean = false,
    val queueSize: Int = 0,
    val currentIndex: Int = 0,
    val errorMessage: String? = null
)
