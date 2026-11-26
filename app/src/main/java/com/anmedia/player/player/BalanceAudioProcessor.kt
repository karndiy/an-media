@file:OptIn(UnstableApi::class)

package com.anmedia.player.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/** Real-time vocal/music split, based on where sounds sit in the stereo image. */
enum class VocalMode(val label: String) {
    NORMAL("Normal"),
    /** Removes centre-panned sound (usually the lead vocal), keeping the bass. */
    KARAOKE("No vocals"),
    /** Keeps only the centre channel in the voice band (~150 Hz - 6 kHz). */
    VOCALS("Vocals only")
}

/**
 * Stereo PCM processor: per-channel gain for the balance/pan control (Media3's
 * [androidx.media3.common.Player.volume] has no left/right split), plus [VocalMode].
 *
 * Vocal separation uses mid/side processing: mid = (L+R)/2 holds what's panned to the centre
 * (lead vocal, but also kick, bass, snare), side = (L-R)/2 holds what's panned wide. It's a
 * classic karaoke trick, not AI stem separation - results depend on the mix, and mono tracks
 * can't be split at all.
 */
class BalanceAudioProcessor : BaseAudioProcessor() {

    @Volatile
    private var leftGain: Float = 1f

    @Volatile
    private var rightGain: Float = 1f

    @Volatile
    var vocalMode: VocalMode = VocalMode.NORMAL

    // One-pole filter coefficients/state, recomputed for the stream's sample rate.
    private var lowCoeff = 0f   // ~150 Hz: bass to keep in karaoke / cut in vocals-only
    private var highCoeff = 0f  // ~6 kHz: upper edge of the voice band
    private var lowState = 0f
    private var highState = 0f

    fun setBalance(balance: Float) {
        val clamped = balance.coerceIn(-1f, 1f)
        leftGain = if (clamped > 0f) 1f - clamped else 1f
        rightGain = if (clamped < 0f) 1f + clamped else 1f
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT && inputAudioFormat.channelCount == 2) {
            lowCoeff = onePoleCoeff(LOW_CUTOFF_HZ, inputAudioFormat.sampleRate)
            highCoeff = onePoleCoeff(HIGH_CUTOFF_HZ, inputAudioFormat.sampleRate)
            inputAudioFormat
        } else {
            AudioProcessor.AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        val outputBuffer = replaceOutputBuffer(remaining)
        val input = inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val output = outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val mode = vocalMode

        while (input.remaining() >= 4) {
            var left = input.short.toFloat()
            var right = input.short.toFloat()

            if (mode != VocalMode.NORMAL) {
                val mid = (left + right) * 0.5f
                val side = (left - right) * 0.5f
                lowState += lowCoeff * (mid - lowState)    // low-passed mid (bass)
                highState += highCoeff * (mid - highState) // mid with highs rolled off
                when (mode) {
                    VocalMode.KARAOKE -> {
                        // Wide parts + centre bass back in, centre voice band removed.
                        left = side + lowState
                        right = -side + lowState
                    }
                    VocalMode.VOCALS -> {
                        // Centre only, band-limited to the voice range.
                        val voice = (highState - lowState) * VOCALS_MAKEUP_GAIN
                        left = voice
                        right = voice
                    }
                    VocalMode.NORMAL -> Unit
                }
            }

            output.putShort(clamp(left * leftGain))
            output.putShort(clamp(right * rightGain))
        }
        inputBuffer.position(inputBuffer.limit())
        outputBuffer.flip()
    }

    override fun onFlush() {
        lowState = 0f
        highState = 0f
    }

    override fun onReset() {
        lowState = 0f
        highState = 0f
    }

    private fun clamp(value: Float): Short =
        max(Short.MIN_VALUE.toInt(), min(Short.MAX_VALUE.toInt(), value.toInt())).toShort()

    private fun onePoleCoeff(cutoffHz: Float, sampleRate: Int): Float =
        if (sampleRate <= 0) 0f else (1.0 - exp(-2.0 * PI * cutoffHz / sampleRate)).toFloat()

    private companion object {
        const val LOW_CUTOFF_HZ = 150f
        const val HIGH_CUTOFF_HZ = 6000f
        const val VOCALS_MAKEUP_GAIN = 1.2f
    }
}
