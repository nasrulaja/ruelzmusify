/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package moe.ruelz.ruelzmusify.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadService
import coil3.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.ruelz.ruelzmusify.LocalDatabase
import moe.ruelz.ruelzmusify.LocalDownloadUtil
import moe.ruelz.ruelzmusify.LocalPlayerConnection
import moe.ruelz.ruelzmusify.LocalSyncUtils
import moe.ruelz.ruelzmusify.R
import moe.ruelz.ruelzmusify.constants.ListThumbnailSize
import moe.ruelz.ruelzmusify.constants.SpeedDialSongIdsKey
import moe.ruelz.ruelzmusify.constants.ThumbnailCornerRadius
import moe.ruelz.ruelzmusify.db.entities.PlaylistEntity
import moe.ruelz.ruelzmusify.db.entities.PlaylistSongMap
import moe.ruelz.ruelzmusify.extensions.toMediaItem
import moe.ruelz.ruelzmusify.innertube.YouTube
import moe.ruelz.ruelzmusify.innertube.models.PlaylistItem
import moe.ruelz.ruelzmusify.innertube.models.SongItem
import moe.ruelz.ruelzmusify.innertube.utils.completed
import moe.ruelz.ruelzmusify.models.MediaMetadata
import moe.ruelz.ruelzmusify.models.toMediaMetadata
import moe.ruelz.ruelzmusify.playback.ExoDownloadService
import moe.ruelz.ruelzmusify.playback.queues.YouTubeQueue
import moe.ruelz.ruelzmusify.ui.component.DefaultDialog
import moe.ruelz.ruelzmusify.ui.component.ListDialog
import moe.ruelz.ruelzmusify.ui.component.MenuAction
import moe.ruelz.ruelzmusify.ui.component.MenuArtworkStyle
import moe.ruelz.ruelzmusify.ui.component.MenuFavoriteToggle
import moe.ruelz.ruelzmusify.ui.component.MenuHero
import moe.ruelz.ruelzmusify.ui.component.MenuPrimaryActions
import moe.ruelz.ruelzmusify.ui.component.MenuQuickActions
import moe.ruelz.ruelzmusify.ui.component.MenuSection
import moe.ruelz.ruelzmusify.ui.component.MenuSheetLayout
import moe.ruelz.ruelzmusify.ui.component.downloadAction
import moe.ruelz.ruelzmusify.ui.utils.HeaderDownloadItem
import moe.ruelz.ruelzmusify.ui.utils.sendAddMissingDownloads
import moe.ruelz.ruelzmusify.utils.SpeedDialPin
import moe.ruelz.ruelzmusify.utils.SpeedDialPinType
import moe.ruelz.ruelzmusify.utils.joinByBullet
import moe.ruelz.ruelzmusify.utils.makeTimeString
import moe.ruelz.ruelzmusify.utils.parseSpeedDialPins
import moe.ruelz.ruelzmusify.utils.rememberPreference
import moe.ruelz.ruelzmusify.utils.serializeSpeedDialPins
import moe.ruelz.ruelzmusify.utils.toggleSpeedDialPin
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun YouTubePlaylistMenu(
    playlist: PlaylistItem,
    songs: List<SongItem> = emptyList(),
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
    selectAction: () -> Unit = {},
    canSelect: Boolean = false,
    snackbarHostState: androidx.compose.material3.SnackbarHostState? = null,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val syncUtils = LocalSyncUtils.current
    val dbPlaylist by database.playlistByBrowseId(playlist.id).collectAsState(initial = null)
    val (speedDialSongIds, onSpeedDialSongIdsChange) = rememberPreference(SpeedDialSongIdsKey, "")
    val speedDialPins = remember(speedDialSongIds) { parseSpeedDialPins(speedDialSongIds) }
    val playlistPin =
        remember(dbPlaylist?.playlist?.id) {
            dbPlaylist?.playlist?.id?.let { SpeedDialPin(type = SpeedDialPinType.PLAYLIST, id = it) }
        }
    val isInSpeedDial =
        remember(speedDialPins, playlistPin) {
            playlistPin?.let { pin -> speedDialPins.any { it.type == pin.type && it.id == pin.id } } == true
        }

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showImportPlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showErrorPlaylistAddDialog by rememberSaveable { mutableStateOf(false) }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<MediaMetadata>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = {
            val allSongs =
                songs
                    .ifEmpty {
                        YouTube
                            .playlist(playlist.id)
                            .completed()
                            .getOrNull()
                            ?.songs
                            .orEmpty()
                    }.map {
                        it.toMediaMetadata()
                    }
            database.withTransaction {
                allSongs.forEach(::insert)
            }
            allSongs.map { it.id }
        },
        onDismiss = { showChoosePlaylistDialog = false },
        onAddComplete = { songCount, playlistNames ->
            val message =
                when {
                    songCount == 1 && playlistNames.size == 1 -> {
                        context.getString(R.string.added_to_playlist, playlistNames.first())
                    }

                    songCount > 1 && playlistNames.size == 1 -> {
                        context.getString(
                            R.string.added_n_songs_to_playlist,
                            songCount,
                            playlistNames.first(),
                        )
                    }

                    songCount == 1 -> {
                        context.getString(R.string.added_to_n_playlists, playlistNames.size)
                    }

                    else -> {
                        context.getString(R.string.added_n_songs_to_n_playlists, songCount, playlistNames.size)
                    }
                }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
    )

    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }
    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.id]?.state == Download.STATE_QUEUED ||
                            downloads[it.id]?.state == Download.STATE_DOWNLOADING ||
                            downloads[it.id]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }
    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }
    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text =
                        stringResource(
                            R.string.remove_download_playlist_confirm,
                            playlist.title,
                        ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = { showRemoveDownloadDialog = false },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false,
                            )
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    ImportPlaylistDialog(
        isVisible = showImportPlaylistDialog,
        onGetSong = {
            val allSongs =
                songs
                    .ifEmpty {
                        YouTube
                            .playlist(playlist.id)
                            .completed()
                            .getOrNull()
                            ?.songs
                            .orEmpty()
                    }.map {
                        it.toMediaMetadata()
                    }
            database.withTransaction {
                allSongs.forEach(::insert)
            }
            allSongs.map { it.id }
        },
        playlistTitle = playlist.title,
        browseId = playlist.id,
        snackbarHostState = snackbarHostState,
        onDismiss = { showImportPlaylistDialog = false },
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
        ) {
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.already_in_playlist)) },
                    leadingContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier = Modifier.clickable { showErrorPlaylistAddDialog = false },
                )
            }

            items(notAddedList) { song ->
                ListItem(
                    headlineContent = { Text(text = song.title) },
                    leadingContent = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(ListThumbnailSize),
                        ) {
                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = null,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                            )
                        }
                    },
                    supportingContent = {
                        Text(
                            text =
                                joinByBullet(
                                    song.artists.joinToString { it.name },
                                    makeTimeString(song.duration * 1000L),
                                ),
                        )
                    },
                )
            }
        }
    }

    val playlistThumbnails = remember(playlist.thumbnail) { listOfNotNull(playlist.thumbnail) }

    val fetchPlaylistSongs: suspend () -> List<SongItem> = {
        songs.ifEmpty {
            withContext(Dispatchers.IO) {
                YouTube
                    .playlist(playlist.id)
                    .completed()
                    .getOrNull()
                    ?.songs
                    .orEmpty()
            }
        }
    }

    val toggleBookmark: () -> Unit = {
        if (dbPlaylist?.playlist == null) {
            coroutineScope.launch(Dispatchers.IO) {
                val fetchedSongs =
                    songs.ifEmpty {
                        YouTube
                            .playlist(playlist.id)
                            .completed()
                            .getOrNull()
                            ?.songs
                            .orEmpty()
                    }
                database.withTransaction {
                    val existingPlaylist = playlistEntityByBrowseId(playlist.id)
                    val targetPlaylistId =
                        if (existingPlaylist == null) {
                            val playlistEntity =
                                PlaylistEntity(
                                    name = playlist.title,
                                    browseId = playlist.id,
                                    thumbnailUrl = playlist.thumbnail,
                                    isEditable = false,
                                    remoteSongCount =
                                        playlist.songCountText?.let {
                                            Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                        },
                                    playEndpointParams = playlist.playEndpoint?.params,
                                    shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                    radioEndpointParams = playlist.radioEndpoint?.params,
                                ).toggleLike()
                            insert(playlistEntity)
                            playlistEntityByBrowseId(playlist.id)?.id ?: playlistEntity.id
                        } else {
                            val refreshedPlaylist =
                                existingPlaylist.copy(
                                    name = playlist.title,
                                    browseId = playlist.id,
                                    thumbnailUrl = playlist.thumbnail,
                                    isEditable = playlist.isEditable,
                                    remoteSongCount =
                                        playlist.songCountText?.let {
                                            Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                        },
                                    playEndpointParams = playlist.playEndpoint?.params,
                                    shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                    radioEndpointParams = playlist.radioEndpoint?.params,
                                )
                            update(
                                if (existingPlaylist.bookmarkedAt == null) {
                                    refreshedPlaylist.toggleLike()
                                } else {
                                    refreshedPlaylist
                                },
                            )
                            existingPlaylist.id
                        }
                    if (fetchedSongs.isNotEmpty()) {
                        clearPlaylist(targetPlaylistId)
                        fetchedSongs.forEach { song -> insert(song.toMediaMetadata()) }
                        fetchedSongs
                            .mapIndexed { index, song ->
                                PlaylistSongMap(
                                    songId = song.id,
                                    playlistId = targetPlaylistId,
                                    position = index,
                                    setVideoId = song.setVideoId,
                                )
                            }.forEach(::insert)
                    }
                }
            }
        } else {
            database.transaction {
                val currentPlaylist = dbPlaylist!!.playlist
                update(currentPlaylist, playlist)
                update(currentPlaylist.toggleLike())
            }
        }
    }

    val showSyncMessage: suspend (String) -> Unit = { message ->
        withContext(Dispatchers.Main) {
            if (snackbarHostState != null) {
                snackbarHostState.showSnackbar(message)
            } else {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val primaryActions =
        buildList {
            playlist.playEndpoint?.let { playEndpoint ->
                add(
                    MenuAction(
                        label = stringResource(R.string.play),
                        icon = R.drawable.play,
                        onClick = {
                            playerConnection.playQueue(YouTubeQueue.playlist(playEndpoint))
                            onDismiss()
                        },
                    ),
                )
            }
            playlist.shuffleEndpoint?.let { shuffleEndpoint ->
                add(
                    MenuAction(
                        label = stringResource(R.string.shuffle),
                        icon = R.drawable.shuffle,
                        onClick = {
                            playerConnection.playQueue(YouTubeQueue.shuffledPlaylist(playlist.id, shuffleEndpoint))
                            onDismiss()
                        },
                    ),
                )
            }
            playlist.radioEndpoint?.let { radioEndpoint ->
                add(
                    MenuAction(
                        label = stringResource(R.string.start_radio),
                        icon = R.drawable.radio,
                        onClick = {
                            playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                            onDismiss()
                        },
                    ),
                )
            }
        }

    val quickActions =
        buildList {
            add(
                MenuAction(
                    label = stringResource(R.string.play_next),
                    icon = R.drawable.playlist_play,
                    onClick = {
                        coroutineScope.launch {
                            playerConnection.playNext(fetchPlaylistSongs().map { it.toMediaItem() })
                        }
                        onDismiss()
                    },
                ),
            )
            add(
                MenuAction(
                    label = stringResource(R.string.add_to_queue),
                    icon = R.drawable.queue_music,
                    onClick = {
                        coroutineScope.launch {
                            playerConnection.addToQueue(fetchPlaylistSongs().map { it.toMediaItem() })
                        }
                        onDismiss()
                    },
                ),
            )
            add(
                MenuAction(
                    label = stringResource(R.string.add_to_playlist),
                    icon = R.drawable.playlist_add,
                    onClick = { showChoosePlaylistDialog = true },
                ),
            )
            add(
                MenuAction(
                    label = stringResource(if (isInSpeedDial) R.string.remove_from_speed_dial else R.string.pin_to_speed_dial),
                    icon = if (isInSpeedDial) R.drawable.bookmark_filled else R.drawable.bookmark,
                    active = isInSpeedDial,
                    onClick = {
                        coroutineScope.launch {
                            val pin =
                                if (isInSpeedDial) {
                                    playlistPin
                                } else {
                                    val localPlaylistId =
                                        withContext(Dispatchers.IO) {
                                            database
                                                .playlistByBrowseId(playlist.id)
                                                .first()
                                                ?.playlist
                                                ?.id
                                                ?: run {
                                                    val playlistEntity =
                                                        PlaylistEntity(
                                                            name = playlist.title,
                                                            browseId = playlist.id,
                                                            thumbnailUrl = playlist.thumbnail,
                                                            isEditable = false,
                                                            remoteSongCount =
                                                                playlist.songCountText?.let {
                                                                    Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                                                },
                                                            playEndpointParams = playlist.playEndpoint?.params,
                                                            shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                                            radioEndpointParams = playlist.radioEndpoint?.params,
                                                        )
                                                    database.transaction {
                                                        insert(playlistEntity)
                                                    }
                                                    playlistEntity.id
                                                }
                                        }
                                    SpeedDialPin(type = SpeedDialPinType.PLAYLIST, id = localPlaylistId)
                                }

                            if (pin == null) return@launch

                            val updatedPins = toggleSpeedDialPin(speedDialPins, pin)
                            onSpeedDialSongIdsChange(serializeSpeedDialPins(updatedPins))
                            onDismiss()
                        }
                    },
                ),
            )
            add(
                MenuAction(
                    label = stringResource(R.string.share),
                    icon = R.drawable.share,
                    onClick = {
                        val intent =
                            Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, playlist.shareLink)
                            }
                        context.startActivity(Intent.createChooser(intent, null))
                        onDismiss()
                    },
                ),
            )
            if (canSelect) {
                add(
                    MenuAction(
                        label = stringResource(R.string.select),
                        icon = R.drawable.select_all,
                        onClick = {
                            onDismiss()
                            selectAction()
                        },
                    ),
                )
            }
        }

    MenuSheetLayout(
        hero = {
            MenuHero(
                title = playlist.title,
                style = MenuArtworkStyle.Playlist,
                thumbnails = playlistThumbnails,
                placeholderIcon = R.drawable.queue_music,
                overline = stringResource(R.string.menu_type_playlist),
                subtitle = playlist.author?.name,
                meta = playlist.songCountText,
                trailing =
                    if (playlist.id != "LM" && !playlist.isEditable) {
                        {
                            MenuFavoriteToggle(
                                checked = dbPlaylist?.playlist?.bookmarkedAt != null,
                                onCheckedChange = { toggleBookmark() },
                            )
                        }
                    } else {
                        null
                    },
            )
        },
        leadingContent = {
            MenuPrimaryActions(actions = primaryActions)
            MenuQuickActions(actions = quickActions)
        },
    ) {
        MenuSection(title = stringResource(R.string.menu_section_library)) {
            action(
                title = stringResource(R.string.import_playlist),
                icon = R.drawable.add,
                onClick = { showImportPlaylistDialog = true },
            )
            toggle(
                title = stringResource(R.string.yt_sync),
                icon = R.drawable.sync,
                checked = dbPlaylist?.playlist?.isAutoSync ?: false,
                onCheckedChange = { newValue ->
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val currentDbPlaylist = dbPlaylist
                            if (currentDbPlaylist?.playlist == null) {
                                val playlistPage = YouTube.playlist(playlist.id).completed().getOrNull()
                                val fetchedSongs = playlistPage?.songs.orEmpty()

                                if (fetchedSongs.isEmpty() && newValue) {
                                    showSyncMessage(context.getString(R.string.import_failed))
                                    return@launch
                                }

                                database.transaction {
                                    val playlistEntity =
                                        PlaylistEntity(
                                            name = playlist.title,
                                            browseId = playlist.id,
                                            thumbnailUrl = playlist.thumbnail,
                                            isEditable = false,
                                            isAutoSync = newValue,
                                            remoteSongCount =
                                                playlist.songCountText?.let {
                                                    Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                                },
                                            playEndpointParams = playlist.playEndpoint?.params,
                                            shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                            radioEndpointParams = playlist.radioEndpoint?.params,
                                        )
                                    insert(playlistEntity)
                                    fetchedSongs.forEach { song -> insert(song.toMediaMetadata()) }
                                    fetchedSongs
                                        .mapIndexed { index, song ->
                                            PlaylistSongMap(
                                                songId = song.id,
                                                playlistId = playlistEntity.id,
                                                position = index,
                                                setVideoId = song.setVideoId,
                                            )
                                        }.forEach(::insert)
                                }

                                if (newValue) {
                                    showSyncMessage(context.getString(R.string.playlist_synced))
                                }
                            } else {
                                val existing = currentDbPlaylist.playlist
                                database.query {
                                    update(existing.copy(isAutoSync = newValue))
                                }

                                if (newValue) {
                                    syncUtils.syncAutoSyncPlaylists()
                                    showSyncMessage(context.getString(R.string.playlist_synced))
                                }
                            }
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to toggle YouTube sync for ${playlist.id}")
                            showSyncMessage(context.getString(R.string.import_failed) + ": ${e.message ?: "Unknown error"}")
                        }
                    }
                },
            )
        }

        MenuSection(title = stringResource(R.string.menu_section_offline)) {
            downloadAction(
                state = downloadState,
                onDownload = {
                    coroutineScope.launch {
                        sendAddMissingDownloads(
                            context = context,
                            songs =
                                fetchPlaylistSongs().map { song ->
                                    HeaderDownloadItem(
                                        id = song.id,
                                        title = song.title,
                                    )
                                },
                            downloads = downloadUtil.downloads.value,
                        )
                    }
                },
                onRemove = { showRemoveDownloadDialog = true },
            )
        }
    }
}
