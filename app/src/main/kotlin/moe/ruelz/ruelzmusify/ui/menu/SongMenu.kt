/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package moe.ruelz.ruelzmusify.ui.menu

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.ruelz.ruelzmusify.LocalDatabase
import moe.ruelz.ruelzmusify.LocalDownloadUtil
import moe.ruelz.ruelzmusify.LocalPlayerConnection
import moe.ruelz.ruelzmusify.LocalSyncUtils
import moe.ruelz.ruelzmusify.R
import moe.ruelz.ruelzmusify.constants.ArtistSeparatorsKey
import moe.ruelz.ruelzmusify.constants.ExternalDownloaderEnabledKey
import moe.ruelz.ruelzmusify.constants.ExternalDownloaderPackageKey
import moe.ruelz.ruelzmusify.constants.ListThumbnailSize
import moe.ruelz.ruelzmusify.constants.SpeedDialSongIdsKey
import moe.ruelz.ruelzmusify.db.entities.ArtistEntity
import moe.ruelz.ruelzmusify.db.entities.Event
import moe.ruelz.ruelzmusify.db.entities.PlaylistSong
import moe.ruelz.ruelzmusify.db.entities.Song
import moe.ruelz.ruelzmusify.extensions.toMediaItem
import moe.ruelz.ruelzmusify.innertube.YouTube
import moe.ruelz.ruelzmusify.models.toMediaMetadata
import moe.ruelz.ruelzmusify.playback.ExoDownloadService
import moe.ruelz.ruelzmusify.playback.queues.YouTubeQueue
import moe.ruelz.ruelzmusify.ui.component.ListDialog
import moe.ruelz.ruelzmusify.ui.component.LocalBottomSheetPageState
import moe.ruelz.ruelzmusify.ui.component.MenuAction
import moe.ruelz.ruelzmusify.ui.component.MenuArtworkStyle
import moe.ruelz.ruelzmusify.ui.component.MenuFavoriteToggle
import moe.ruelz.ruelzmusify.ui.component.MenuHero
import moe.ruelz.ruelzmusify.ui.component.MenuPrimaryActions
import moe.ruelz.ruelzmusify.ui.component.MenuQuickActions
import moe.ruelz.ruelzmusify.ui.component.MenuRowTone
import moe.ruelz.ruelzmusify.ui.component.MenuSection
import moe.ruelz.ruelzmusify.ui.component.MenuSheetLayout
import moe.ruelz.ruelzmusify.ui.component.SongListItem
import moe.ruelz.ruelzmusify.ui.component.TextFieldDialog
import moe.ruelz.ruelzmusify.ui.component.downloadAction
import moe.ruelz.ruelzmusify.ui.utils.ShowMediaInfo
import moe.ruelz.ruelzmusify.ui.utils.YtimgResizePolicy
import moe.ruelz.ruelzmusify.ui.utils.navigateToAlbum
import moe.ruelz.ruelzmusify.ui.utils.navigateToArtist
import moe.ruelz.ruelzmusify.ui.utils.resize
import moe.ruelz.ruelzmusify.utils.ExternalDownloaderLaunchResult
import moe.ruelz.ruelzmusify.utils.SpeedDialPin
import moe.ruelz.ruelzmusify.utils.SpeedDialPinType
import moe.ruelz.ruelzmusify.utils.joinByBullet
import moe.ruelz.ruelzmusify.utils.makeTimeString
import moe.ruelz.ruelzmusify.utils.openExternalDownloader
import moe.ruelz.ruelzmusify.utils.parseSpeedDialPins
import moe.ruelz.ruelzmusify.utils.rememberPreference
import moe.ruelz.ruelzmusify.utils.serializeSpeedDialPins
import moe.ruelz.ruelzmusify.utils.shareLocalAudio
import moe.ruelz.ruelzmusify.utils.toggleSpeedDialPin
import moe.ruelz.ruelzmusify.viewmodels.CachePlaylistViewModel
import timber.log.Timber

