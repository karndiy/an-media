package com.anmedia.player.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.anmedia.player.ui.screens.equalizer.EqualizerViewModel
import com.anmedia.player.ui.screens.library.LibraryViewModel
import com.anmedia.player.ui.screens.player.PlayerViewModel

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(PlayerViewModel::class.java) ->
                PlayerViewModel(container.playerManager, container.mediaRepository) as T

            modelClass.isAssignableFrom(EqualizerViewModel::class.java) ->
                EqualizerViewModel(container.playerStateRepository) as T

            modelClass.isAssignableFrom(LibraryViewModel::class.java) ->
                LibraryViewModel(
                    container.mediaRepository,
                    container.playlistRepository,
                    container.youTubeStreamResolver,
                    container.playerManager
                ) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
