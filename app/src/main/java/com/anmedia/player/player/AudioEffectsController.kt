package com.anmedia.player.player

import android.media.audiofx.Equalizer
import android.media.audiofx.Visualizer
import android.util.Log
import com.anmedia.player.domain.model.EqPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.hypot
import kotlin.math.min

data class EqBandInfo(
    val index: Int,
    val centerFreqHz: Int,
    val levelMb: Int
)

data class EqUiState(
    val supported: Boolean = false,
    val enabled: Boolean = true,
    val minLevelMb: Int = -1500,
    val maxLevelMb: Int = 1500,
    val bands: List<EqBandInfo> = emptyList(),
    val preset: EqPreset = EqPreset.FLAT,
    val preampMb: Int = 0
)

/**
 * Process-wide owner of the native [Equalizer] and [Visualizer], attached to whatever
 * ExoPlayer audio session is currently live. Lives alongside [PlaybackService] in the same
 * process as the UI, so the Compose layer collects these flows directly with no IPC.
 */
object AudioEffectsController {

    private const val TAG = "AudioEffectsController"
    private const val SPECTRUM_BARS = 24
    private const val CAPTURE_RATE_DIVISOR = 2

    private var equalizer: Equalizer? = null
    private var visualizer: Visualizer? = null
    private var currentSessionId: Int = 0
    private var pendingCustomBandLevels: List<Int>? = null

    private val presetTablesDb: Map<EqPreset, List<Int>> = mapOf(
        EqPreset.FLAT to listOf(0, 0, 0, 0, 0),
        EqPreset.ROCK to listOf(5, 3, -3, 2, 4),
        EqPreset.POP to listOf(-1, 2, 4, 2, -1),
        EqPreset.JAZZ to listOf(3, 2, 0, 2, 3),
        EqPreset.BASS_BOOST to listOf(7, 5, 0, 0, 0)
    )

    private val _eqState = MutableStateFlow(EqUiState())
    val eqState: StateFlow<EqUiState> = _eqState

    private val _spectrum = MutableStateFlow(FloatArray(SPECTRUM_BARS))
    val spectrum: StateFlow<FloatArray> = _spectrum

    private val _waveform = MutableStateFlow(ByteArray(0))
    val waveform: StateFlow<ByteArray> = _waveform

