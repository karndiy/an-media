package com.anmedia.player.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anmedia.player.data.local.entity.MediaItemEntity
import com.anmedia.player.data.local.entity.PlaylistEntity
import com.anmedia.player.data.repository.MediaRepository
import com.anmedia.player.data.repository.PlaylistRepository
import com.anmedia.player.domain.model.SourceType
import com.anmedia.player.domain.model.StreamMode
import com.anmedia.player.domain.youtube.YouTubeStreamResolver
import com.anmedia.player.domain.youtube.YoutubeResolveResult
import com.anmedia.player.domain.youtube.YoutubeSearchResult
import com.anmedia.player.domain.youtube.YoutubeUrlUtils
import com.anmedia.player.player.PlayerManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class YoutubeAddState {
    data object Idle : YoutubeAddState()
    data object Loading : YoutubeAddState()
    data class Success(val message: String) : YoutubeAddState()
    data class Error(val message: String) : YoutubeAddState()
}

sealed class YoutubeSearchUiState {
    data object Idle : YoutubeSearchUiState()
    data object Loading : YoutubeSearchUiState()
    data class Success(val results: List<YoutubeSearchResult>) : YoutubeSearchUiState()
    data class Error(val message: String) : YoutubeSearchUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val mediaRepository: MediaRepository,
    private val playlistRepository: PlaylistRepository,
    private val youTubeStreamResolver: YouTubeStreamResolver,
    private val playerManager: PlayerManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val localAudio: StateFlow<List<MediaItemEntity>> =
        combine(mediaRepository.observeLocalAudio(), _searchQuery, ::filterBySearch)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localVideo: StateFlow<List<MediaItemEntity>> =
        combine(mediaRepository.observeLocalVideo(), _searchQuery, ::filterBySearch)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val youtubeQueue: StateFlow<List<MediaItemEntity>> =
        combine(mediaRepository.observeYoutubeItems(), _searchQuery, ::filterBySearch)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> =
        playlistRepository.observePlaylists()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId: StateFlow<Long?> = _selectedPlaylistId

    val selectedPlaylistItems: StateFlow<List<MediaItemEntity>> = _selectedPlaylistId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else playlistRepository.observePlaylistItems(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _youtubeAddState = MutableStateFlow<YoutubeAddState>(YoutubeAddState.Idle)
    val youtubeAddState: StateFlow<YoutubeAddState> = _youtubeAddState

    private val _youtubeSearchQuery = MutableStateFlow("")
    val youtubeSearchQuery: StateFlow<String> = _youtubeSearchQuery

    private val _youtubeSearchState = MutableStateFlow<YoutubeSearchUiState>(YoutubeSearchUiState.Idle)
    val youtubeSearchState: StateFlow<YoutubeSearchUiState> = _youtubeSearchState

    private val _resolvingSearchUrl = MutableStateFlow<String?>(null)
    val resolvingSearchUrl: StateFlow<String?> = _resolvingSearchUrl

    private fun filterBySearch(items: List<MediaItemEntity>, query: String): List<MediaItemEntity> =
        if (query.isBlank()) {
            items
        } else {
            items.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.artist?.contains(query, ignoreCase = true) == true ||
                    it.channelName?.contains(query, ignoreCase = true) == true
            }
        }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectPlaylist(id: Long?) {
        _selectedPlaylistId.value = id
    }

    fun rescanLibrary() = viewModelScope.launch { mediaRepository.rescanLocalLibrary() }

    fun playLocalAudio(startEntity: MediaItemEntity) {
        val queue = localAudio.value
        playerManager.playQueue(queue, queue.indexOfFirst { it.id == startEntity.id }.coerceAtLeast(0))
    }

    fun playLocalVideo(entity: MediaItemEntity) {
        playerManager.playQueue(listOf(entity), 0)
    }

    fun playYoutubeQueueFrom(entity: MediaItemEntity) {
        val queue = youtubeQueue.value
        playerManager.playQueue(queue, queue.indexOfFirst { it.id == entity.id }.coerceAtLeast(0))
    }

    fun playPlaylistFrom(entity: MediaItemEntity) {
        val queue = selectedPlaylistItems.value
        playerManager.playQueue(queue, queue.indexOfFirst { it.id == entity.id }.coerceAtLeast(0))
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { playlistRepository.createPlaylist(name) }
    }

    fun deletePlaylist(id: Long) = viewModelScope.launch {
        playlistRepository.deletePlaylist(id)
        if (_selectedPlaylistId.value == id) _selectedPlaylistId.value = null
    }

    fun addToPlaylist(playlistId: Long, mediaId: Long) = viewModelScope.launch {
        playlistRepository.addToPlaylist(playlistId, mediaId)
    }

    fun removeFromPlaylist(playlistId: Long, mediaId: Long) = viewModelScope.launch {
        playlistRepository.removeFromPlaylist(playlistId, mediaId)
    }

    fun moveInSelectedPlaylist(fromIndex: Int, toIndex: Int) {
        val playlistId = _selectedPlaylistId.value ?: return
        val current = selectedPlaylistItems.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val moved = current.removeAt(fromIndex)
        current.add(toIndex, moved)
        viewModelScope.launch { playlistRepository.reorder(playlistId, current.map { it.id }) }
    }

