package com.anmedia.player.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.anmedia.player.data.local.entity.PlaylistEntity
import com.anmedia.player.domain.model.StreamMode
import com.anmedia.player.ui.components.SegmentedControl
import com.anmedia.player.ui.theme.LocalExtendedColors

@Composable
fun AddToPlaylistDialog(
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onAddToExisting: (playlistId: Long) -> Unit,
    onCreateAndAdd: (name: String) -> Unit
) {
    var newPlaylistName by remember { mutableStateOf("") }
    val extended = LocalExtendedColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("New playlist") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                onCreateAndAdd(newPlaylistName.trim())
                                onDismiss()
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Create and add")
                    }
                }

                if (playlists.isEmpty()) {
                    Text(
                        "No playlists yet - create one above.",
                        color = extended.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(playlists, key = PlaylistEntity::id) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddToExisting(playlist.id)
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.PlaylistAdd,
                                        contentDescription = null,
                                        tint = extended.accent
                                    )
                                }
                                Text(
                                    text = playlist.name,
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun ChooseStreamModeDialog(
    videoTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (StreamMode) -> Unit
) {
    var mode by remember { mutableStateOf(StreamMode.AUDIO_ONLY) }
    val extended = LocalExtendedColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    videoTitle,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.textSecondary
                )
                Text("Add as:", style = MaterialTheme.typography.bodyMedium)
                SegmentedControl(
                    options = listOf("Audio only", "Video"),
                    selectedIndex = if (mode == StreamMode.AUDIO_ONLY) 0 else 1,
                    onSelect = { mode = if (it == 0) StreamMode.AUDIO_ONLY else StreamMode.VIDEO }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(mode) }) { Text("Continue") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val extended = LocalExtendedColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message, color = extended.textSecondary) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text("Delete", color = extended.danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
