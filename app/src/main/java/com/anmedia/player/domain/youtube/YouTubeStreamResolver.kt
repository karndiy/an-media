package com.anmedia.player.domain.youtube

import com.anmedia.player.player.PlaybackDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.VideoStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Resolves a YouTube watch/playlist URL into direct, ExoPlayer-playable stream URLs via
 * NewPipeExtractor. Successful resolutions are cached in memory for [ResolvedYoutubeStream.STREAM_TTL_MS]
 * so repeated lookups (metadata preview, then playback, then a later re-open of a chunked
 * HTTP source) don't re-run extraction needlessly.
 */
class YouTubeStreamResolver {

    private val cache = ConcurrentHashMap<String, ResolvedYoutubeStream>()

    private fun ensureInitialized() {
        if (initialized.compareAndSet(false, true)) {
            NewPipe.init(
                NewPipeDownloader.getInstance(),
                Localization.fromLocale(Locale.getDefault()),
                ContentCountry(Locale.getDefault().country.ifBlank { "US" })
            )
        }
    }

    /**
     * Synchronous, zero-I/O lookup into the in-memory resolution cache - safe to call from the
     * playback/main thread (e.g. when building a [androidx.media3.exoplayer.source.MediaSource])
     * where a suspending network re-resolve would risk an ANR. Returns null on a cache miss
     * instead of resolving, since [resolve] is the only path allowed to do network I/O.
     */
    fun peekCached(url: String, audioOnly: Boolean): ResolvedYoutubeStream? {
        val cacheKey = "$url|$audioOnly"
        val cached = cache[cacheKey] ?: return null
        return if (cached.expiresAtEpochMs > System.currentTimeMillis()) cached else null
    }

