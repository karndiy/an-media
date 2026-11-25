package com.anmedia.player.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.anmedia.player.BuildConfig
import com.anmedia.player.data.repository.PlayerStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** A newer release found on GitHub than the one currently installed. */
data class UpdateInfo(
    val versionName: String,
    val htmlUrl: String,
    val downloadUrl: String?,
    val releaseNotes: String
)

sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data object UpToDate : UpdateCheckState
    data class Available(val info: UpdateInfo) : UpdateCheckState
    data class Error(val message: String) : UpdateCheckState
}

/**
 * Process-wide singleton (same pattern as [com.anmedia.player.ui.theme.ThemeController]) that
 * checks this project's GitHub releases (https://github.com/karndiy/an-media) for a version
 * newer than [BuildConfig.VERSION_NAME]. AnMedia is sideloaded, not distributed through the Play
 * Store, so GitHub Releases - built and published by `.github/workflows/release.yml` whenever a
 * `vX.Y.Z` tag is pushed - is the actual update channel.
 *
 * An automatic check runs at most once every [AUTO_CHECK_INTERVAL_MS] (tracked via
 * [PlayerStateRepository.loadUpdateLastCheckedAt]); [checkNow] always runs regardless, for a
 * user-initiated "Check for updates" tap. When a release is newer than any version already
 * notified about ([PlayerStateRepository.loadUpdateLastNotifiedVersion]), a system notification
 * is posted whose tap target is the release page (or the APK asset directly, if one is
 * attached) - there's no in-app installer, so handing off to the browser/download manager is
 * the simplest correct behavior here.
 */
object UpdateChecker {

    private const val REPO = "karndiy/an-media"
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/$REPO/releases/latest"
    private const val RELEASES_PAGE_URL = "https://github.com/$REPO/releases/latest"
    private const val CHANNEL_ID = "anmedia_updates_channel"
    private const val NOTIFICATION_ID = 1001
    private val AUTO_CHECK_INTERVAL_MS = TimeUnit.HOURS.toMillis(12)

    private val _state = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val state: StateFlow<UpdateCheckState> = _state

    private var appContext: Context? = null
    private var repository: PlayerStateRepository? = null
    private var scope: CoroutineScope? = null
    private var initialized = false

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    fun init(context: Context, repository: PlayerStateRepository, scope: CoroutineScope) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        this.repository = repository
        this.scope = scope

        scope.launch {
            val lastChecked = repository.loadUpdateLastCheckedAt()
            if (System.currentTimeMillis() - lastChecked >= AUTO_CHECK_INTERVAL_MS) {
                checkNow()
            }
        }
    }

    /** Runs a check immediately, bypassing the automatic-check rate limit. */
    fun checkNow() {
        val repo = repository ?: return
        val context = appContext ?: return
        val coroutineScope = scope ?: return

        coroutineScope.launch {
            _state.value = UpdateCheckState.Checking
            val result = withContext(Dispatchers.IO) { fetchLatestRelease() }
            repo.saveUpdateLastCheckedAt(System.currentTimeMillis())

            result.fold(
                onSuccess = { info ->
                    if (info == null) {
                        _state.value = UpdateCheckState.UpToDate
                    } else {
                        _state.value = UpdateCheckState.Available(info)
                        maybeNotify(context, repo, info)
                    }
                },
                onFailure = { error ->
                    _state.value = UpdateCheckState.Error(error.message ?: "Update check failed")
                }
            )
        }
    }

    private suspend fun maybeNotify(context: Context, repo: PlayerStateRepository, info: UpdateInfo) {
        // Only notify once per newly-seen version, so a background re-check every 12h doesn't
        // re-notify about the same release the user already saw (or dismissed).
        if (repo.loadUpdateLastNotifiedVersion() == info.versionName) return
        repo.saveUpdateLastNotifiedVersion(info.versionName)
        postNotification(context, info)
    }

    private fun postNotification(context: Context, info: UpdateInfo) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "App updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Notifies when a new AnMedia version is available"
                }
            )
        }

        val targetUrl = info.downloadUrl ?: info.htmlUrl
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            viewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("AnMedia ${info.versionName} is available")
            .setContentText("Tap to download the update")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(info.releaseNotes.ifBlank { "Tap to download the update" })
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun fetchLatestRelease(): Result<UpdateInfo?> = try {
        val request = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "AnMedia-App")
            .build()

        client.newCall(request).execute().use { response ->
            // No releases published yet (or none reachable) - not an error, just nothing to offer.
            if (response.code == 404) return Result.success(null)
            if (!response.isSuccessful) return Result.failure(IOException("HTTP ${response.code}"))

            val body = response.body?.string()
                ?: return Result.failure(IOException("Empty response body"))
            val json = JSONObject(body)
            val remoteVersion = json.optString("tag_name").removePrefix("v").removePrefix("V")

            if (remoteVersion.isBlank() || !isNewerVersion(remoteVersion, BuildConfig.VERSION_NAME)) {
                return Result.success(null)
            }

            Result.success(
                UpdateInfo(
                    versionName = remoteVersion,
                    htmlUrl = json.optString("html_url", RELEASES_PAGE_URL),
                    downloadUrl = findApkAssetUrl(json),
                    releaseNotes = json.optString("body").trim()
                )
            )
        }
    } catch (e: IOException) {
        Result.failure(e)
    } catch (e: JSONException) {
        Result.failure(e)
    }

    private fun findApkAssetUrl(release: JSONObject): String? {
        val assets = release.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                return asset.optString("browser_download_url").takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    /** Numeric, dot-separated comparison (e.g. "1.0.15" > "1.0.9"); missing segments count as 0. */
    private fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".", "-").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".", "-").mapNotNull { it.toIntOrNull() }
        val length = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r != l) return r > l
        }
        return false
    }
}
