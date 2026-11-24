package com.anmedia.player.di

import android.content.Context
import com.anmedia.player.data.local.AppDatabase
import com.anmedia.player.data.mediastore.MediaStoreScanner
import com.anmedia.player.data.repository.MediaRepository
import com.anmedia.player.data.repository.PlayerStateRepository
import com.anmedia.player.data.repository.PlaylistRepository
import com.anmedia.player.domain.youtube.YouTubeStreamResolver
import com.anmedia.player.player.PlayerManager
import com.anmedia.player.ui.theme.ThemeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Hand-rolled dependency container, app process-scoped. Keeps [PlayerManager] and the
 * [YouTubeStreamResolver] cache alive for the whole app lifetime so they stay in sync with
 * [com.anmedia.player.service.PlaybackService], which runs in this same process.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }

    private val mediaStoreScanner by lazy { MediaStoreScanner(appContext) }

    val mediaRepository: MediaRepository by lazy {
        MediaRepository(database.mediaItemDao(), mediaStoreScanner)
    }

    val playlistRepository: PlaylistRepository by lazy {
        PlaylistRepository(database.playlistDao(), database.mediaItemDao())
    }

    val playerStateRepository: PlayerStateRepository by lazy {
        PlayerStateRepository(database.playerStateDao())
    }

    val youTubeStreamResolver: YouTubeStreamResolver by lazy { YouTubeStreamResolver() }

    val playerManager: PlayerManager by lazy {
        PlayerManager(appContext, mediaRepository, playerStateRepository, appScope)
    }

    init {
        ThemeController.init(playerStateRepository, appScope)
    }
}