    suspend fun resolve(url: String, audioOnly: Boolean): YoutubeResolveResult = withContext(Dispatchers.IO) {
        val cacheKey = "$url|$audioOnly"
        cache[cacheKey]?.let { cached ->
            if (cached.expiresAtEpochMs > System.currentTimeMillis()) {
                return@withContext YoutubeResolveResult.Success(cached)
            }
            cache.remove(cacheKey)
        }

        ensureInitialized()
        try {
            val videoId = YoutubeUrlUtils.extractVideoId(url) ?: url
            val service = ServiceList.YouTube
            val extractor = service.getStreamExtractor(url)
            extractor.fetchPage()

            // Long content gets the lowest-bitrate audio track instead of the best one: the
            // video-only leg already has to share whatever bandwidth is available, so halving
            // the audio requirement too (hi-fi vs. the lowest tier is usually ~2-3x) matters
            // when every kbps counts, and the quality difference is minor for background-type
            // long-form content in the first place.
            val audioCandidates = extractor.audioStreams.filter { !it.url.isNullOrBlank() }
            val bestAudio: AudioStream? = if (extractor.length > 60 * 60) {
                audioCandidates.minByOrNull { it.averageBitrate }
            } else {
                audioCandidates.maxByOrNull { it.averageBitrate }
            }

            val bestVideo: VideoStream? = if (audioOnly) {
                null
            } else {
                extractor.videoStreams
                    .filter { !it.url.isNullOrBlank() }
                    .maxByOrNull { resolutionRank(it.resolution) }
            }

            // YouTube has largely dropped muxed (audio+video combined) delivery for most
            // videos, so videoStreams above is often empty. Fall back to the best video-only
            // DASH stream, which YoutubeMediaSourceFactory merges with the audio-only stream.
            val bestVideoOnly: VideoStream? = if (audioOnly || bestVideo != null) {
                null
            } else {
                val candidates = extractor.videoOnlyStreams.filter { !it.url.isNullOrBlank() }
                // Picking the single highest resolution unconditionally can hand the device a
                // stream its hardware decoder can't handle (e.g. VP9 at 2160p throws
                // MediaCodecVideoRenderer's NO_EXCEEDS_CAPABILITIES with no stream to fall back
                // to, since this is a single static URL rather than an adaptive selection).
                // Stay within a broadly-supported resolution and prefer AVC/H.264, which has
                // near-universal hardware decode support, over VP9/AV1.
                //
                // Also cap harder for long content: this is a single fixed-quality progressive
                // stream, not adaptive DASH, so there is no fallback if YouTube's CDN throttles
                // a sustained download (which it does for long transfers - short clips finish
                // before throttling engages, long ones don't). No buffer size can compensate for
                // an average download rate that's permanently below what the stream needs, so
                // the only real lever is needing less bandwidth in the first place.
                val safeHeightCap = maxSafeVideoHeight(extractor.length)
                val withinSafeResolution = candidates.filter { it.height in 1..safeHeightCap }
                if (withinSafeResolution.isNotEmpty()) {
                    withinSafeResolution
                        .sortedWith(
                            compareByDescending<VideoStream> { it.codec?.startsWith("avc", ignoreCase = true) == true }
                                .thenByDescending { it.height }
                                // Among equal height/codec preference (e.g. a 60fps vs 30fps
                                // encode of the same resolution), prefer the lower-bitrate one -
                                // meaningfully less bandwidth for near-identical visual quality.
                                .thenBy { it.bitrate.takeIf { bitrate -> bitrate > 0 } ?: Int.MAX_VALUE }
                        )
                        .firstOrNull()
                } else {
                    // Every option exceeds the safe cap (e.g. only 1440p/2160p available) -
                    // take the lowest of those as the best chance of still decoding, rather
                    // than the highest, which is what caused this in the first place.
                    candidates.minByOrNull { it.height }
                }
            }

            val thumbnail = extractor.thumbnails
                .maxByOrNull { it.height }
                ?.url

            PlaybackDiagnostics.log(
                "resolve url=$url audioOnly=$audioOnly durationSec=${extractor.length} " +
                    "muxed=${bestVideo?.let { "${it.resolution}@${it.bitrate / 1000}kbps" } ?: "none"} " +
                    "videoOnly=${bestVideoOnly?.let { "${it.height}p ${it.codec} ${it.bitrate / 1000}kbps host=${runCatching { android.net.Uri.parse(it.url).host }.getOrNull()}" } ?: "none"} " +
                    "audio=${bestAudio?.let { "${it.averageBitrate / 1000}kbps" } ?: "none"}"
            )

            val stream = ResolvedYoutubeStream(
                videoId = videoId,
                title = extractor.name ?: "Unknown title",
                channelName = extractor.uploaderName,
                durationSeconds = extractor.length,
                thumbnailUrl = thumbnail,
                originalUrl = url,
                audioStreamUrl = bestAudio?.url,
                videoStreamUrl = bestVideo?.url,
                videoOnlyStreamUrl = bestVideoOnly?.url,
                bitrateKbps = bestAudio?.averageBitrate ?: 0
            )
            cache[cacheKey] = stream
            YoutubeResolveResult.Success(stream)
        } catch (e: ExtractionException) {
            YoutubeResolveResult.Error("Could not extract YouTube stream: ${e.message}", e)
        } catch (e: IOException) {
            YoutubeResolveResult.Error("Network error resolving YouTube stream: ${e.message}", e)
        } catch (e: Exception) {
            YoutubeResolveResult.Error("Unexpected error resolving YouTube stream: ${e.message}", e)
        }
    }

