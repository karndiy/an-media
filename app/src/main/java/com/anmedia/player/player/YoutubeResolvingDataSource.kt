@file:OptIn(UnstableApi::class)

package com.anmedia.player.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import com.anmedia.player.data.repository.MediaRepository
import com.anmedia.player.domain.model.StreamMode
import com.anmedia.player.domain.youtube.YouTubeStreamResolver
import com.anmedia.player.domain.youtube.YoutubeResolveResult
import com.anmedia.player.domain.youtube.YoutubeUrlUtils
import kotlinx.coroutines.runBlocking
import java.io.IOException

/**
 * Lets ExoPlayer treat a YouTube watch URL as a normal [MediaItem] URI. Resolution happens
 * lazily, on the loader thread, the first time the source is opened - and again once the
 * cached CDN URL expires, since [YouTubeStreamResolver] caches resolutions with a TTL.
 */
class YoutubeResolvingDataSource(
    private val resolver: YouTubeStreamResolver,
    private val mediaRepository: MediaRepository
) : ResolvingDataSource.Resolver {

    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val originalUrl = dataSpec.uri.toString()
        if (!YoutubeUrlUtils.isYoutubeUrl(originalUrl)) return dataSpec

        return runBlocking {
            val entity = mediaRepository.getBySourceUri(originalUrl)
            val audioOnly = entity?.preferredStreamMode != StreamMode.VIDEO

            when (val result = resolver.resolve(originalUrl, audioOnly)) {
                is YoutubeResolveResult.Success -> {
                    val stream = result.stream
                    val playableUrl = (if (audioOnly) {
                        stream.audioStreamUrl ?: stream.videoStreamUrl
                    } else {
                        stream.videoStreamUrl ?: stream.audioStreamUrl
                    }) ?: throw IOException("No playable stream found for $originalUrl")

                    entity?.let {
                        runCatching {
                            mediaRepository.updateCachedStream(it.id, playableUrl, stream.expiresAtEpochMs)
                        }
                    }

                    dataSpec.buildUpon().setUri(Uri.parse(playableUrl)).build()
                }
                is YoutubeResolveResult.Error ->
                    throw IOException(result.message, result.cause)
                is YoutubeResolveResult.PlaylistSuccess ->
                    throw IOException("Expected a video stream but resolved a playlist: $originalUrl")
                is YoutubeResolveResult.SearchSuccess ->
                    throw IOException("Expected a video stream but resolved a search result: $originalUrl")
            }
        }
    }
}
