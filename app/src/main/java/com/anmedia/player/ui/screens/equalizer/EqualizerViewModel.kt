package com.anmedia.player.ui.screens.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anmedia.player.data.repository.PlayerStateRepository
import com.anmedia.player.domain.model.EqPreset
import com.anmedia.player.player.AudioEffectsController
import com.anmedia.player.player.EqUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EqualizerViewModel(
    private val playerStateRepository: PlayerStateRepository
) : ViewModel() {

    val eqState: StateFlow<EqUiState> = AudioEffectsController.eqState

    fun setEnabled(enabled: Boolean) {
        AudioEffectsController.setEqEnabled(enabled)
        viewModelScope.launch { playerStateRepository.saveEqEnabled(enabled) }
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        AudioEffectsController.setBandLevel(bandIndex, levelMb)
        viewModelScope.launch { playerStateRepository.saveEqBandLevels(AudioEffectsController.currentBandLevels()) }
        viewModelScope.launch { playerStateRepository.saveEqPreset(EqPreset.CUSTOM) }
    }

    fun applyPreset(preset: EqPreset) {
        AudioEffectsController.applyPreset(preset)
        viewModelScope.launch {
            playerStateRepository.saveEqPreset(preset)
            playerStateRepository.saveEqBandLevels(AudioEffectsController.currentBandLevels())
        }
    }

    fun setPreamp(levelMb: Int) {
        AudioEffectsController.setPreampLevel(levelMb)
        viewModelScope.launch {
            playerStateRepository.savePreampLevel(levelMb)
            playerStateRepository.saveEqBandLevels(AudioEffectsController.currentBandLevels())
        }
    }
}
