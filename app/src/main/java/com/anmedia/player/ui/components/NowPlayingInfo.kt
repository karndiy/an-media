package com.anmedia.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anmedia.player.ui.theme.LocalExtendedColors
import java.util.Locale

@Composable
fun NowPlayingSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val extended = LocalExtendedColors.current
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, extended.divider, MaterialTheme.shapes.medium)
            .padding(PaddingValues(horizontal = 14.dp, vertical = 10.dp))
    ) {
        content()
    }
}

@Composable
fun MarqueeTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    Text(
        text = text.ifBlank { "Nothing playing" },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color,
        maxLines = 1,
        modifier = modifier
            .fillMaxWidth()
            .basicMarquee(iterations = Int.MAX_VALUE)
    )
}

@Composable
fun FormatBadge(
    bitrateKbps: Int,
    sampleRateHz: Int,
    modifier: Modifier = Modifier
) {
    val label = buildString {
        if (bitrateKbps > 0) append("$bitrateKbps kbps")
        if (sampleRateHz > 0) {
            if (isNotEmpty()) append(" - ")
            append("${sampleRateHz / 1000} kHz")
        }
        if (isEmpty()) append("--")
    }
    val extended = LocalExtendedColors.current
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = extended.textSecondary,
        modifier = modifier
    )
}

@Composable
fun PlaybackClock(
    positionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    val extended = LocalExtendedColors.current
    Text(
        text = formatTime(positionMs) + " / " + formatTime(durationMs),
        style = MaterialTheme.typography.labelMedium,
        color = extended.textSecondary,
        modifier = modifier
    )
}

@Composable
fun MetaStatusRow(
    bitrateKbps: Int,
    sampleRateHz: Int,
    positionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FormatBadge(bitrateKbps, sampleRateHz)
        PlaybackClock(positionMs, durationMs)
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
