package com.anmedia.player.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.anmedia.player.data.local.entity.MediaItemEntity
import com.anmedia.player.data.local.entity.PlaylistEntity
import com.anmedia.player.domain.youtube.YoutubeSearchResult
import com.anmedia.player.ui.components.AppSearchField
import com.anmedia.player.ui.components.ReorderableList
import com.anmedia.player.ui.components.SegmentedControl
import com.anmedia.player.ui.components.formatTime
import com.anmedia.player.ui.screens.youtube.AddYoutubeUrlDialog
import com.anmedia.player.ui.theme.LocalExtendedColors

private val TAB_TITLES = listOf("Local", "Video", "Playlists", "YouTube")

@Composable
fun LibraryScreen(viewModel: LibraryViewModel, modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var showAddYoutubeDialog by remember { mutableStateOf(false) }
    var addToPlaylistTarget by remember { mutableStateOf<MediaItemEntity?>(null) }
    var pendingDeleteYoutubeItem by remember { mutableStateOf<MediaItemEntity?>(null) }
    var pendingSearchResultModePick by remember { mutableStateOf<YoutubeSearchResult?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        SegmentedControl(
            options = TAB_TITLES,
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        if (selectedTab != 2) {
            AppSearchField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = "Search library",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        when (selectedTab) {
            0 -> LocalAudioTab(viewModel, onAddToPlaylist = { addToPlaylistTarget = it })
            1 -> LocalVideoTab(viewModel, onAddToPlaylist = { addToPlaylistTarget = it })
            2 -> PlaylistsTab(viewModel)
            3 -> YoutubeTab(
                viewModel,
                onAddClicked = { showAddYoutubeDialog = true },
                onAddToPlaylist = { addToPlaylistTarget = it },
                onDeleteItem = { pendingDeleteYoutubeItem = it },
                onSearchResultAddToPlaylist = { pendingSearchResultModePick = it }
            )
        }
    }

    if (showAddYoutubeDialog) {
        val addState by viewModel.youtubeAddState.collectAsStateWithLifecycle()
        AddYoutubeUrlDialog(
            state = addState,
            onDismiss = {
                showAddYoutubeDialog = false
                viewModel.resetYoutubeAddState()
            },
            onAdd = { url, mode -> viewModel.addYoutubeUrl(url, mode) }
        )
    }

    addToPlaylistTarget?.let { target ->
        val playlists by viewModel.playlists.collectAsStateWithLifecycle()
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { addToPlaylistTarget = null },
            onAddToExisting = { playlistId -> viewModel.addToPlaylist(playlistId, target.id) },
            onCreateAndAdd = { name -> viewModel.createPlaylistAndAdd(name, target.id) }
        )
    }

    pendingDeleteYoutubeItem?.let { item ->
        ConfirmDeleteDialog(
            title = "Delete YouTube video?",
            message = "\"${item.title}\" will be removed from your library and any playlists.",
            onConfirm = { viewModel.deleteYoutubeItem(item.id) },
            onDismiss = { pendingDeleteYoutubeItem = null }
        )
    }

    pendingSearchResultModePick?.let { result ->
        ChooseStreamModeDialog(
            videoTitle = result.title,
            onDismiss = { pendingSearchResultModePick = null },
            onConfirm = { mode ->
                viewModel.prepareSearchResultForPlaylist(result, mode) { entity -> addToPlaylistTarget = entity }
                pendingSearchResultModePick = null
            }
        )
    }
}

@Composable
private fun LocalAudioTab(viewModel: LibraryViewModel, onAddToPlaylist: (MediaItemEntity) -> Unit) {
    val items by viewModel.localAudio.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = viewModel::rescanLibrary) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Rescan device")
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items, key = { it.id }) { entity ->
                MediaRow(
                    entity = entity,
                    onClick = { viewModel.playLocalAudio(entity) },
                    onAddToPlaylist = { onAddToPlaylist(entity) }
                )
            }
        }
    }
}

@Composable
private fun LocalVideoTab(viewModel: LibraryViewModel, onAddToPlaylist: (MediaItemEntity) -> Unit) {
    val items by viewModel.localVideo.collectAsStateWithLifecycle()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items, key = { it.id }) { entity ->
            MediaRow(
                entity = entity,
                onClick = { viewModel.playLocalVideo(entity) },
                onAddToPlaylist = { onAddToPlaylist(entity) }
            )
        }
    }
}

