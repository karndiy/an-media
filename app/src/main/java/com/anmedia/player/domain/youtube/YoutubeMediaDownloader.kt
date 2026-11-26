package com.anmedia.player.domain.youtube

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.MediaFormat as NpFormat
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.VideoStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** What to save: audio-only (.m4a, AAC straight from YouTube) or a video (.mp4) capped at [maxHeight]. */
sealed class DownloadFormat(val label: String) {
    data object AudioM4a : DownloadFormat("Audio (M4A)")
    data class VideoMp4(val maxHeight: Int) : DownloadFormat("Video MP4 ${maxHeight}p")

    companion object {
        val ALL: List<DownloadFormat> = listOf(AudioM4a, VideoMp4(360), VideoMp4(720), VideoMp4(1080))
    }
}

/**
 * Downloads a YouTube video to shared storage: audio to Music/AnMedia (.m4a), video to
 * Movies/AnMedia (.mp4). No transcoding - YouTube's AAC audio and H.264 video streams are
 * fetched as-is and remuxed into a plain MP4 container with [MediaMuxer], so no FFmpeg is needed.
 *
 * Runs on the app-process scope (not tied to a screen), with progress in a notification.
 */
class YoutubeMediaDownloader(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSaved: suspend () -> Unit = {}
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val nextNotificationId = AtomicInteger(5000)

    fun download(url: String, title: String, format: DownloadFormat) {
        val notificationId = nextNotificationId.getAndIncrement()
        Toast.makeText(context, "Downloading: $title", Toast.LENGTH_SHORT).show()
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { downloadBlocking(url, title, format, notificationId) }
            }
            result.onSuccess { savedPath ->
                notifyDone(notificationId, title, "Saved to $savedPath")
                Toast.makeText(context, "Saved: $title", Toast.LENGTH_SHORT).show()
                runCatching { onSaved() }
            }.onFailure { e ->
                notifyDone(notificationId, title, "Download failed: ${e.message}")
                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun downloadBlocking(url: String, title: String, format: DownloadFormat, notificationId: Int): String {
        // Make sure NewPipe is initialised (the resolver does it lazily on first use).
        YouTubeStreamResolver.ensureNewPipeInitialized()

        notifyProgress(notificationId, title, "Preparing...", -1)
        val extractor = ServiceList.YouTube.getStreamExtractor(url)
        extractor.fetchPage()

        val audio: AudioStream = extractor.audioStreams
            .filter { it.format == NpFormat.M4A && it.isProgressive() }
            .maxByOrNull { it.averageBitrate }
            ?: throw IOException("No M4A audio stream available for this video")

        val video: VideoStream? = (format as? DownloadFormat.VideoMp4)?.let { videoFormat ->
            val candidates = extractor.videoOnlyStreams.filter {
                it.format == NpFormat.MPEG_4 &&
                    it.isProgressive() &&
                    it.codec?.startsWith("avc", ignoreCase = true) == true
            }
            candidates.filter { it.height in 1..videoFormat.maxHeight }.maxByOrNull { it.height }
                ?: candidates.minByOrNull { it.height }
                ?: throw IOException("No MP4 video stream available for this video")
        }

        val safeName = sanitizeFileName(extractor.name ?: title)
        val workDir = File(context.cacheDir, "downloads").apply { mkdirs() }
        val audioTemp = File(workDir, "$notificationId-audio.m4a")
        val videoTemp = File(workDir, "$notificationId-video.mp4")
        val outTemp = File(workDir, "$notificationId-out.mp4")

        try {
            if (video != null) {
                fetchToFile(video.content, videoTemp) { pct ->
                    notifyProgress(notificationId, title, "Video ${video.height}p: $pct%", pct * 8 / 10)
                }
                fetchToFile(audio.content, audioTemp) { pct ->
                    notifyProgress(notificationId, title, "Audio: $pct%", 80 + pct * 15 / 100)
                }
            } else {
                fetchToFile(audio.content, audioTemp) { pct ->
                    notifyProgress(notificationId, title, "Audio: $pct%", pct * 95 / 100)
                }
            }

            notifyProgress(notificationId, title, "Saving...", 97)
            // Remux even audio-only: YouTube serves fragmented MP4, which some players handle
            // poorly; a plain MP4 container plays everywhere. Fall back to the raw file if needed.
            val finalFile = try {
                mux(videoTemp.takeIf { video != null }, audioTemp, outTemp)
                outTemp
            } catch (e: Exception) {
                if (video != null) throw IOException("Could not merge audio and video: ${e.message}", e)
                audioTemp
            }

            return if (video != null) {
                publish(finalFile, "$safeName.mp4", isVideo = true)
            } else {
                publish(finalFile, "$safeName.m4a", isVideo = false)
            }
        } finally {
            audioTemp.delete()
            videoTemp.delete()
            outTemp.delete()
        }
    }

    private fun AudioStream.isProgressive() =
        deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && !content.isNullOrBlank()

    private fun VideoStream.isProgressive() =
        deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && !content.isNullOrBlank()

    /**
     * Downloads in ranged chunks: YouTube's CDN throttles single long-running requests hard,
     * but serves each short ranged request at full speed.
     */
    private fun fetchToFile(url: String, dest: File, onProgress: (Int) -> Unit) {
        var position = 0L
        var total = -1L
        var lastPct = -1
        FileOutputStream(dest).use { out ->
            while (total < 0 || position < total) {
                val end = position + CHUNK_SIZE - 1
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Range", "bytes=$position-$end")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    val body = response.body ?: throw IOException("Empty response")
                    if (total < 0) {
                        total = response.header("Content-Range")
                            ?.substringAfterLast('/')
                            ?.toLongOrNull()
                            ?: body.contentLength().takeIf { response.code == 200 && it > 0 }
                            ?: throw IOException("Unknown file size")
                    }
                    val read = body.byteStream().use { it.copyTo(out) }
                    if (read <= 0L) throw IOException("Download stalled")
                    position += read
                }
                val pct = ((position * 100) / total).toInt().coerceIn(0, 100)
                if (pct != lastPct) {
                    lastPct = pct
                    onProgress(pct)
                }
            }
        }
    }

    /** Copies the audio (and optional video) track into a plain MP4, interleaved by timestamp. */
    private fun mux(videoFile: File?, audioFile: File, out: File) {
        val muxer = MediaMuxer(out.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val sources = mutableListOf<Pair<MediaExtractor, Int>>()
        try {
            for ((file, mimePrefix) in listOfNotNull(videoFile?.let { it to "video/" }, audioFile to "audio/")) {
                val extractor = MediaExtractor()
                extractor.setDataSource(file.path)
                val trackIndex = (0 until extractor.trackCount).firstOrNull {
                    extractor.getTrackFormat(it).getString(android.media.MediaFormat.KEY_MIME)
                        ?.startsWith(mimePrefix) == true
                } ?: throw IOException("No $mimePrefix track found")
                extractor.selectTrack(trackIndex)
                val muxerTrack = muxer.addTrack(extractor.getTrackFormat(trackIndex))
                sources += extractor to muxerTrack
            }
            muxer.start()

            val buffer = ByteBuffer.allocate(4 * 1024 * 1024)
            val info = MediaCodec.BufferInfo()
            val active = sources.toMutableList()
            while (active.isNotEmpty()) {
                // Always write the source whose next sample is earliest, so tracks stay interleaved.
                val next = active.minByOrNull { it.first.sampleTime }!!
                val (extractor, muxerTrack) = next
                buffer.clear()
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) {
                    active.remove(next)
                    continue
                }
                info.offset = 0
                info.size = size
                info.presentationTimeUs = extractor.sampleTime
                info.flags = if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                    MediaCodec.BUFFER_FLAG_KEY_FRAME
                } else {
                    0
                }
                muxer.writeSampleData(muxerTrack, buffer, info)
                extractor.advance()
            }
            muxer.stop()
        } finally {
            sources.forEach { it.first.release() }
            runCatching { muxer.release() }
        }
    }

    /** Copies [file] into shared storage and returns a human-readable location. */
    private fun publish(file: File, displayName: String, isVideo: Boolean): String {
        val folder = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = if (isVideo) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, if (isVideo) "video/mp4" else "audio/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$folder/AnMedia")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: throw IOException("Could not create file in $folder")
            try {
                resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                    ?: throw IOException("Could not open output file")
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
            return "$folder/AnMedia/$displayName"
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(folder), "AnMedia").apply { mkdirs() }
            var dest = File(dir, displayName)
            var n = 1
            while (dest.exists()) {
                dest = File(dir, displayName.substringBeforeLast('.') + " ($n)." + displayName.substringAfterLast('.'))
                n++
            }
            file.copyTo(dest)
            MediaScannerConnection.scanFile(context, arrayOf(dest.path), null, null)
            return "$folder/AnMedia/${dest.name}"
        }
    }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().take(100).ifBlank { "youtube" }

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun ensureChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "YouTube download progress"
                }
            )
        }
    }

    @Suppress("MissingPermission")
    private fun notifyProgress(id: Int, title: String, text: String, percent: Int) {
        if (!canNotify()) return
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, percent.coerceAtLeast(0), percent < 0)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    @Suppress("MissingPermission")
    private fun notifyDone(id: Int, title: String, text: String) {
        if (!canNotify()) return
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        private const val CHANNEL_ID = "downloads"
        private const val CHUNK_SIZE = 5L * 1024 * 1024
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
    }
}