@Composable
fun SongMenu(
    originalSong: Song,
    event: Event? = null,
    navController: NavController,
    playlistSong: PlaylistSong? = null,
    playlistBrowseId: String? = null,
    onChangeSource: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    isFromCache: Boolean = false,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val songState = database.song(originalSong.id).collectAsState(initial = originalSong)
    val song = songState.value ?: originalSong
    val download by LocalDownloadUtil.current
        .getDownload(originalSong.id)
        .collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val syncUtils = LocalSyncUtils.current
    var refetchIconDegree by remember { mutableFloatStateOf(0f) }

    val cacheViewModel = hiltViewModel<CachePlaylistViewModel>()

    val rotationAnimation by animateFloatAsState(
        targetValue = refetchIconDegree,
        animationSpec = tween(durationMillis = 800),
        label = "",
    )

    // Artist separators for splitting artist names
    val (artistSeparators) = rememberPreference(ArtistSeparatorsKey, defaultValue = ",;/&")
    val (externalDownloaderEnabled) = rememberPreference(ExternalDownloaderEnabledKey, defaultValue = false)
    val (externalDownloaderPackage) = rememberPreference(ExternalDownloaderPackageKey, defaultValue = "")
    val (speedDialSongIds, onSpeedDialSongIdsChange) = rememberPreference(SpeedDialSongIdsKey, "")
    val speedDialPins = remember(speedDialSongIds) { parseSpeedDialPins(speedDialSongIds) }
    val songPin = remember(song.id) { SpeedDialPin(type = SpeedDialPinType.SONG, id = song.id) }
    val isInSpeedDial =
        remember(speedDialPins, songPin) {
            speedDialPins.any { it.type == songPin.type && it.id == songPin.id }
        }

    val orderedArtists by produceState(initialValue = emptyList<ArtistEntity>(), song) {
        withContext(Dispatchers.IO) {
            val artistMaps = database.songArtistMap(song.id).sortedBy { it.position }
            val sorted =
                artistMaps.mapNotNull { map ->
                    song.artists.firstOrNull { it.id == map.artistId }
                }
            value = sorted
        }
    }

    // Split artists by configured separators
    data class SplitArtist(
        val name: String,
        val originalArtist: ArtistEntity?,
    )

    val splitArtists =
        remember(orderedArtists, artistSeparators) {
            if (artistSeparators.isEmpty()) {
                orderedArtists.map { SplitArtist(it.name, it) }
            } else {
                val separatorRegex = "[${Regex.escape(artistSeparators)}]".toRegex()
                orderedArtists.flatMap { artist ->
                    val parts =
                        artist.name
                            .split(separatorRegex)
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                    if (parts.size > 1) {
                        // If the name contains separators, create split artists
                        // The first part keeps the original artist reference for navigation
                        parts.mapIndexed { index, name ->
                            SplitArtist(name, if (index == 0) artist else null)
                        }
                    } else {
                        listOf(SplitArtist(artist.name, artist))
                    }
                }
            }
        }

    var showEditDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val TextFieldValueSaver: Saver<TextFieldValue, *> =
        Saver(
            save = { it.text },
            restore = { text -> TextFieldValue(text, TextRange(text.length)) },
        )

    var titleField by rememberSaveable(stateSaver = TextFieldValueSaver) {
        mutableStateOf(TextFieldValue(song.song.title))
    }

    var artistField by rememberSaveable(stateSaver = TextFieldValueSaver) {
        mutableStateOf(
            TextFieldValue(
                song.artists
                    .firstOrNull()
                    ?.name
                    .orEmpty(),
            ),
        )
    }

    if (showEditDialog) {
        TextFieldDialog(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = null,
                )
            },
            title = {
                Text(text = stringResource(R.string.edit_song))
            },
            textFields =
                listOf(
                    stringResource(R.string.song_title) to titleField,
                    stringResource(R.string.artist_name) to artistField,
                ),
            onTextFieldsChange = { index, newValue ->
                if (index == 0) {
                    titleField = newValue
                } else {
                    artistField = newValue
                }
            },
            onDoneMultiple = { values ->
                val newTitle = values[0]
                val newArtist = values[1]

                coroutineScope.launch {
                    database.query {
                        update(song.song.copy(title = newTitle, titleOverride = true))
                        val artist = song.artists.firstOrNull()
                        if (artist != null) {
                            update(artist.copy(name = newArtist))
                        }
                    }

                    showEditDialog = false
                    onDismiss()
                }
            },
            onDismiss = { showEditDialog = false },
        )
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = {
            listOf(song.id)
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        },
        onAddComplete = { songCount, playlistNames ->
            val message =
                when {
                    playlistNames.size == 1 -> context.getString(R.string.added_to_playlist, playlistNames.first())
                    else -> context.getString(R.string.added_to_n_playlists, playlistNames.size)
                }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
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

            items(listOf(song)) { song ->
                SongListItem(song = song)
            }
        }
    }

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(
                items = splitArtists.distinctBy { it.name },
                key = { it.name },
            ) { splitArtist ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = splitArtist.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = {
                        AsyncImage(
                            model =
                                splitArtist.originalArtist?.thumbnailUrl?.resize(
                                    width = 200,
                                    height = 200,
                                    ytimgResizePolicy = YtimgResizePolicy.PreserveOriginal,
                                ),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                        )
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                splitArtist.originalArtist?.let { artist ->
                                    navController.navigateToArtist(artist.id)
                                    showSelectArtistDialog = false
                                    onDismiss()
                                }
                            },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }

    val bottomSheetPageState = LocalBottomSheetPageState.current
    val isLocalSong = song.song.isLocal
    val songThumbnails = remember(song.thumbnailUrl) { listOfNotNull(song.thumbnailUrl) }
    val heroSubtitle = remember(song.artists) { song.artists.joinToString { it.name } }
    val heroMeta =
        remember(song.album?.title, song.song.albumName, song.song.duration) {
            joinByBullet(
                song.album?.title ?: song.song.albumName,
                song.song.duration.takeIf { it > 0 }?.let { makeTimeString(it * 1000L) },
            )
        }

    val toggleLike: () -> Unit = {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val requestedSong =
                    database.withTransaction {
                        val currentSong =
                            getSongById(song.id)
                                ?: run {
                                    insert(song.toMediaMetadata())
                                    getSongById(song.id)
                                }
                                ?: return@withTransaction null
                        currentSong.song.toggleLike()
                    } ?: return@launch
                syncUtils.likeSong(requestedSong).onFailure { error ->
                    Timber.w(error, "Failed to update liked song ${song.id}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, R.string.error_unknown, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.e(error, "Failed to prepare liked song ${song.id}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.error_unknown, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val playNextText = stringResource(R.string.play_next)
    val addToQueueText = stringResource(R.string.add_to_queue)
    val startRadioText = stringResource(R.string.start_radio)
    val addToPlaylistText = stringResource(R.string.add_to_playlist)
    val shareText = stringResource(R.string.share)
    val editText = stringResource(R.string.edit)
    val speedDialText = stringResource(if (isInSpeedDial) R.string.remove_from_speed_dial else R.string.pin_to_speed_dial)

    val primaryActions =
        buildList {
            add(
                MenuAction(
                    label = playNextText,
                    icon = R.drawable.playlist_play,
                    onClick = {
                        onDismiss()
                        playerConnection.playNext(song.toMediaItem())
                    },
                ),
            )
            add(
                MenuAction(
                    label = addToQueueText,
                    icon = R.drawable.queue_music,
                    onClick = {
                        onDismiss()
                        playerConnection.addToQueue(song.toMediaItem())
                    },
                ),
            )
            if (!isLocalSong) {
                add(
                    MenuAction(
                        label = startRadioText,
                        icon = R.drawable.radio,
                        onClick = {
                            onDismiss()
                            playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                        },
                    ),
                )
            }
        }

    val quickActions =
        listOf(
            MenuAction(
                label = addToPlaylistText,
                icon = R.drawable.playlist_add,
                onClick = { showChoosePlaylistDialog = true },
            ),
            MenuAction(
                label = shareText,
                icon = R.drawable.share,
                onClick = {
                    onDismiss()
                    if (isLocalSong) {
                        shareLocalAudio(context, song.id, song.format?.mimeType)
                    } else {
                        val intent =
                            Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${song.id}")
                            }
                        context.startActivity(Intent.createChooser(intent, null))
                    }
                },
            ),
            MenuAction(
                label = editText,
                icon = R.drawable.edit,
                onClick = { showEditDialog = true },
            ),
            MenuAction(
                label = speedDialText,
                icon = if (isInSpeedDial) R.drawable.bookmark_filled else R.drawable.bookmark,
                active = isInSpeedDial,
                onClick = {
                    val updatedPins = toggleSpeedDialPin(speedDialPins, songPin)
                    onSpeedDialSongIdsChange(serializeSpeedDialPins(updatedPins))
                    onDismiss()
                },
            ),
        )

    MenuSheetLayout(
        hero = {
            MenuHero(
                title = song.song.title,
                style = MenuArtworkStyle.Song,
                thumbnails = songThumbnails,
                placeholderIcon = R.drawable.music_note,
                overline = stringResource(R.string.menu_type_song),
                subtitle = heroSubtitle,
                meta = heroMeta,
                trailing = {
                    MenuFavoriteToggle(
                        checked = song.song.liked,
                        onCheckedChange = { toggleLike() },
                    )
                },
            )
        },
        leadingContent = {
            MenuPrimaryActions(actions = primaryActions)
            MenuQuickActions(actions = quickActions)
        },
    ) {
        if (!isLocalSong) {
            MenuSection(title = stringResource(R.string.menu_section_library)) {
                val inLibrary = song.song.inLibrary != null
                action(
                    title = stringResource(if (inLibrary) R.string.remove_from_library else R.string.add_to_library),
                    icon = if (inLibrary) R.drawable.library_add_check else R.drawable.library_add,
                    tone = if (inLibrary) MenuRowTone.Active else MenuRowTone.Neutral,
                    onClick = {
                        onDismiss()
                        database.query {
                            update(song.song.toggleLibrary())
                        }
                    },
                )
            }

            MenuSection(title = stringResource(R.string.menu_section_offline)) {
                downloadAction(
                    state = download?.state,
                    onDownload = {
                        val downloadRequest =
                            DownloadRequest
                                .Builder(song.id, song.id.toUri())
                                .setCustomCacheKey(song.id)
                                .setData(song.song.title.toByteArray())
                                .build()
                        DownloadService.sendAddDownload(
                            context,
                            ExoDownloadService::class.java,
                            downloadRequest,
                            false,
                        )
                    },
                    onRemove = {
                        DownloadService.sendRemoveDownload(
                            context,
                            ExoDownloadService::class.java,
                            song.id,
                            false,
                        )
                    },
                )
                if (externalDownloaderEnabled) {
                    action(
                        title = stringResource(R.string.open_with_downloader),
                        icon = R.drawable.download,
                        onClick = {
                            onDismiss()
                            val url = "https://music.youtube.com/watch?v=${song.id}"
                            when (context.openExternalDownloader(externalDownloaderPackage, url)) {
                                ExternalDownloaderLaunchResult.STARTED -> Unit
                                ExternalDownloaderLaunchResult.NOT_CONFIGURED -> {
                                    Toast
                                        .makeText(
                                            context,
                                            context.getString(R.string.external_downloader_not_configured),
                                            Toast.LENGTH_LONG,
                                        ).show()
                                }
                                ExternalDownloaderLaunchResult.NOT_INSTALLED -> {
                                    Toast
                                        .makeText(
                                            context,
                                            context.getString(R.string.external_downloader_not_installed),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                }
                            }
                        },
                    )
                }
            }
        }

        MenuSection(title = stringResource(R.string.menu_section_go_to)) {
            action(
                title = stringResource(R.string.view_artist),
                icon = R.drawable.artist,
                navigates = true,
                onClick = {
                    if (splitArtists.size == 1 && splitArtists[0].originalArtist != null) {
                        navController.navigateToArtist(splitArtists[0].originalArtist!!.id)
                        onDismiss()
                    } else {
                        showSelectArtistDialog = true
                    }
                },
            )
            song.song.albumId?.let { albumId ->
                action(
                    title = stringResource(R.string.view_album),
                    icon = R.drawable.album,
                    navigates = true,
                    onClick = {
                        onDismiss()
                        navController.navigateToAlbum(albumId)
                    },
                )
            }
        }

        MenuSection(title = stringResource(R.string.menu_section_manage)) {
            if (onChangeSource != null) {
                action(
                    title = stringResource(R.string.change_song_source),
                    supporting = stringResource(R.string.change_song_source_desc),
                    icon = R.drawable.sync,
                    onClick = {
                        onChangeSource()
                        onDismiss()
                    },
                )
            }
            if (!isLocalSong) {
                action(
                    title = stringResource(R.string.refetch),
                    icon = R.drawable.sync,
                    iconRotation = { rotationAnimation },
                    onClick = {
                        refetchIconDegree -= 360
                        coroutineScope.launch(Dispatchers.IO) {
                            YouTube.queue(listOf(song.id)).onSuccess {
                                val newSong = it.firstOrNull()
                                if (newSong != null) {
                                    database.transaction {
                                        update(song, newSong.toMediaMetadata())
                                    }
                                }
                            }
                        }
                    },
                )
            }
            action(
                title = stringResource(R.string.details),
                icon = R.drawable.info,
                navigates = true,
                onClick = {
                    onDismiss()
                    bottomSheetPageState.show {
                        ShowMediaInfo(song.id)
                    }
                },
            )
        }

        MenuSection(title = null) {
            if (event != null) {
                action(
                    title = stringResource(R.string.remove_from_history),
                    icon = R.drawable.delete,
                    tone = MenuRowTone.Destructive,
                    onClick = {
                        onDismiss()
                        database.query {
                            delete(event)
                        }
                    },
                )
            }
            if (playlistSong != null) {
                action(
                    title = stringResource(R.string.remove_from_playlist),
                    icon = R.drawable.delete,
                    tone = MenuRowTone.Destructive,
                    onClick = {
                        val map = playlistSong.map
                        coroutineScope.launch(Dispatchers.IO) {
                            val browseId = playlistBrowseId
                            if (browseId != null) {
                                val remoteResult = removeSongFromRemotePlaylist(browseId, map)
                                if (remoteResult.isFailure) {
                                    withContext(Dispatchers.Main) {
                                        Toast
                                            .makeText(
                                                context,
                                                context.getString(R.string.error_unknown),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        onDismiss()
                                    }
                                    return@launch
                                }
                            }
                            database.withTransaction {
                                val maxPosition = maxPlaylistSongPosition(map.playlistId) ?: map.position
                                if (map.position < maxPosition) {
                                    move(map.playlistId, map.position, maxPosition)
                                }
                                delete(map)
                            }
                            withContext(Dispatchers.Main) {
                                onDismiss()
                            }
                        }
                    },
                )
            }
            if (isFromCache) {
                action(
                    title = stringResource(R.string.remove_from_cache),
                    icon = R.drawable.delete,
                    tone = MenuRowTone.Destructive,
                    onClick = {
                        onDismiss()
                        cacheViewModel.removeSongFromCache(song.id)
                    },
                )
            }
        }
    }
}
