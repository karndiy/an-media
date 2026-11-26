package com.anmedia.player.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-wide singleton so the UI layer can push balance changes straight into the
 * [BalanceAudioProcessor] instance wired into ExoPlayer's renderer chain, with no IPC.
 */
object StereoBalanceController {
    val processor = BalanceAudioProcessor()

    private val _balance = MutableStateFlow(0f)
    val balance: StateFlow<Float> = _balance

    private val _vocalMode = MutableStateFlow(VocalMode.NORMAL)
    val vocalMode: StateFlow<VocalMode> = _vocalMode

    fun setVocalMode(mode: VocalMode) {
        processor.vocalMode = mode
        _vocalMode.value = mode
    }

    fun setBalance(value: Float) {
        val clamped = value.coerceIn(-1f, 1f)
        processor.setBalance(clamped)
        _balance.value = clamped
    }
}
