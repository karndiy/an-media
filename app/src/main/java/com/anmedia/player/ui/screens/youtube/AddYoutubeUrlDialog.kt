package com.anmedia.player.ui.screens.youtube

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.anmedia.player.domain.model.StreamMode
import com.anmedia.player.ui.components.SegmentedControl
import com.anmedia.player.ui.screens.library.YoutubeAddState
import com.anmedia.player.ui.theme.LocalExtendedColors

@Composable
fun AddYoutubeUrlDialog(
    state: YoutubeAddState,
    onDismiss: () -> Unit,
    onAdd: (url: String, mode: StreamMode) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(StreamMode.AUDIO_ONLY) }
    val extended = LocalExtendedColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add YouTube URL") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Paste a video or playlist URL. The stream is resolved and cached for replay.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.textSecondary
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("youtube.com/watch?v=...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SegmentedControl(
                    options = listOf("Audio only", "Video"),
                    selectedIndex = if (mode == StreamMode.AUDIO_ONLY) 0 else 1,
                    onSelect = { mode = if (it == 0) StreamMode.AUDIO_ONLY else StreamMode.VIDEO }
                )
                when (state) {
                    is YoutubeAddState.Loading -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                        Text("Resolving stream...", color = extended.textSecondary)
                    }
                    is YoutubeAddState.Error -> Text(state.message, color = extended.danger)
                    is YoutubeAddState.Success -> Text(state.message, color = extended.success)
                    YoutubeAddState.Idle -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(url, mode) }, enabled = url.isNotBlank() && state !is YoutubeAddState.Loading) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
