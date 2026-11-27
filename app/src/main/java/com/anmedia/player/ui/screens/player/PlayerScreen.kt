package com.anmedia.player.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.anmedia.player.player.StereoBalanceController
import com.anmedia.player.player.VocalMode
import com.anmedia.player.ui.components.SegmentedControl
import com.anmedia.player.ui.components.AppSlider
import com.anmedia.player.ui.components.IconCircleButton
import com.anmedia.player.ui.components.IconToggleButton
import com.anmedia.player.ui.components.MarqueeTitle
import com.anmedia.player.ui.components.SpectrumVisualizerPanel
import com.anmedia.player.ui.components.VideoSurface
import com.anmedia.player.ui.components.formatTime
import com.anmedia.player.ui.screens.common.AboutDialog
import com.anmedia.player.ui.theme.LocalExtendedColors
import com.anmedia.player.ui.theme.ThemeController
import com.anmedia.player.update.UpdateCheckState
import com.anmedia.player.update.UpdateChecker

/**
 * Now Playing layout: the transport controls are always on screen. The artwork box takes
 * whatever height is left (`weight(1f)`) and shrinks on small screens, so the controls are never
 * pushed off-screen. The queue lives in a bottom sheet, and volume/balance live in a second
 * sheet, so neither competes with the player for space.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    onNavigateToLibrary: () -> Unit = {},
    onEnterFullscreen: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val player by viewModel.playerFlow.collectAsStateWithLifecycle()
    val spectrum by AudioEffectsController.spectrum.collectAsStateWithLifecycle()
    val waveform by AudioEffectsController.waveform.collectAsStateWithLifecycle()
    val extended = LocalExtendedColors.current
    val themeMode by ThemeController.themeMode.collectAsStateWithLifecycle()
    val updateState by UpdateChecker.state.collectAsStateWithLifecycle()
    val vocalMode by StereoBalanceController.vocalMode.collectAsStateWithLifecycle()
    var showThemePicker by remember { mutableStateOf(false) }
    var showDebugLog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showSound by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Top bar: one overflow menu instead of three separate icons.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.titleSmall,
                color = extended.textSecondary,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = extended.textSecondary)
                }
                if (updateState is UpdateCheckState.Available) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(extended.accent)
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Theme") },
                        leadingIcon = { Icon(Icons.Filled.Palette, contentDescription = null) },
                        onClick = { showMenu = false; showThemePicker = true }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(if (updateState is UpdateCheckState.Available) "About (update available)" else "About")
                        },
                        leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                        onClick = { showMenu = false; showAbout = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Debug log") },
                        leadingIcon = { Icon(Icons.Filled.BugReport, contentDescription = null) },
                        onClick = { showMenu = false; showDebugLog = true }
                    )
                }
            }
        }

        // Artwork / video: fills the leftover height, keeping its aspect ratio.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(if (state.hasVideo) 16f / 9f else 1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(extended.surfaceSunken)
            ) {
                when {
                    state.hasVideo -> {
                        VideoSurface(player = player, useController = false, modifier = Modifier.fillMaxSize())
                        // Transparent layer on top: the video view would otherwise eat the tap.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(onClick = onEnterFullscreen)
                        )
                        IconButton(
                            onClick = onEnterFullscreen,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .padding(4.dp)
                                    .size(24.dp)
                            )
                        }
                    }
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
                            .size(64.dp)
                    )
                }
            }
        }

        MarqueeTitle(text = state.title, color = extended.textPrimary)
        if (state.artist.isNotBlank()) {
            Text(
                text = state.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = extended.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        state.errorMessage?.let { message ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
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

        SpectrumVisualizerPanel(
            spectrum = spectrum,
            waveform = waveform,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(56.dp)
        )

        SeekBar(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            onSeek = viewModel::seekTo,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(state.positionMs),
                style = MaterialTheme.typography.labelMedium,
                color = extended.textSecondary
            )
            Text(
                text = formatTime(state.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = extended.textSecondary
            )
        }

        // Transport: Stop removed (Pause covers it), bigger Play button.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
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
                size = 52.dp,
                onClick = viewModel::skipToPrevious
            )
            IconCircleButton(
                icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = "Play/Pause",
                size = 68.dp,
                filled = true,
                onClick = viewModel::playPause
            )
            IconCircleButton(
                icon = Icons.Filled.SkipNext,
                contentDescription = "Next",
                size = 52.dp,
                onClick = viewModel::skipToNext
            )
            IconToggleButton(
                icon = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                contentDescription = "Repeat",
                active = state.repeatMode != RepeatMode.OFF,
                onClick = viewModel::cycleRepeatMode
            )
        }

        // Bottom actions: open the queue sheet / the volume & balance sheet.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { showQueue = true }) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = extended.accent)
                Text(
                    text = "Playlist (${queue.size})",
                    color = extended.textPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            IconButton(onClick = { showSound = true }) {
                // Accent tint while a vocal mode is on, so it's obvious the audio is being altered.
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Volume, balance and vocals",
                    tint = if (vocalMode != VocalMode.NORMAL) extended.accent else extended.textSecondary
                )
            }
        }
    }

    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playlist (${queue.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = extended.textPrimary
                )
                IconButton(onClick = { showQueue = false; onNavigateToLibrary() }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add to playlist", tint = extended.accent)
                }
            }
            if (queue.isEmpty()) {
                Text(
                    text = "Queue is empty. Add tracks from the Library tab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 40.dp)
                )
            } else {
                val listState = rememberLazyListState(
                    initialFirstVisibleItemIndex = state.currentIndex.coerceIn(0, queue.lastIndex)
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    playlistItems(
                        queue = queue,
                        currentIndex = state.currentIndex,
                        onPlay = viewModel::playQueueIndex,
                        onRemove = viewModel::removeFromQueue
                    )
                }
            }
        }
    }

    if (showSound) {
        ModalBottomSheet(
            onDismissRequest = { showSound = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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
                Text(
                    text = "Vocals / Music",
                    style = MaterialTheme.typography.labelLarge,
                    color = extended.textSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                SegmentedControl(
                    options = VocalMode.entries.map { it.label },
                    selectedIndex = vocalMode.ordinal,
                    onSelect = { StereoBalanceController.setVocalMode(VocalMode.entries[it]) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Works best on stereo songs with the vocal in the centre.",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textTertiary
                )
            }
        }
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
