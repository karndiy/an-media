@file:OptIn(UnstableApi::class)

package com.anmedia.player.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * Scales per-channel PCM gain to emulate a stereo balance/pan control, since Media3's
 * [androidx.media3.common.Player.volume] is a single overall gain with no left/right split.
 */
class BalanceAudioProcessor : BaseAudioProcessor() {

    @Volatile
    private var leftGain: Float = 1f

    @Volatile
    private var rightGain: Float = 1f

    fun setBalance(balance: Float) {
        val clamped = balance.coerceIn(-1f, 1f)
        leftGain = if (clamped > 0f) 1f - clamped else 1f
        rightGain = if (clamped < 0f) 1f + clamped else 1f
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT && inputAudioFormat.channelCount == 2) {
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

        while (input.remaining() >= 4) {
            val left = input.short
            val right = input.short
            output.putShort(scale(left, leftGain))
            output.putShort(scale(right, rightGain))
        }
        inputBuffer.position(inputBuffer.limit())
        outputBuffer.flip()
    }

    private fun scale(sample: Short, gain: Float): Short {
        val scaled = (sample * gain).toInt()
        return max(Short.MIN_VALUE.toInt(), min(Short.MAX_VALUE.toInt(), scaled)).toShort()
    }
}
