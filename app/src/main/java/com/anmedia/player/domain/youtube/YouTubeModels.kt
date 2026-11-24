package com.anmedia.player.domain.youtube

data class ResolvedYoutubeStream(
    val videoId: String,
    val title: String,
    val channelName: String?,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val originalUrl: String,
    val audioStreamUrl: String?,
    val videoStreamUrl: String?,
    val bitrateKbps: Int,
    val videoOnlyStreamUrl: String? = null,
    val resolvedAtEpochMs: Long = System.currentTimeMillis()
) {
    val expiresAtEpochMs: Long get() = resolvedAtEpochMs + STREAM_TTL_MS

    companion object {
        const val STREAM_TTL_MS = 5 * 60 * 60 * 1000L
    }
}

data class YoutubePlaylistInfo(
    val name: String,
    val videoUrls: List<String>
)

data class YoutubeSearchResult(
    val url: String,
    val title: String,
    val channelName: String?,
    val durationSeconds: Long,
    val thumbnailUrl: String?
)

sealed class YoutubeResolveResult {
    data class Success(val stream: ResolvedYoutubeStream) : YoutubeResolveResult()
    data class PlaylistSuccess(val playlist: YoutubePlaylistInfo) : YoutubeResolveResult()
    data class SearchSuccess(val results: List<YoutubeSearchResult>) : YoutubeResolveResult()
    data class Error(val message: String, val cause: Throwable? = null) : YoutubeResolveResult()
}

object YoutubeUrlUtils {
    private val VIDEO_ID_REGEX = Regex("(?:v=|/|youtu\\.be/|embed/|shorts/)([a-zA-Z0-9_-]{11})")

    fun isPlaylistUrl(url: String): Boolean = url.contains("list=")

    fun isYoutubeUrl(url: String): Boolean =
        url.contains("youtube.com") || url.contains("youtu.be")

    fun extractVideoId(url: String): String? = VIDEO_ID_REGEX.find(url)?.groupValues?.get(1)
}