    @Synchronized
    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == currentSessionId) return
        releaseInternal()
        currentSessionId = audioSessionId

        try {
            val eq = Equalizer(0, audioSessionId)
            eq.enabled = _eqState.value.enabled
            equalizer = eq
            publishEqState(eq)

            val pendingLevels = pendingCustomBandLevels
            pendingCustomBandLevels = null
            if (pendingLevels != null && _eqState.value.preset == EqPreset.CUSTOM) {
                pendingLevels.forEachIndexed { index, level ->
                    if (index < eq.numberOfBands) setBandLevel(index, level)
                }
            } else {
                applyPresetInternal(_eqState.value.preset, eq)
            }
        } catch (e: RuntimeException) {
            Log.w(TAG, "Equalizer unavailable for session $audioSessionId", e)
            _eqState.value = _eqState.value.copy(supported = false)
        }

        try {
            val viz = Visualizer(audioSessionId)
            val captureSize = Visualizer.getCaptureSizeRange()[1]
            viz.captureSize = captureSize
            val rate = min(Visualizer.getMaxCaptureRate() / CAPTURE_RATE_DIVISOR, 20000)
            viz.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray, samplingRate: Int) {
                    _waveform.value = waveform
                }

                override fun onFftDataCapture(v: Visualizer?, fft: ByteArray, samplingRate: Int) {
                    _spectrum.value = fftToBars(fft, SPECTRUM_BARS)
                }
            }, rate, true, true)
            viz.enabled = true
            visualizer = viz
        } catch (e: SecurityException) {
            Log.w(TAG, "RECORD_AUDIO permission missing; visualizer disabled", e)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Visualizer unavailable for session $audioSessionId", e)
        }
    }

    @Synchronized
    fun release() {
        releaseInternal()
        currentSessionId = 0
    }

    private fun releaseInternal() {
        equalizer?.release()
        equalizer = null
        visualizer?.let {
            it.enabled = false
            it.release()
        }
        visualizer = null
        _spectrum.value = FloatArray(SPECTRUM_BARS)
        _waveform.value = ByteArray(0)
    }

    fun setEqEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        _eqState.value = _eqState.value.copy(enabled = enabled)
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        val eq = equalizer ?: return
        val clamped = levelMb.coerceIn(_eqState.value.minLevelMb, _eqState.value.maxLevelMb)
        eq.setBandLevel(bandIndex.toShort(), clamped.toShort())
        _eqState.value = _eqState.value.copy(
            bands = _eqState.value.bands.map {
                if (it.index == bandIndex) it.copy(levelMb = clamped) else it
            },
            preset = EqPreset.CUSTOM
        )
    }

    fun setPreampLevel(levelMb: Int) {
        val previousPreamp = _eqState.value.preampMb
        _eqState.value = _eqState.value.copy(preampMb = levelMb)

        val eq = equalizer
        if (eq != null && _eqState.value.preset == EqPreset.CUSTOM) {
            // applyPresetInternal is a no-op for CUSTOM (it must not clobber hand-tuned bands
            // with a preset table), so shift every custom band by the preamp delta directly.
            val delta = levelMb - previousPreamp
            if (delta != 0) {
                val newBands = _eqState.value.bands.map { band ->
                    val newLevel = (band.levelMb + delta).coerceIn(_eqState.value.minLevelMb, _eqState.value.maxLevelMb)
                    eq.setBandLevel(band.index.toShort(), newLevel.toShort())
                    band.copy(levelMb = newLevel)
                }
                _eqState.value = _eqState.value.copy(bands = newBands)
            }
        } else {
            reapplyCurrentPreset()
        }
    }

    fun applyPreset(preset: EqPreset) {
        val eq = equalizer ?: run {
            _eqState.value = _eqState.value.copy(preset = preset)
            return
        }
        applyPresetInternal(preset, eq)
    }

    fun restorePersistedState(bandLevels: List<Int>?, preset: EqPreset, enabled: Boolean, preampMb: Int) {
        _eqState.value = _eqState.value.copy(preset = preset, enabled = enabled, preampMb = preampMb)
        val eq = equalizer
        if (eq != null) {
            eq.enabled = enabled
            if (bandLevels != null && preset == EqPreset.CUSTOM) {
                bandLevels.forEachIndexed { index, level ->
                    if (index < eq.numberOfBands) setBandLevel(index, level)
                }
            } else {
                applyPresetInternal(preset, eq)
            }
        } else if (preset == EqPreset.CUSTOM) {
            // No live Equalizer yet (audio session hasn't attached): stash the custom levels so
            // attach() can apply them once it runs, instead of losing them to the flat default.
            pendingCustomBandLevels = bandLevels
        }
    }

    fun currentBandLevels(): List<Int> = _eqState.value.bands.map { it.levelMb }

    private fun reapplyCurrentPreset() {
        val eq = equalizer ?: return
        applyPresetInternal(_eqState.value.preset, eq)
    }

    private fun applyPresetInternal(preset: EqPreset, eq: Equalizer) {
        if (preset == EqPreset.CUSTOM) {
            _eqState.value = _eqState.value.copy(preset = preset)
            return
        }
        val baseTableDb = presetTablesDb[preset] ?: presetTablesDb.getValue(EqPreset.FLAT)
        val bandCount = eq.numberOfBands.toInt()
        val preampMb = _eqState.value.preampMb
        val newBands = mutableListOf<EqBandInfo>()
        for (band in 0 until bandCount) {
            val sourceIndex = (band * baseTableDb.size) / bandCount.coerceAtLeast(1)
            val baseMb = baseTableDb[sourceIndex.coerceIn(0, baseTableDb.size - 1)] * 100
            val levelMb = (baseMb + preampMb).coerceIn(_eqState.value.minLevelMb, _eqState.value.maxLevelMb)
            eq.setBandLevel(band.toShort(), levelMb.toShort())
            newBands += EqBandInfo(band, eq.getCenterFreq(band.toShort()) / 1000, levelMb)
        }
        _eqState.value = _eqState.value.copy(preset = preset, bands = newBands)
    }

    private fun publishEqState(eq: Equalizer) {
        val range = eq.bandLevelRange
        val bands = (0 until eq.numberOfBands).map { band ->
            EqBandInfo(
                index = band,
                centerFreqHz = eq.getCenterFreq(band.toShort()) / 1000,
                levelMb = eq.getBandLevel(band.toShort()).toInt()
            )
        }
        _eqState.value = _eqState.value.copy(
            supported = true,
            minLevelMb = range[0].toInt(),
            maxLevelMb = range[1].toInt(),
            bands = bands
        )
    }

    private fun fftToBars(fft: ByteArray, barCount: Int): FloatArray {
        if (fft.size < 4) return FloatArray(barCount)
        val magnitudeCount = fft.size / 2
        val magnitudes = FloatArray(magnitudeCount)
        magnitudes[0] = Math.abs(fft[0].toInt()).toFloat()
        for (i in 1 until magnitudeCount) {
            val re = fft[2 * i].toFloat()
            val im = if (2 * i + 1 < fft.size) fft[2 * i + 1].toFloat() else 0f
            magnitudes[i] = hypot(re.toDouble(), im.toDouble()).toFloat()
        }

        val bars = FloatArray(barCount)
        val perBar = (magnitudeCount / barCount).coerceAtLeast(1)
        for (bar in 0 until barCount) {
            val start = bar * perBar
            val end = min(start + perBar, magnitudeCount)
            var sum = 0f
            var count = 0
            for (i in start until end) {
                sum += magnitudes[i]
                count++
            }
            val avg = if (count > 0) sum / count else 0f
            bars[bar] = (avg / 128f).coerceIn(0f, 1f)
        }
        return bars
    }
}
