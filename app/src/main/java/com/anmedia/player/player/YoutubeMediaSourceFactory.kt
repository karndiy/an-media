@file:OptIn(UnstableApi::class)

package com.anmedia.player.player

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import com.anmedia.player.domain.youtube.YouTubeStreamResolver
import com.anmedia.player.domain.youtube.YoutubeUrlUtils

/**
 * Wraps the app's normal [MediaSource.Factory] to handle the common case where YouTube has no
 * muxed (audio+video combined) stream for a video - only a video-only DASH stream plus a
 * separate audio-only stream. [YoutubeResolvingDataSource] can only swap in a single resolved
 * URI, so it can't represent "two streams that need to play in sync"; that decision has to be
 * made here, at [MediaSource] construction time, instead.
 *
 * Looks up [YouTubeStreamResolver.peekCached] - a synchronous, zero-I/O read of the in-memory
 * resolution cache already populated when the URL was added - rather than resolving over the
 * network here, since this runs on the player's application thread (the main thread, in this
 * app) and a blocking network call here would risk an ANR. A cache miss (expired or never
 * resolved for video mode) just falls through to the existing single-URI behavior.
 */
class YoutubeMediaSourceFactory(
    private val delegate: MediaSource.Factory,
    private val dataSourceFactory: DataSource.Factory,
    private val resolver: YouTubeStreamResolver
) : MediaSource.Factory {

    // The merge-path legs below are built directly from ProgressiveMediaSource.Factory, not
    // through [delegate], so they need their own copy of whatever policy gets set here -
    // defaults to the app's resilient policy rather than Media3's 3-retry default even if
    // nothing ever calls setLoadErrorHandlingPolicy explicitly.
    private var loadErrorHandlingPolicy: LoadErrorHandlingPolicy = ResilientLoadErrorHandlingPolicy()

    override fun setDrmSessionManagerProvider(provider: DrmSessionManagerProvider): MediaSource.Factory {
        delegate.setDrmSessionManagerProvider(provider)
        return this
    }

    override fun setLoadErrorHandlingPolicy(policy: LoadErrorHandlingPolicy): MediaSource.Factory {
        loadErrorHandlingPolicy = policy
        delegate.setLoadErrorHandlingPolicy(policy)
        return this
    }

    override fun getSupportedTypes(): IntArray = delegate.supportedTypes

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val uri = mediaItem.localConfiguration?.uri?.toString()
        if (uri != null && YoutubeUrlUtils.isYoutubeUrl(uri)) {
            val cached = resolver.peekCached(uri, audioOnly = false)
            val videoOnlyUrl = cached?.videoOnlyStreamUrl
            val audioUrl = cached?.audioStreamUrl
            if (cached != null && cached.videoStreamUrl == null && videoOnlyUrl != null && audioUrl != null) {
                PlaybackDiagnostics.log("mediaSource: MERGE path (video-only + audio) for $uri")
                val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                    .createMediaSource(mediaItem.buildUpon().setUri(videoOnlyUrl).build())
                val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                    .createMediaSource(mediaItem.buildUpon().setUri(audioUrl).build())
                return MergingMediaSource(videoSource, audioSource)
            }
            PlaybackDiagnostics.log(
                "mediaSource: single-URI path for $uri (cache ${if (cached == null) "miss" else "hit, muxed stream available"})"
            )
        }
        return delegate.createMediaSource(mediaItem)
    }
}