@Composable
private fun YoutubeTab(
    viewModel: LibraryViewModel,
    onAddClicked: () -> Unit,
    onAddToPlaylist: (MediaItemEntity) -> Unit,
    onDeleteItem: (MediaItemEntity) -> Unit,
    onSearchResultAddToPlaylist: (YoutubeSearchResult) -> Unit
) {
    val extended = LocalExtendedColors.current
    val items by viewModel.youtubeQueue.collectAsStateWithLifecycle()
    val searchQuery by viewModel.youtubeSearchQuery.collectAsStateWithLifecycle()
    val searchState by viewModel.youtubeSearchState.collectAsStateWithLifecycle()
    val resolvingUrl by viewModel.resolvingSearchUrl.collectAsStateWithLifecycle()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onAddClicked) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Add YouTube URL")
                }
            }
        }

        item {
            AppSearchField(
                value = searchQuery,
                onValueChange = viewModel::setYoutubeSearchQuery,
                placeholder = "Search YouTube",
                onSearch = viewModel::searchYoutube,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        when (val state = searchState) {
            is YoutubeSearchUiState.Loading -> item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            }
            is YoutubeSearchUiState.Error -> item {
                Text(
                    state.message,
                    color = extended.danger,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            is YoutubeSearchUiState.Success -> {
                item {
                    Text(
                        if (state.results.isEmpty()) "No results found." else "Search results",
                        style = MaterialTheme.typography.labelLarge,
                        color = extended.textSecondary,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp)
                    )
                }
                items(state.results, key = { it.url }) { result ->
                    YoutubeSearchResultRow(
                        result = result,
                        isResolving = resolvingUrl == result.url,
                        onClick = { viewModel.playYoutubeSearchResult(result) },
                        onAddToPlaylist = { onSearchResultAddToPlaylist(result) }
                    )
                }
            }
            YoutubeSearchUiState.Idle -> Unit
        }

        item {
            Text(
                "Your YouTube videos",
                style = MaterialTheme.typography.labelLarge,
                color = extended.textSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 2.dp)
            )
        }

        if (items.isEmpty()) {
            item {
                Text(
                    "No YouTube videos added yet.",
                    color = extended.textSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else {
            items(items, key = { it.id }) { entity ->
                MediaRow(
                    entity = entity,
                    onClick = { viewModel.playYoutubeQueueFrom(entity) },
                    onAddToPlaylist = { onAddToPlaylist(entity) },
                    onDelete = { onDeleteItem(entity) }
                )
            }
        }
    }
}

@Composable
private fun YoutubeSearchResultRow(
    result: YoutubeSearchResult,
    isResolving: Boolean,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val extended = LocalExtendedColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isResolving, onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (result.thumbnailUrl != null) {
            AsyncImage(
                model = result.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {}
        }
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(result.title, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
            Text(
                listOfNotNull(result.channelName, formatTime(result.durationSeconds * 1000)).joinToString(" · "),
                maxLines = 1,
                color = extended.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (isResolving) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 10.dp),
                strokeWidth = 2.dp
            )
        } else {
            IconButton(onClick = onAddToPlaylist) {
                Icon(
                    Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = "Add to playlist",
                    tint = extended.textSecondary
                )
            }
        }
    }
}

@Composable
private fun PlaylistsTab(viewModel: LibraryViewModel) {
    val extended = LocalExtendedColors.current
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedPlaylistId.collectAsStateWithLifecycle()
    val selectedItems by viewModel.selectedPlaylistItems.collectAsStateWithLifecycle()
    var newPlaylistName by remember { mutableStateOf("") }
    var pendingDeletePlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var pendingRemoveItem by remember { mutableStateOf<MediaItemEntity?>(null) }

    if (selectedId == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("New playlist name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    viewModel.createPlaylist(newPlaylistName)
                    newPlaylistName = ""
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Create playlist")
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(playlists, key = PlaylistEntity::id) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectPlaylist(playlist.id) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = extended.accent)
                            Text(text = "  ${playlist.name}")
                        }
                        IconButton(onClick = { pendingDeletePlaylist = playlist }) {
                            Icon(Icons.Filled.Close, contentDescription = "Delete playlist")
                        }
                    }
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                IconButton(onClick = { viewModel.selectPlaylist(null) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = playlists.firstOrNull { it.id == selectedId }?.name ?: "Playlist",
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            ReorderableList(
                items = selectedItems,
                key = { it.id },
                onMove = viewModel::moveInSelectedPlaylist,
                onDragEnd = { },
                modifier = Modifier.fillMaxSize()
            ) { entity, isDragging ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.playPlaylistFrom(entity) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = if (isDragging) extended.accent else extended.textSecondary
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(entity.title, maxLines = 1)
                            Text(
                                entity.artist ?: entity.channelName ?: "",
                                maxLines = 1,
                                color = extended.textSecondary
                            )
                        }
                    }
                    IconButton(onClick = { pendingRemoveItem = entity }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove from playlist")
                    }
                }
            }
        }
    }

    pendingDeletePlaylist?.let { playlist ->
        ConfirmDeleteDialog(
            title = "Delete playlist?",
            message = "\"${playlist.name}\" will be removed. This can't be undone.",
            onConfirm = { viewModel.deletePlaylist(playlist.id) },
            onDismiss = { pendingDeletePlaylist = null }
        )
    }

    pendingRemoveItem?.let { item ->
        val playlistId = selectedId
        ConfirmDeleteDialog(
            title = "Remove from playlist?",
            message = "\"${item.title}\" will be removed from this playlist.",
            onConfirm = { if (playlistId != null) viewModel.removeFromPlaylist(playlistId, item.id) },
            onDismiss = { pendingRemoveItem = null }
        )
    }
}

@Composable
private fun MediaRow(
    entity: MediaItemEntity,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val extended = LocalExtendedColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (entity.thumbnailUrl != null) {
            AsyncImage(
                model = entity.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {}
        }
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(entity.title, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
            Text(
                entity.artist ?: entity.channelName ?: entity.album ?: "",
                maxLines = 1,
                color = extended.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        IconButton(onClick = onAddToPlaylist) {
            Icon(
                Icons.AutoMirrored.Filled.PlaylistAdd,
                contentDescription = "Add to playlist",
                tint = extended.textSecondary
            )
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Delete",
                    tint = extended.danger
                )
            }
        }
    }
}
