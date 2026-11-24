package com.anmedia.player.ui.screens.player

import androidx.lifecycle.ViewModel
import com.anmedia.player.data.local.entity.MediaItemEntity
import com.anmedia.player.data.repository.MediaRepository
import com.anmedia.player.player.PlayerManager
import com.anmedia.player.player.PlayerUiState
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel(
    private val playerManager: PlayerManager,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    val uiState: StateFlow<PlayerUiState> = playerManager.uiState
    val playerFlow: StateFlow<androidx.media3.common.Player?> = playerManager.playerFlow
    val queue: StateFlow<List<MediaItemEntity>> = playerManager.queue

    fun playQueue(entities: List<MediaItemEntity>, startIndex: Int = 0) =
        playerManager.playQueue(entities, startIndex)

    fun appendToQueue(entities: List<MediaItemEntity>) = playerManager.appendToQueue(entities)

    fun removeFromQueue(index: Int) = playerManager.removeFromQueue(index)

    fun playQueueIndex(index: Int) = playerManager.playQueueIndex(index)

    fun playPause() = playerManager.playPause()

    fun stop() = playerManager.stop()

    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)

    fun skipToNext() = playerManager.skipToNext()

    fun skipToPrevious() = playerManager.skipToPrevious()

    fun toggleShuffle() = playerManager.toggleShuffle()

    fun cycleRepeatMode() = playerManager.cycleRepeatMode()

    fun setVolume(volume: Float) = playerManager.setVolume(volume)

    fun setBalance(balance: Float) = playerManager.setBalance(balance)

    fun dismissError() = playerManager.dismissError()
}