    fun addYoutubeUrl(url: String, streamMode: StreamMode) {
        val trimmed = url.trim()
        if (trimmed.isBlank() || !YoutubeUrlUtils.isYoutubeUrl(trimmed)) {
            _youtubeAddState.value = YoutubeAddState.Error("Enter a valid YouTube URL")
            return
        }
        viewModelScope.launch {
            _youtubeAddState.value = YoutubeAddState.Loading
            val queueId = playlistRepository.getOrCreateYoutubeQueue()

            if (YoutubeUrlUtils.isPlaylistUrl(trimmed)) {
                when (val result = youTubeStreamResolver.resolvePlaylist(trimmed)) {
                    is YoutubeResolveResult.PlaylistSuccess -> {
                        var added = 0
                        var lastError: String? = null
                        for (videoUrl in result.playlist.videoUrls) {
                            addSingleYoutubeVideo(videoUrl, streamMode, queueId)
                                .onSuccess { added++ }
                                .onFailure { lastError = it.message }
                        }
                        _youtubeAddState.value = if (added > 0) {
                            YoutubeAddState.Success(
                                "Added $added video(s) from \"${result.playlist.name}\""
                            )
                        } else {
                            YoutubeAddState.Error(lastError ?: "Could not resolve any video in that playlist")
                        }
                    }
                    is YoutubeResolveResult.Error ->
                        _youtubeAddState.value = YoutubeAddState.Error(result.message)
                    else -> Unit
                }
            } else {
                addSingleYoutubeVideo(trimmed, streamMode, queueId).fold(
                    onSuccess = { _youtubeAddState.value = YoutubeAddState.Success("Added to YouTube queue") },
                    onFailure = { _youtubeAddState.value = YoutubeAddState.Error(it.message ?: "Could not resolve that video") }
                )
            }
        }
    }

    private suspend fun addSingleYoutubeVideo(url: String, mode: StreamMode, queueId: Long): Result<Long> {
        return resolveAndStoreYoutubeItem(url, mode).onSuccess { id ->
            playlistRepository.addToPlaylist(queueId, id)
        }
    }

    private suspend fun resolveAndStoreYoutubeItem(url: String, mode: StreamMode): Result<Long> {
        return when (val result = youTubeStreamResolver.resolve(url, mode == StreamMode.AUDIO_ONLY)) {
            is YoutubeResolveResult.Success -> {
                val stream = result.stream
                val entity = MediaItemEntity(
                    title = stream.title,
                    channelName = stream.channelName,
                    durationMs = stream.durationSeconds * 1000,
                    sourceUri = stream.originalUrl,
                    sourceType = SourceType.YOUTUBE,
                    thumbnailUrl = stream.thumbnailUrl,
                    bitrate = stream.bitrateKbps,
                    preferredStreamMode = mode
                )
                Result.success(mediaRepository.findOrCreateYoutubeItem(entity))
            }
            is YoutubeResolveResult.Error -> Result.failure(Exception(result.message, result.cause))
            else -> Result.failure(Exception("Expected a video"))
        }
    }

    fun deleteYoutubeItem(id: Long) = viewModelScope.launch {
        mediaRepository.deleteItem(id)
    }

    fun resetYoutubeAddState() {
        _youtubeAddState.value = YoutubeAddState.Idle
    }

    fun setYoutubeSearchQuery(query: String) {
        _youtubeSearchQuery.value = query
    }

    fun searchYoutube() {
        val query = _youtubeSearchQuery.value.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            _youtubeSearchState.value = YoutubeSearchUiState.Loading
            when (val result = youTubeStreamResolver.search(query, limit = 10)) {
                is YoutubeResolveResult.SearchSuccess ->
                    _youtubeSearchState.value = YoutubeSearchUiState.Success(result.results)
                is YoutubeResolveResult.Error ->
                    _youtubeSearchState.value = YoutubeSearchUiState.Error(result.message)
                else -> Unit
            }
        }
    }

    fun playYoutubeSearchResult(result: YoutubeSearchResult, mode: StreamMode = StreamMode.AUDIO_ONLY) {
        viewModelScope.launch {
            _resolvingSearchUrl.value = result.url
            val queueId = playlistRepository.getOrCreateYoutubeQueue()
            addSingleYoutubeVideo(result.url, mode, queueId).onSuccess { mediaId ->
                val queue = playlistRepository.getPlaylistItemsOnce(queueId)
                val index = queue.indexOfFirst { it.id == mediaId }.coerceAtLeast(0)
                playerManager.playQueue(queue, index)
            }
            _resolvingSearchUrl.value = null
        }
    }

    fun addYoutubeSearchResultToQueue(result: YoutubeSearchResult, mode: StreamMode = StreamMode.AUDIO_ONLY) {
        viewModelScope.launch {
            _resolvingSearchUrl.value = result.url
            val queueId = playlistRepository.getOrCreateYoutubeQueue()
            addSingleYoutubeVideo(result.url, mode, queueId)
            _resolvingSearchUrl.value = null
        }
    }

    /**
     * Resolves a search result into a stored YouTube media item (without adding it to the
     * implicit YouTube queue) and hands it back so the caller can show [AddToPlaylistDialog]
     * for it, same as any other already-stored media item.
     */
    fun prepareSearchResultForPlaylist(
        result: YoutubeSearchResult,
        mode: StreamMode = StreamMode.AUDIO_ONLY,
        onReady: (MediaItemEntity) -> Unit
    ) {
        viewModelScope.launch {
            _resolvingSearchUrl.value = result.url
            resolveAndStoreYoutubeItem(result.url, mode)
                .onSuccess { id -> mediaRepository.getById(id)?.let(onReady) }
                .onFailure { _youtubeSearchState.value = YoutubeSearchUiState.Error(it.message ?: "Could not resolve that video") }
            _resolvingSearchUrl.value = null
        }
    }

    fun createPlaylistAndAdd(name: String, mediaId: Long) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(name)
            playlistRepository.addToPlaylist(playlistId, mediaId)
        }
    }
}
