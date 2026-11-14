package com.rigel.mp3p.ui

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.rigel.mp3p.data.Album
import com.rigel.mp3p.data.MediaStoreScanner
import com.rigel.mp3p.data.Playlist
import com.rigel.mp3p.data.Song
import com.rigel.mp3p.service.MusicService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MusicUiState(
    val songs: List<Song> = emptyList(),
    val filteredSongs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val favoriteSongIds: Set<Long> = emptySet(),
    val currentSong: Song? = null,
    val currentQueue: List<Song> = emptyList(),
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF, // REPEAT_MODE_OFF, REPEAT_MODE_ALL, REPEAT_MODE_ONE
    val playbackSpeed: Float = 1.0f,
    val searchQuery: String = "",
    val selectedTab: Int = 0, // 0: Songs, 1: Albums, 2: Playlists, 3: Favorites
    val isFullPlayerVisible: Boolean = false,
    val isQueueVisible: Boolean = false,
    val isSleepTimerDialogVisible: Boolean = false,
    val isCreatePlaylistDialogVisible: Boolean = false,
    val sleepTimerMinutesLeft: Int? = null,
    val appTheme: Int = 0, // 0: Purple, 1: Cyan, 2: Coral, 3: Emerald
    val isLoading: Boolean = true
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private val mediaStoreScanner = MediaStoreScanner(application)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private var progressUpdateJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        initMediaController()
        loadSongs()
    }

    private fun initMediaController() {
        val sessionToken = SessionToken(
            getApplication(),
            ComponentName(getApplication(), MusicService::class.java)
        )
        controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupPlayerListener()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId
                val foundSong = _uiState.value.songs.find { it.id.toString() == mediaId || it.contentUri.toString() == mediaId }
                _uiState.update {
                    it.copy(
                        currentSong = foundSong,
                        duration = mediaController?.duration?.coerceAtLeast(0L) ?: foundSong?.duration ?: 0L
                    )
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _uiState.update {
                        it.copy(
                            duration = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
                        )
                    }
                }
            }
        })
    }

    fun loadSongs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val scannedSongs = mediaStoreScanner.scanAudioFiles()

            val albumMap = scannedSongs.groupBy { it.album }
                .toList()
                .mapIndexed { index, (albumName, songs) ->
                    Album(
                        id = index.toLong(),
                        name = albumName,
                        artist = songs.firstOrNull()?.artist ?: "Unknown Artist",
                        songCount = songs.size,
                        albumArtUri = songs.firstOrNull()?.albumArtUri,
                        songs = songs
                    )
                }

            _uiState.update { state ->
                state.copy(
                    songs = scannedSongs,
                    filteredSongs = filterSongsList(scannedSongs, state.searchQuery),
                    albums = albumMap,
                    isLoading = false
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredSongs = filterSongsList(state.songs, query)
            )
        }
    }

    private fun filterSongsList(songs: List<Song>, query: String): List<Song> {
        if (query.isBlank()) return songs
        val q = query.lowercase().trim()
        return songs.filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.album.lowercase().contains(q)
        }
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun playSong(song: Song, queue: List<Song> = _uiState.value.songs) {
        val controller = mediaController ?: return
        val index = queue.indexOf(song).coerceAtLeast(0)

        val mediaItems = queue.map { songItem ->
            val metadata = MediaMetadata.Builder()
                .setTitle(songItem.title)
                .setArtist(songItem.artist)
                .setAlbumTitle(songItem.album)
                .setArtworkUri(songItem.albumArtUri)
                .build()

            MediaItem.Builder()
                .setMediaId(songItem.id.toString())
                .setUri(songItem.contentUri)
                .setMediaMetadata(metadata)
                .build()
        }

        controller.setMediaItems(mediaItems, index, 0L)
        controller.prepare()
        controller.play()

        _uiState.update {
            it.copy(
                currentSong = song,
                currentQueue = queue,
                isPlaying = true,
                duration = song.duration
            )
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.mediaItemCount == 0 && _uiState.value.songs.isNotEmpty()) {
                val songToPlay = _uiState.value.currentSong ?: _uiState.value.songs.first()
                playSong(songToPlay)
            } else {
                controller.play()
            }
        }
    }

    fun nextSong() {
        mediaController?.seekToNextMediaItem()
    }

    fun previousSong() {
        mediaController?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _uiState.update { it.copy(currentPosition = positionMs) }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val newShuffle = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = newShuffle
        _uiState.update { it.copy(isShuffleEnabled = newShuffle) }
    }

    fun toggleRepeatMode() {
        val controller = mediaController ?: return
        val nextMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
        _uiState.update { it.copy(repeatMode = nextMode) }
    }

    fun setPlaybackSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun toggleFavorite(songId: Long) {
        _uiState.update { state ->
            val updated = state.favoriteSongIds.toMutableSet()
            if (updated.contains(songId)) {
                updated.remove(songId)
            } else {
                updated.add(songId)
            }
            state.copy(favoriteSongIds = updated)
        }
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _uiState.update { it.copy(sleepTimerMinutesLeft = minutes, isSleepTimerDialogVisible = false) }

        sleepTimerJob = viewModelScope.launch {
            var remainingSeconds = minutes * 60
            while (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
                val min = (remainingSeconds / 60) + if (remainingSeconds % 60 > 0) 1 else 0
                _uiState.update { it.copy(sleepTimerMinutesLeft = min) }
            }
            // Pause playback when timer expires
            mediaController?.pause()
            _uiState.update { it.copy(sleepTimerMinutesLeft = null) }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _uiState.update { it.copy(sleepTimerMinutesLeft = null) }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        val newPlaylist = Playlist(
            id = System.currentTimeMillis().toString(),
            name = name,
            songs = emptyList()
        )
        _uiState.update { state ->
            state.copy(
                playlists = state.playlists + newPlaylist,
                isCreatePlaylistDialogVisible = false
            )
        }
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        _uiState.update { state ->
            val updatedPlaylists = state.playlists.map { playlist ->
                if (playlist.id == playlistId && !playlist.songs.contains(song)) {
                    playlist.copy(songs = playlist.songs + song)
                } else {
                    playlist
                }
            }
            state.copy(playlists = updatedPlaylists)
        }
    }

    fun setFullPlayerVisible(visible: Boolean) {
        _uiState.update { it.copy(isFullPlayerVisible = visible) }
    }

    fun setQueueVisible(visible: Boolean) {
        _uiState.update { it.copy(isQueueVisible = visible) }
    }

    fun setSleepTimerDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isSleepTimerDialogVisible = visible) }
    }

    fun setCreatePlaylistDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isCreatePlaylistDialogVisible = visible) }
    }

    fun setAppTheme(themeIndex: Int) {
        _uiState.update { it.copy(appTheme = themeIndex) }
    }

    private fun startProgressTracker() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (true) {
                mediaController?.let { controller ->
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.coerceAtLeast(0L)
                    _uiState.update {
                        it.copy(
                            currentPosition = pos,
                            duration = if (dur > 0) dur else it.duration
                        )
                    }
                }
                delay(250L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressUpdateJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        progressUpdateJob?.cancel()
        sleepTimerJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