    suspend fun resolvePlaylist(url: String): YoutubeResolveResult = withContext(Dispatchers.IO) {
        ensureInitialized()
        try {
            val service = ServiceList.YouTube
            val extractor = service.getPlaylistExtractor(url)
            extractor.fetchPage()

            val urls = mutableListOf<String>()
            var page = extractor.initialPage
            urls += page.items.mapNotNull { it.url }

            while (page.hasNextPage() && urls.size < MAX_PLAYLIST_ITEMS) {
                page = extractor.getPage(page.nextPage)
                urls += page.items.mapNotNull { it.url }
            }

            YoutubeResolveResult.PlaylistSuccess(
                YoutubePlaylistInfo(
                    name = extractor.name ?: "YouTube Playlist",
                    videoUrls = urls.take(MAX_PLAYLIST_ITEMS)
                )
            )
        } catch (e: ExtractionException) {
            YoutubeResolveResult.Error("Could not extract YouTube playlist: ${e.message}", e)
        } catch (e: IOException) {
            YoutubeResolveResult.Error("Network error resolving YouTube playlist: ${e.message}", e)
        } catch (e: Exception) {
            YoutubeResolveResult.Error("Unexpected error resolving YouTube playlist: ${e.message}", e)
        }
    }

    suspend fun search(query: String, limit: Int = 10): YoutubeResolveResult = withContext(Dispatchers.IO) {
        ensureInitialized()
        try {
            val service = ServiceList.YouTube
            val extractor = service.getSearchExtractor(query)
            extractor.fetchPage()

            val results = extractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .take(limit)
                .map { item ->
                    YoutubeSearchResult(
                        url = item.url,
                        title = item.name ?: "Unknown title",
                        channelName = item.uploaderName,
                        durationSeconds = item.duration.coerceAtLeast(0),
                        thumbnailUrl = item.thumbnails.maxByOrNull { it.height }?.url
                    )
                }

            YoutubeResolveResult.SearchSuccess(results)
        } catch (e: ExtractionException) {
            YoutubeResolveResult.Error("Could not search YouTube: ${e.message}", e)
        } catch (e: IOException) {
            YoutubeResolveResult.Error("Network error searching YouTube: ${e.message}", e)
        } catch (e: Exception) {
            YoutubeResolveResult.Error("Unexpected error searching YouTube: ${e.message}", e)
        }
    }

    private fun resolutionRank(resolution: String?): Int =
        resolution?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0

    /**
     * Lower the resolution ceiling as content gets longer, since a longer download is more
     * likely to run into YouTube's sustained-download throttling - the required bitrate matters
     * far more than any buffer size once the average available rate is below it for good.
     *
     * The default (short-content) cap is 720p, not 1080p: a high-bitrate video-only stream
     * doesn't need to be *long* to stutter under this app's merged video-only+audio-only
     * playback - it needs two independent HTTP downloads to sustain a combined rate at once,
     * which is a much tighter bandwidth budget than a single equivalent muxed stream would need.
     * "Large file" symptoms reported on contently well under an hour confirmed the duration-only
     * cap left plenty of normal-length videos fully uncapped at 1080p.
     *
     * The >3h tier drops all the way to 144p based on a real diagnostic log: a 332-minute video
     * was still only sustaining ~35-48% of the rate needed even at the (then) 360p cap, a steady
     * ratio across multiple measurements rather than random jitter - a fixed, severe throttle on
     * that connection, not a transient dip. Downloading the actual CDN stream directly confirmed
     * it isn't a blanket server-side throttle on the video itself (full speed, no slowdown from
     * the start of the file to the end), so the constraint is specific to that network/device;
     * 144p needs on the order of 15kbps of video, about as low as YouTube goes, to leave maximum
     * headroom for whatever is actually limiting that connection.
     */
    private fun maxSafeVideoHeight(durationSeconds: Long): Int = when {
        durationSeconds > 3 * 60 * 60 -> 144    // > 3h: prioritize stability hard
        durationSeconds > 60 * 60 -> 480         // 1-3h
        else -> MAX_SAFE_VIDEO_HEIGHT             // default cap
    }

    companion object {
        private const val MAX_PLAYLIST_ITEMS = 200
        private const val MAX_SAFE_VIDEO_HEIGHT = 720
        private val initialized = AtomicBoolean(false)
    }
}
