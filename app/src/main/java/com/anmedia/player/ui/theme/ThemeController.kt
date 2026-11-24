package com.anmedia.player.ui.theme

import com.anmedia.player.data.repository.PlayerStateRepository
import com.anmedia.player.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Process-wide holder of the user's chosen [ThemeMode], read once at startup in [init] and kept
 * in sync with the persisted value afterwards - the same singleton-controller pattern as
 * [com.anmedia.player.player.AudioEffectsController], so the theme can be changed from deep in
 * the nav graph (the Player screen's theme picker) while [MainActivity] collects it at the top
 * of the Compose tree without threading a shared ViewModel through every screen in between.
 */
object ThemeController {

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private var repository: PlayerStateRepository? = null
    private var scope: CoroutineScope? = null
    private var initialized = false

    fun init(repository: PlayerStateRepository, scope: CoroutineScope) {
        if (initialized) return
        initialized = true
        this.repository = repository
        this.scope = scope
        scope.launch {
            repository.observeThemeMode().collect { _themeMode.value = it }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        scope?.launch { repository?.saveThemeMode(mode) }
    }
}
