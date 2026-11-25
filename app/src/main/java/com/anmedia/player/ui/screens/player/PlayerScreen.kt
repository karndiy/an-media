package com.anmedia.player.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.anmedia.player.data.local.entity.MediaItemEntity
import com.anmedia.player.domain.model.RepeatMode
import com.anmedia.player.domain.model.ThemeMode
import com.anmedia.player.player.AudioEffectsController
import com.anmedia.player.ui.components.AppSlider
import com.anmedia.player.ui.components.BevelPanel
import com.anmedia.player.ui.components.IconCircleButton
import com.anmedia.player.ui.components.IconToggleButton
import com.anmedia.player.ui.components.MarqueeTitle
import com.anmedia.player.ui.components.PlaybackClock
import com.anmedia.player.ui.components.SpectrumVisualizerPanel
import com.anmedia.player.ui.components.VideoSurface
import com.anmedia.player.ui.screens.common.AboutDialog
import com.anmedia.player.ui.theme.LocalExtendedColors
import com.anmedia.player.ui.theme.ThemeController
import com.anmedia.player.update.UpdateCheckState
import com.anmedia.player.update.UpdateChecker

/**
 * The whole screen is one [LazyColumn] - including the fixed-looking header content above the
 * playlist - rather than a [Column] with a `weight(1f)` playlist panel. A weighted child only
 * ever gets *leftover* space after its siblings, which on a small screen (or just a long error
 * message) can squeeze it to zero height with no way to reach it; a single scrollable list has
 * no such failure mode on any screen size, and keeps LazyColumn's recycling for long queues.
 */
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    onNavigateToLibrary: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val player by viewModel.playerFlow.collectAsStateWithLifecycle()
    val spectrum by AudioEffectsController.spectrum.collectAsStateWithLifecycle()
    val waveform by AudioEffectsController.waveform.collectAsStateWithLifecycle()
    val extended = LocalExtendedColors.current
    val themeMode by ThemeController.themeMode.collectAsStateWithLifecycle()
    val updateState by UpdateChecker.state.collectAsStateWithLifecycle()
    var showThemePicker by remember { mutableStateOf(false) }
    var showDebugLog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { showDebugLog = true }) {
                    Icon(Icons.Filled.BugReport, contentDescription = "Debug log", tint = extended.textSecondary)
                }
                IconButton(onClick = { showThemePicker = true }) {
                    Icon(Icons.Filled.Palette, contentDescription = "Change theme", tint = extended.textSecondary)
                }
                Box {
                    IconButton(onClick = { showAbout = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "About AnMedia", tint = extended.textSecondary)
                    }
                    if (updateState is UpdateCheckState.Available) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                                .size(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(extended.accent)
                        )
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(extended.surfaceSunken)
            ) {
                when {
                    state.hasVideo -> VideoSurface(player = player, useController = false, modifier = Modifier.fillMaxSize())
                    state.artworkUri != null -> AsyncImage(
                        model = state.artworkUri,
                        contentDescription = "Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = extended.textTertiary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MarqueeTitle(
                    text = state.title,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                PlaybackClock(positionMs = state.positionMs, durationMs = state.durationMs)
            }
        }

        state.errorMessage?.let { message ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(extended.danger.copy(alpha = 0.12f))
                        .padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = extended.danger,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = viewModel::dismissError) {
                        Icon(Icons.Filled.Close, contentDescription = "Dismiss error", tint = extended.danger)
                    }
                }
            }
        }

        item {
            SpectrumVisualizerPanel(
                spectrum = spectrum,
                waveform = waveform,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
        }

        item {
            SeekBar(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                onSeek = viewModel::seekTo,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconToggleButton(
                    icon = Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    active = state.shuffleEnabled,
                    onClick = viewModel::toggleShuffle
                )
                IconCircleButton(
                    icon = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    onClick = viewModel::skipToPrevious
                )
                IconCircleButton(
                    icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    size = 56.dp,
                    filled = true,
                    onClick = viewModel::playPause
                )
                IconCircleButton(
                    icon = Icons.Filled.Stop,
                    contentDescription = "Stop",
                    onClick = viewModel::stop
                )
                IconCircleButton(
                    icon = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    onClick = viewModel::skipToNext
                )
                IconToggleButton(
                    icon = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = "Repeat",
                    active = state.repeatMode != RepeatMode.OFF,
                    onClick = viewModel::cycleRepeatMode
                )
            }
        }

        item {
            BevelPanel(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Volume", style = MaterialTheme.typography.labelLarge, color = extended.textSecondary)
                    AppSlider(
                        value = state.volume,
                        onValueChange = viewModel::setVolume,
                        valueRange = 0f..1f
                    )
                    Text(text = "Balance", style = MaterialTheme.typography.labelLarge, color = extended.textSecondary)
                    AppSlider(
                        value = state.balance,
                        onValueChange = viewModel::setBalance,
                        valueRange = -1f..1f
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Playlist", style = MaterialTheme.typography.titleSmall, color = extended.textPrimary)
                IconButton(onClick = onNavigateToLibrary) {
                    Icon(Icons.Filled.Add, contentDescription = "Add to playlist", tint = extended.accent)
                }
            }
        }

        if (queue.isEmpty()) {
            item {
                BevelPanel(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Queue is empty. Add tracks from the Library tab.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extended.textTertiary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        } else {
            playlistItems(
                queue = queue,
                currentIndex = state.currentIndex,
                onPlay = viewModel::playQueueIndex,
                onRemove = viewModel::removeFromQueue
            )
        }

        item { Box(modifier = Modifier.height(12.dp)) }
    }

    if (showThemePicker) {
        ThemePickerDialog(
            current = themeMode,
            onSelect = {
                ThemeController.setThemeMode(it)
                showThemePicker = false
            },
            onDismiss = { showThemePicker = false }
        )
    }

    if (showDebugLog) {
        DebugLogDialog(onDismiss = { showDebugLog = false })
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

@Composable
private fun ThemePickerDialog(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val extended = LocalExtendedColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme") },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mode == current,
                            onClick = { onSelect(mode) }
                        )
                        Text(
                            text = mode.label,
                            modifier = Modifier.padding(start = 8.dp),
                            color = extended.textPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

private fun LazyListScope.playlistItems(
    queue: List<MediaItemEntity>,
    currentIndex: Int,
    onPlay: (Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    itemsIndexed(queue, key = { _, item -> item.id }) { index, item ->
        QueueRow(
            entity = item,
            isCurrent = index == currentIndex,
            onClick = { onPlay(index) },
            onRemove = { onRemove(index) }
        )
    }
}

@Composable
private fun QueueRow(
    entity: MediaItemEntity,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val extended = LocalExtendedColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCurrent) extended.chipSelectedBg else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entity.title,
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isCurrent) extended.accent else extended.textPrimary
            )
            Text(
                text = entity.artist ?: entity.channelName ?: "",
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
                color = extended.textSecondary
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = "Remove from playlist", tint = extended.textSecondary)
        }
    }
}

@Composable
private fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val duration = durationMs.coerceAtLeast(1L).toFloat()
    var dragValue by remember { mutableStateOf<Float?>(null) }

    val displayedValue = dragValue ?: positionMs.toFloat().coerceIn(0f, duration)

    AppSlider(
        value = displayedValue,
        onValueChange = { dragValue = it },
        onValueChangeFinished = {
            dragValue?.let { onSeek(it.toLong()) }
            dragValue = null
        },
        valueRange = 0f..duration,
        modifier = modifier
    )
}
