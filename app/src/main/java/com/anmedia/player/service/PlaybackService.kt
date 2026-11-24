@file:OptIn(UnstableApi::class)

package com.anmedia.player.service

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.anmedia.player.AnMediaApp
import com.anmedia.player.R
import com.anmedia.player.player.AudioEffectsController
import com.anmedia.player.player.CustomRenderersFactory
import com.anmedia.player.player.PlaybackDiagnostics
import com.anmedia.player.player.ResilientLoadErrorHandlingPolicy
import com.anmedia.player.player.StereoBalanceController
import com.anmedia.player.player.YoutubeMediaSourceFactory
import com.anmedia.player.player.YoutubeResolvingDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pendingEqRestore: EqRestoreState? = null

    private data class EqRestoreState(
        val levels: List<Int>?,
        val preset: com.anmedia.player.domain.model.EqPreset,
        val enabled: Boolean,
        val preampMb: Int
    )

    override fun onCreate() {
        super.onCreate()
        val container = (application as AnMediaApp).container

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            // YouTube's CDN throttles sustained downloads of long videos (hours, not minutes) -
            // short clips finish before throttling kicks in, but a long stream can see its
            // read rate drop well below realtime for stretches. 20s was tight enough to trip a
            // SocketTimeoutException during those dips, forcing ExoPlayer to tear down and
            // reopen the connection (visible as stutter); this only needs to be generous enough
            // to tolerate a slow patch, not to mask a genuinely dead connection.
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("AnMedia/1.0 (Android)")

        val resolvingFactory = ResolvingDataSource.Factory(
            httpDataSourceFactory,
            YoutubeResolvingDataSource(container.youTubeStreamResolver, container.mediaRepository)
        )
        val dataSourceFactory = DefaultDataSource.Factory(this, resolvingFactory)
        val mediaSourceFactory = YoutubeMediaSourceFactory(
            delegate = DefaultMediaSourceFactory(this)
                .setDataSourceFactory(dataSourceFactory)
                .setLoadErrorHandlingPolicy(ResilientLoadErrorHandlingPolicy()),
            dataSourceFactory = dataSourceFactory,
            resolver = container.youTubeStreamResolver
        )

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // A bigger buffer cushion than the Media3 defaults (15s/50s) so a long YouTube stream
        // can ride out a throttled patch (download rate briefly below playback rate) without
        // visibly rebuffering - it just eats into the larger reserve built up while bandwidth
        // was good, instead of stalling. Matters much more for hours-long content than it does
        // for a 3-minute song, where the default buffer was already plenty.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 30_000,
                /* maxBufferMs = */ 180_000,
                /* bufferForPlaybackMs = */ 2_500,
                /* bufferForPlaybackAfterRebufferMs = */ 5_000
            )
            .build()

        val exoPlayer = ExoPlayer.Builder(this, CustomRenderersFactory(this))
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {
                AudioEffectsController.attach(audioSessionId)
                // One-shot: only the first session attach after process start should pull in the
                // persisted EQ snapshot. Previously this was never cleared, so every later session
                // change (i.e. practically every track) re-applied the stale startup preset/bands
                // and silently clobbered any preset/band the user picked in the Equalizer screen.
                pendingEqRestore?.let {
                    AudioEffectsController.restorePersistedState(it.levels, it.preset, it.enabled, it.preampMb)
                }
                pendingEqRestore = null
            }

            override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
                val stateName = when (state) {
                    androidx.media3.common.Player.STATE_IDLE -> "IDLE"
                    androidx.media3.common.Player.STATE_BUFFERING -> "BUFFERING"
                    androidx.media3.common.Player.STATE_READY -> "READY"
                    androidx.media3.common.Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN($state)"
                }
                PlaybackDiagnostics.log(
                    "state=$stateName pos=${eventTime.currentPlaybackPositionMs}ms buffered=${eventTime.totalBufferedDurationMs}ms"
                )
            }

            override fun onIsPlayingChanged(eventTime: AnalyticsListener.EventTime, isPlaying: Boolean) {
                PlaybackDiagnostics.log("isPlaying=$isPlaying pos=${eventTime.currentPlaybackPositionMs}ms")
            }

            override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: androidx.media3.common.PlaybackException) {
                PlaybackDiagnostics.log("PLAYER ERROR: ${error.errorCodeName} - ${error.message}")
            }

            override fun onLoadError(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: androidx.media3.exoplayer.source.LoadEventInfo,
                mediaLoadData: androidx.media3.exoplayer.source.MediaLoadData,
                error: java.io.IOException,
                wasCanceled: Boolean
            ) {
                PlaybackDiagnostics.log(
                    "LOAD ERROR (canceled=$wasCanceled) uri=${loadEventInfo.uri.host} bytesLoaded=${loadEventInfo.bytesLoaded} - ${error.javaClass.simpleName}: ${error.message}"
                )
            }

            override fun onBandwidthEstimate(
                eventTime: AnalyticsListener.EventTime,
                totalLoadTimeMs: Int,
                totalBytesLoaded: Long,
                bitrateEstimate: Long
            ) {
                PlaybackDiagnostics.log(
                    "bandwidth estimate=${bitrateEstimate / 1000}kbps over ${totalLoadTimeMs}ms (${totalBytesLoaded} bytes)"
                )
            }

            override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
                PlaybackDiagnostics.log("dropped $droppedFrames video frames over ${elapsedMs}ms (decoder/rendering falling behind)")
            }

            override fun onVideoInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: androidx.media3.common.Format,
                decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?
            ) {
                PlaybackDiagnostics.log(
                    "video format: ${format.width}x${format.height} ${format.codecs ?: format.sampleMimeType} bitrate=${format.bitrate / 1000}kbps fps=${format.frameRate}"
                )
            }

            override fun onAudioInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: androidx.media3.common.Format,
                decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?
            ) {
                PlaybackDiagnostics.log(
                    "audio format: ${format.codecs ?: format.sampleMimeType} bitrate=${format.bitrate / 1000}kbps sampleRate=${format.sampleRate}Hz"
                )
            }
        })

        player = exoPlayer

        mediaSession = MediaSession.Builder(this, exoPlayer).build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .setChannelName(R.string.notification_channel_name)
                .build()
        )

        restorePersistedPlayerState(exoPlayer, container)
    }

    private fun restorePersistedPlayerState(exoPlayer: ExoPlayer, container: com.anmedia.player.di.AppContainer) {
        serviceScope.launch(Dispatchers.IO) {
            val stateRepo = container.playerStateRepository
            val volume = stateRepo.loadVolume()
            val balance = stateRepo.loadBalance()
            val shuffle = stateRepo.loadShuffleMode()
            val repeat = stateRepo.loadRepeatMode()
            val eqLevels = stateRepo.loadEqBandLevels()
            val eqPreset = stateRepo.loadEqPreset()
            val eqEnabled = stateRepo.loadEqEnabled()
            val preamp = stateRepo.loadPreampLevel()

            pendingEqRestore = EqRestoreState(eqLevels, eqPreset, eqEnabled, preamp)

            launch(Dispatchers.Main.immediate) {
                exoPlayer.volume = volume
                StereoBalanceController.setBalance(balance)
                exoPlayer.shuffleModeEnabled = shuffle
                exoPlayer.repeatMode = when (repeat) {
                    com.anmedia.player.domain.model.RepeatMode.ONE -> androidx.media3.common.Player.REPEAT_MODE_ONE
                    com.anmedia.player.domain.model.RepeatMode.ALL -> androidx.media3.common.Player.REPEAT_MODE_ALL
                    com.anmedia.player.domain.model.RepeatMode.OFF -> androidx.media3.common.Player.REPEAT_MODE_OFF
                }
                // The audio session can attach before this IO load finishes, in which case
                // onAudioSessionIdChanged already found pendingEqRestore null and skipped it -
                // so also apply it here directly; restorePersistedState is a no-op if there's
                // no live Equalizer yet (attach() will pick up the now-updated eqState instead).
                AudioEffectsController.restorePersistedState(eqLevels, eqPreset, eqEnabled, preamp)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val session = mediaSession ?: return
        val currentPlayer = session.player
        if (!currentPlayer.playWhenReady || currentPlayer.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        AudioEffectsController.release()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "anmedia_playback_channel"
    }
}
