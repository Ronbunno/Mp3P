package com.rigel.mp3p

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import coil.compose.SubcomposeAsyncImage
import com.rigel.mp3p.data.Album
import com.rigel.mp3p.data.Playlist
import com.rigel.mp3p.data.Song
import com.rigel.mp3p.ui.MusicViewModel
import com.rigel.mp3p.ui.components.CreatePlaylistDialog
import com.rigel.mp3p.ui.components.FullNowPlayingScreen
import com.rigel.mp3p.ui.components.MiniPlayer
import com.rigel.mp3p.ui.components.QueueBottomSheet
import com.rigel.mp3p.ui.components.SleepTimerDialog
import com.rigel.mp3p.ui.components.SongItem
import com.rigel.mp3p.ui.components.SongListBottomSheet
import com.rigel.mp3p.ui.theme.Mp3PTheme

class MainActivity : ComponentActivity() {

    private val musicViewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by musicViewModel.uiState.collectAsState()
            Mp3PTheme(appTheme = uiState.appTheme) {
                MainScreen(viewModel = musicViewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MusicViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var songForPlaylistSelection by remember { mutableStateOf<Song?>(null) }
    var selectedAlbumForSongs by remember { mutableStateOf<Album?>(null) }
    var selectedPlaylistForSongs by remember { mutableStateOf<Playlist?>(null) }
    var isAboutDialogVisible by remember { mutableStateOf(false) }

    // Permission handling
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    var hasPermission by remember {
        mutableStateOf(
            permissionsToRequest.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.all { it }
        if (hasPermission) {
            viewModel.loadSongs()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(top = 28.dp)
            ) {
                // Header & Compact Search
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.mp3p),
                        contentDescription = "Mp3P App Logo",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Mp3P",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    var isThemeMenuExpanded by remember { mutableStateOf(false) }

                    Box {
                        IconButton(
                            onClick = { isThemeMenuExpanded = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Change Color Theme",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = isThemeMenuExpanded,
                            onDismissRequest = { isThemeMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Neon Purple") },
                                onClick = { viewModel.setAppTheme(0); isThemeMenuExpanded = false },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFC084FC))
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Electric Cyan") },
                                onClick = { viewModel.setAppTheme(1); isThemeMenuExpanded = false },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22D3EE))
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sunset Coral") },
                                onClick = { viewModel.setAppTheme(2); isThemeMenuExpanded = false },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFB7185))
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Emerald Green") },
                                onClick = { viewModel.setAppTheme(3); isThemeMenuExpanded = false },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF34D399))
                                    )
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = { isAboutDialogVisible = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About App",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        placeholder = { 
                            Text(
                                "Search songs...", 
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                            ) 
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.setSearchQuery("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)
                        )
                    )
                }

                // Storage Permission Warning Banner if missing
                if (!hasPermission) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Demo Mode Active",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Grant storage permission to scan your local MP3 files.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Button(onClick = { permissionLauncher.launch(permissionsToRequest) }) {
                                Text("Grant")
                            }
                        }
                    }
                }

                // Category Tabs (Songs, Albums, Playlists, Favorites)
                val tabs = listOf(
                    "Songs (${uiState.songs.size})" to Icons.Default.LibraryMusic,
                    "Albums (${uiState.albums.size})" to Icons.Default.Album,
                    "Playlists (${uiState.playlists.size})" to Icons.Default.PlaylistPlay,
                    "Favorites (${uiState.favoriteSongIds.size})" to Icons.Default.Favorite
                )

                ScrollableTabRow(
                    selectedTabIndex = uiState.selectedTab,
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, (title, icon) ->
                        Tab(
                            selected = uiState.selectedTab == index,
                            onClick = { viewModel.setSelectedTab(index) },
                            text = { Text(title) },
                            icon = { Icon(imageVector = icon, contentDescription = null) }
                        )
                    }
                }
            }
        },
        bottomBar = {
            MiniPlayer(
                song = uiState.currentSong,
                isPlaying = uiState.isPlaying,
                currentPosition = uiState.currentPosition,
                duration = uiState.duration,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onNextClick = { viewModel.nextSong() },
                onExpandPlayer = { viewModel.setFullPlayerVisible(true) },
                onQueueClick = { viewModel.setQueueVisible(true) }
            )
        },
        floatingActionButton = {
            if (uiState.selectedTab == 2) {
                FloatingActionButton(
                    onClick = { viewModel.setCreatePlaylistDialogVisible(true) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Create Playlist")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                when (uiState.selectedTab) {
                    // 0: Songs List
                    0 -> {
                        val displaySongs = uiState.filteredSongs
                        if (displaySongs.isEmpty()) {
                            EmptyStateMessage(
                                message = if (uiState.searchQuery.isNotEmpty()) "No songs found matching '${uiState.searchQuery}'" else "No audio files found"
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                items(displaySongs, key = { it.id }) { song ->
                                    val isSelected = uiState.currentSong?.id == song.id
                                    SongItem(
                                        song = song,
                                        isSelected = isSelected,
                                        isPlaying = uiState.isPlaying && isSelected,
                                        isFavorite = uiState.favoriteSongIds.contains(song.id),
                                        onSongClick = { viewModel.playSong(song, displaySongs) },
                                        onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                                        onAddToPlaylistClick = { songForPlaylistSelection = song }
                                    )
                                }
                            }
                        }
                    }

                    // 1: Albums Grid
                    1 -> {
                        if (uiState.albums.isEmpty()) {
                            EmptyStateMessage(message = "No albums found")
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.albums, key = { it.id }) { album ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedAlbumForSongs = album
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                    ) {
                                        Column {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Album,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(48.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (album.albumArtUri != null) {
                                                    SubcomposeAsyncImage(
                                                        model = album.albumArtUri,
                                                        contentDescription = album.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize(),
                                                        loading = {
                                                            Box(
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Album,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(48.dp),
                                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        },
                                                        error = {
                                                            Box(
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Album,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(48.dp),
                                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = album.name,
                                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${album.artist} • ${album.songCount} songs",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2: Playlists View
                    2 -> {
                        if (uiState.playlists.isEmpty()) {
                            EmptyStateMessage(message = "No playlists created yet. Tap '+' to create one!")
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.playlists, key = { it.id }) { playlist ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedPlaylistForSongs = playlist
                                            },
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.FolderSpecial,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = playlist.name,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = "${playlist.songs.size} tracks",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3: Favorites List
                    3 -> {
                        val favoriteSongs = uiState.songs.filter { uiState.favoriteSongIds.contains(it.id) }
                        if (favoriteSongs.isEmpty()) {
                            EmptyStateMessage(message = "No favorite songs yet. Tap the heart icon on any song!")
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                items(favoriteSongs, key = { it.id }) { song ->
                                    val isSelected = uiState.currentSong?.id == song.id
                                    SongItem(
                                        song = song,
                                        isSelected = isSelected,
                                        isPlaying = uiState.isPlaying && isSelected,
                                        isFavorite = true,
                                        onSongClick = { viewModel.playSong(song, favoriteSongs) },
                                        onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                                        onAddToPlaylistClick = { songForPlaylistSelection = song }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Full Now Playing Screen
    if (uiState.isFullPlayerVisible) {
        FullNowPlayingScreen(
            song = uiState.currentSong,
            isPlaying = uiState.isPlaying,
            currentPosition = uiState.currentPosition,
            duration = uiState.duration,
            isShuffleEnabled = uiState.isShuffleEnabled,
            repeatMode = uiState.repeatMode,
            playbackSpeed = uiState.playbackSpeed,
            isFavorite = uiState.currentSong?.let { uiState.favoriteSongIds.contains(it.id) } ?: false,
            sleepTimerMinutesLeft = uiState.sleepTimerMinutesLeft,
            onDismiss = { viewModel.setFullPlayerVisible(false) },
            onPlayPauseClick = { viewModel.togglePlayPause() },
            onNextClick = { viewModel.nextSong() },
            onPreviousClick = { viewModel.previousSong() },
            onSeekTo = { viewModel.seekTo(it) },
            onToggleShuffle = { viewModel.toggleShuffle() },
            onToggleRepeat = { viewModel.toggleRepeatMode() },
            onToggleFavorite = { uiState.currentSong?.let { viewModel.toggleFavorite(it.id) } },
            onSetPlaybackSpeed = { viewModel.setPlaybackSpeed(it) },
            onOpenSleepTimerDialog = { viewModel.setSleepTimerDialogVisible(true) },
            onOpenQueue = { viewModel.setQueueVisible(true) }
        )
    }

    // Queue Bottom Sheet
    if (uiState.isQueueVisible) {
        QueueBottomSheet(
            queue = uiState.currentQueue,
            currentSong = uiState.currentSong,
            isPlaying = uiState.isPlaying,
            favoriteSongIds = uiState.favoriteSongIds,
            onDismiss = { viewModel.setQueueVisible(false) },
            onSongClick = { viewModel.playSong(it, uiState.currentQueue) },
            onFavoriteClick = { viewModel.toggleFavorite(it.id) }
        )
    }

    // Sleep Timer Dialog
    if (uiState.isSleepTimerDialogVisible) {
        SleepTimerDialog(
            currentMinutesLeft = uiState.sleepTimerMinutesLeft,
            onDismiss = { viewModel.setSleepTimerDialogVisible(false) },
            onSelectMinutes = { viewModel.startSleepTimer(it) },
            onCancelTimer = { viewModel.cancelSleepTimer() }
        )
    }

    // Create Playlist Dialog
    if (uiState.isCreatePlaylistDialogVisible) {
        CreatePlaylistDialog(
            onDismiss = { viewModel.setCreatePlaylistDialogVisible(false) },
            onCreatePlaylist = { viewModel.createPlaylist(it) }
        )
    }

    // Select Playlist Dialog when adding a song to a playlist
    if (songForPlaylistSelection != null) {
        AlertDialog(
            onDismissRequest = { songForPlaylistSelection = null },
            title = { Text("Add to Playlist") },
            text = {
                if (uiState.playlists.isEmpty()) {
                    Text("No playlists found. Create one first!")
                } else {
                    Column {
                        uiState.playlists.forEach { playlist ->
                            TextButton(
                                onClick = {
                                    viewModel.addSongToPlaylist(playlist.id, songForPlaylistSelection!!)
                                    songForPlaylistSelection = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = playlist.name,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { songForPlaylistSelection = null }) {
                    Text("Close")
                }
            }
        )
    }

    selectedAlbumForSongs?.let { album ->
        SongListBottomSheet(
            title = album.name,
            subtitle = "${album.artist} • ${album.songCount} songs",
            songs = album.songs,
            currentSong = uiState.currentSong,
            isPlaying = uiState.isPlaying,
            favoriteSongIds = uiState.favoriteSongIds,
            onDismiss = { selectedAlbumForSongs = null },
            onSongClick = { song -> viewModel.playSong(song, album.songs) },
            onFavoriteClick = { song -> viewModel.toggleFavorite(song.id) }
        )
    }

    selectedPlaylistForSongs?.let { playlist ->
        SongListBottomSheet(
            title = playlist.name,
            subtitle = "${playlist.songs.size} songs",
            songs = playlist.songs,
            currentSong = uiState.currentSong,
            isPlaying = uiState.isPlaying,
            favoriteSongIds = uiState.favoriteSongIds,
            onDismiss = { selectedPlaylistForSongs = null },
            onSongClick = { song -> viewModel.playSong(song, playlist.songs) },
            onFavoriteClick = { song -> viewModel.toggleFavorite(song.id) }
        )
    }

    if (isAboutDialogVisible) {
        AlertDialog(
            onDismissRequest = { isAboutDialogVisible = false },
            icon = {
                Image(
                    painter = painterResource(id = R.drawable.mp3p),
                    contentDescription = "Mp3P Logo",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            },
            title = {
                Text(
                    text = "Mp3P",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Created by ronbunno",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "© 2025 Mp3P • All rights reserved",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { isAboutDialogVisible = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Created by ronbunno",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}
