@file:OptIn(UnstableApi::class)

package com.anmedia.player.player

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.anmedia.player.data.local.entity.MediaItemEntity
import com.anmedia.player.data.repository.MediaRepository
import com.anmedia.player.data.repository.PlayerStateRepository
import com.anmedia.player.domain.model.RepeatMode
import com.anmedia.player.service.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerManager(
    private val context: Context,
    private val mediaRepository: MediaRepository,
    private val playerStateRepository: PlayerStateRepository,
    private val externalScope: CoroutineScope
) {
    private var controller: MediaController? = null
    private var positionTickerJob: Job? = null
    private var lastPersistTick = 0L
    private var currentQueue: List<MediaItemEntity> = emptyList()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    private val _queue = MutableStateFlow<List<MediaItemEntity>>(emptyList())
    val queue: StateFlow<List<MediaItemEntity>> = _queue

    private val _playerFlow = MutableStateFlow<Player?>(null)
    val playerFlow: StateFlow<Player?> = _playerFlow

    init {
        externalScope.launch { connect() }
    }

    private suspend fun connect() {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val mediaController = MediaController.Builder(context, token).buildAsync().await()
        controller = mediaController
        _playerFlow.value = mediaController
        mediaController.addListener(playerListener)

        externalScope.launch {
            val volume = playerStateRepository.loadVolume()
            val balance = playerStateRepository.loadBalance()
            val shuffle = playerStateRepository.loadShuffleMode()
            val repeat = playerStateRepository.loadRepeatMode()
            mediaController.volume = volume
            StereoBalanceController.setBalance(balance)
            mediaController.shuffleModeEnabled = shuffle
            mediaController.repeatMode = repeat.toPlayerConstant()

            _uiState.value = _uiState.value.copy(
                connected = true,
                volume = volume,
                balance = balance,
                shuffleEnabled = shuffle,
                repeatMode = repeat
            )

            if (mediaController.mediaItemCount == 0) {
                restoreLastSession(mediaController)
            }
        }

        startPositionTicker()
    }

    private suspend fun restoreLastSession(mediaController: MediaController) {
        val lastId = playerStateRepository.loadLastMediaId() ?: return
        val entity = mediaRepository.getById(lastId) ?: return
        val positionMs = playerStateRepository.loadLastPositionMs()
        setCurrentQueue(listOf(entity))
        mediaController.setMediaItem(QueueBuilder.build(entity), positionMs)
        mediaController.prepare()
    }

    fun playQueue(entities: List<MediaItemEntity>, startIndex: Int = 0, playWhenReady: Boolean = true) {
        val mediaController = controller ?: return
        if (entities.isEmpty()) return
        setCurrentQueue(entities)
        val items = QueueBuilder.buildAll(entities)
        mediaController.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
        mediaController.prepare()
        mediaController.playWhenReady = playWhenReady
        externalScope.launch { mediaRepository.recordPlay(entities[startIndex.coerceIn(0, entities.lastIndex)].id) }
    }

    fun appendToQueue(entities: List<MediaItemEntity>) {
        val mediaController = controller ?: return
        setCurrentQueue(currentQueue + entities)
        mediaController.addMediaItems(QueueBuilder.buildAll(entities))
    }

    fun removeFromQueue(index: Int) {
        val mediaController = controller ?: return
        if (index !in currentQueue.indices) return
        mediaController.removeMediaItem(index)
        setCurrentQueue(currentQueue.toMutableList().apply { removeAt(index) })
    }

    fun playQueueIndex(index: Int) {
        val mediaController = controller ?: return
        if (index !in currentQueue.indices) return
        mediaController.seekTo(index, 0L)
        mediaController.playWhenReady = true
    }

    private fun setCurrentQueue(entities: List<MediaItemEntity>) {
        currentQueue = entities
        _queue.value = entities
    }

    fun playPause() {
        val mediaController = controller ?: return
        if (mediaController.playbackState == Player.STATE_IDLE) {
            mediaController.prepare()
        }
        mediaController.playWhenReady = !mediaController.playWhenReady
    }

    fun stop() {
        controller?.stop()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _uiState.value = _uiState.value.copy(positionMs = positionMs)
    }

    fun seekToQueueIndex(index: Int) {
        controller?.seekTo(index, 0L)
    }

    fun skipToNext() {
        controller?.seekToNext()
    }

    fun skipToPrevious() {
        val mediaController = controller ?: return
        if (mediaController.currentPosition > 3000) {
            mediaController.seekTo(0)
        } else {
            mediaController.seekToPrevious()
        }
    }

    fun toggleShuffle() {
        val mediaController = controller ?: return
        val newValue = !mediaController.shuffleModeEnabled
        mediaController.shuffleModeEnabled = newValue
        _uiState.value = _uiState.value.copy(shuffleEnabled = newValue)
        externalScope.launch { playerStateRepository.saveShuffleMode(newValue) }
    }

    fun cycleRepeatMode() {
        val mediaController = controller ?: return
        val next = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        mediaController.repeatMode = next.toPlayerConstant()
        _uiState.value = _uiState.value.copy(repeatMode = next)
        externalScope.launch { playerStateRepository.saveRepeatMode(next) }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        controller?.volume = clamped
        _uiState.value = _uiState.value.copy(volume = clamped)
        externalScope.launch { playerStateRepository.saveVolume(clamped) }
    }

    fun setBalance(balance: Float) {
        val clamped = balance.coerceIn(-1f, 1f)
        StereoBalanceController.setBalance(clamped)
        _uiState.value = _uiState.value.copy(balance = clamped)
        externalScope.launch { playerStateRepository.saveBalance(clamped) }
    }

    fun currentQueueEntities(): List<MediaItemEntity> = currentQueue

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = externalScope.launch {
            while (isActive) {
                val mediaController = controller
                if (mediaController != null) {
                    _uiState.value = _uiState.value.copy(
                        positionMs = mediaController.currentPosition.coerceAtLeast(0),
                        durationMs = mediaController.duration.coerceAtLeast(0),
                        bufferedPositionMs = mediaController.bufferedPosition.coerceAtLeast(0),
                        currentIndex = mediaController.currentMediaItemIndex,
                        queueSize = mediaController.mediaItemCount
                    )
                    maybePersistProgress(mediaController)
                }
                delay(500)
            }
        }
    }

    private fun maybePersistProgress(mediaController: MediaController) {
        val now = System.currentTimeMillis()
        if (!mediaController.isPlaying || now - lastPersistTick < 5000) return
        lastPersistTick = now
        val dbId = mediaController.currentMediaItem?.let { QueueBuilder.dbIdOf(it) } ?: return
        externalScope.launch {
            playerStateRepository.saveLastTrack(dbId, mediaController.currentPosition)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            if (!isPlaying) {
                val mediaController = controller ?: return
                val dbId = mediaController.currentMediaItem?.let { QueueBuilder.dbIdOf(it) }
                if (dbId != null) {
                    externalScope.launch {
                        playerStateRepository.saveLastTrack(dbId, mediaController.currentPosition)
                    }
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.value = _uiState.value.copy(isBuffering = playbackState == Player.STATE_BUFFERING)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (mediaItem == null) return
            val dbId = QueueBuilder.dbIdOf(mediaItem)
            _uiState.value = _uiState.value.copy(
                currentMediaDbId = dbId,
                title = mediaItem.mediaMetadata.title?.toString() ?: "",
                artist = mediaItem.mediaMetadata.artist?.toString() ?: "",
                artworkUri = mediaItem.mediaMetadata.artworkUri?.toString(),
                sourceType = QueueBuilder.sourceTypeOf(mediaItem),
                positionMs = 0,
                errorMessage = null
            )
            dbId?.let { id -> externalScope.launch { mediaRepository.recordPlay(id) } }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.value = _uiState.value.copy(shuffleEnabled = shuffleModeEnabled)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            val mapped = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            }
            _uiState.value = _uiState.value.copy(repeatMode = mapped)
        }

        override fun onVolumeChanged(volume: Float) {
            _uiState.value = _uiState.value.copy(volume = volume)
        }

        override fun onTracksChanged(tracks: Tracks) {
            var bitrate = 0
            var sampleRate = 0
            var hasVideo = false
            for (group in tracks.groups) {
                if (group.type == C.TRACK_TYPE_VIDEO && group.isSelected) hasVideo = true
                if (group.type == C.TRACK_TYPE_AUDIO) {
                    for (i in 0 until group.length) {
                        if (group.isTrackSelected(i)) {
                            val format = group.getTrackFormat(i)
                            if (format.bitrate > 0) bitrate = format.bitrate / 1000
                            if (format.sampleRate > 0) sampleRate = format.sampleRate
                        }
                    }
                }
            }
            _uiState.value = _uiState.value.copy(
                bitrateKbps = bitrate,
                sampleRateHz = sampleRate,
                hasVideo = hasVideo
            )
        }

        override fun onPlayerError(error: PlaybackException) {
            _uiState.value = _uiState.value.copy(errorMessage = error.message)
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            val mediaController = controller ?: return
            _uiState.value = _uiState.value.copy(
                queueSize = mediaController.mediaItemCount,
                currentIndex = mediaController.currentMediaItemIndex
            )
        }
    }

    private fun RepeatMode.toPlayerConstant(): Int = when (this) {
        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
    }
}
