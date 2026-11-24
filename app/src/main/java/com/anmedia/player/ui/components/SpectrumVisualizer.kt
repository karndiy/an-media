package com.anmedia.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.anmedia.player.ui.theme.LocalExtendedColors

private enum class VisualizerMode { BARS, SCOPE }

@Composable
fun SpectrumVisualizerPanel(
    spectrum: FloatArray,
    waveform: ByteArray,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(VisualizerMode.BARS) }
    val extended = LocalExtendedColors.current

    NowPlayingSurface(
        modifier = modifier.clickable {
            mode = if (mode == VisualizerMode.BARS) VisualizerMode.SCOPE else VisualizerMode.BARS
        }
    ) {
        if (mode == VisualizerMode.BARS) {
            SpectrumBars(
                spectrum = spectrum,
                modifier = Modifier.fillMaxSize(),
                barColor = extended.accent,
                peakColor = extended.success
            )
        } else {
            Oscilloscope(waveform = waveform, modifier = Modifier.fillMaxSize(), color = extended.accent)
        }
    }
}

@Composable
fun SpectrumBars(
    spectrum: FloatArray,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    peakColor: Color = MaterialTheme.colorScheme.primary
) {
    val barCount = spectrum.size
    val peaks = remember(barCount) { FloatArray(barCount) }
    val peakHold = remember(barCount) { IntArray(barCount) }

    for (i in spectrum.indices) {
        val v = spectrum[i].coerceIn(0f, 1f)
        when {
            v >= peaks[i] -> {
                peaks[i] = v
                peakHold[i] = 12
            }
            peakHold[i] > 0 -> peakHold[i] = peakHold[i] - 1
            else -> peaks[i] = (peaks[i] - 0.03f).coerceAtLeast(v)
        }
    }

    Canvas(modifier = modifier.background(Color.Transparent)) {
        if (barCount == 0) return@Canvas
        val gapPx = 2.dp.toPx()
        val barWidth = ((size.width - gapPx * (barCount - 1)) / barCount).coerceAtLeast(1f)
        for (i in 0 until barCount) {
            val value = spectrum[i].coerceIn(0f, 1f)
            val barHeight = size.height * value
            val x = i * (barWidth + gapPx)

            drawRect(
                color = barColor,
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight)
            )

            val peakY = (size.height * (1f - peaks[i].coerceIn(0f, 1f))).coerceIn(0f, size.height - 2.dp.toPx())
            drawRect(
                color = peakColor,
                topLeft = Offset(x, peakY),
                size = Size(barWidth, 2.dp.toPx())
            )
        }
    }
}

@Composable
fun Oscilloscope(
    waveform: ByteArray,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier.background(Color.Transparent)) {
        if (waveform.size < 2) {
            drawLine(color, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 2.dp.toPx())
            return@Canvas
        }
        val path = Path()
        val stepX = size.width / (waveform.size - 1)
        waveform.forEachIndexed { index, byte ->
            val normalized = (byte.toInt() and 0xFF) / 255f
            val y = size.height * (1f - normalized)
            val x = index * stepX
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 2.dp.toPx()))
    }
}
