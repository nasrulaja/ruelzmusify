/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.ruelz.ruelzmusify.ui.menu

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.ruelz.ruelzmusify.LocalDatabase
import moe.ruelz.ruelzmusify.LocalPlayerConnection
import moe.ruelz.ruelzmusify.R
import moe.ruelz.ruelzmusify.constants.ArtistSongSortType
import moe.ruelz.ruelzmusify.constants.SpeedDialSongIdsKey
import moe.ruelz.ruelzmusify.db.entities.Artist
import moe.ruelz.ruelzmusify.extensions.toMediaItem
import moe.ruelz.ruelzmusify.playback.queues.ListQueue
import moe.ruelz.ruelzmusify.ui.component.MenuAction
import moe.ruelz.ruelzmusify.ui.component.MenuArtworkStyle
import moe.ruelz.ruelzmusify.ui.component.MenuHero
import moe.ruelz.ruelzmusify.ui.component.MenuHeroToggleButton
import moe.ruelz.ruelzmusify.ui.component.MenuPrimaryActions
import moe.ruelz.ruelzmusify.ui.component.MenuRowTone
import moe.ruelz.ruelzmusify.ui.component.MenuSection
import moe.ruelz.ruelzmusify.ui.component.MenuSheetLayout
import moe.ruelz.ruelzmusify.utils.SpeedDialPin
import moe.ruelz.ruelzmusify.utils.SpeedDialPinType
import moe.ruelz.ruelzmusify.utils.parseSpeedDialPins
import moe.ruelz.ruelzmusify.utils.rememberPreference
import moe.ruelz.ruelzmusify.utils.serializeSpeedDialPins
import moe.ruelz.ruelzmusify.utils.toggleSpeedDialPin

@Composable
fun ArtistMenu(
    originalArtist: Artist,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val artistState = database.artist(originalArtist.id).collectAsStateWithLifecycle(initialValue = originalArtist)
    val artist = artistState.value ?: originalArtist
    val (speedDialSongIds, onSpeedDialSongIdsChange) = rememberPreference(SpeedDialSongIdsKey, "")
    val speedDialPins = remember(speedDialSongIds) { parseSpeedDialPins(speedDialSongIds) }
    val artistPin = remember(artist.id) { SpeedDialPin(type = SpeedDialPinType.ARTIST, id = artist.id) }
    val isInSpeedDial =
        remember(speedDialPins, artistPin) {
            speedDialPins.any { it.type == artistPin.type && it.id == artistPin.id }
        }

    val artistThumbnails = remember(artist.artist.thumbnailUrl) { listOfNotNull(artist.artist.thumbnailUrl) }
    val isBookmarked = artist.artist.bookmarkedAt != null
    val isLocalArtist = artist.artist.isLocal
    val heroSubtitle =
        artist.songCount.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.n_song, it, it) }

    val primaryActions =
        buildList {
            if (artist.songCount > 0) {
                add(
                    MenuAction(
                        label = stringResource(R.string.play),
                        icon = R.drawable.play,
                        onClick = {
                            coroutineScope.launch {
                                val songs =
                                    withContext(Dispatchers.IO) {
                                        database
                                            .artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                            .first()
                                            .map { it.toMediaItem() }
                                    }
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = artist.artist.name,
                                        items = songs,
                                    ),
                                )
                            }
                            onDismiss()
                        },
                    ),
                )
                add(
                    MenuAction(
                        label = stringResource(R.string.shuffle),
                        icon = R.drawable.shuffle,
                        onClick = {
                            coroutineScope.launch {
                                val songs =
                                    withContext(Dispatchers.IO) {
                                        database
                                            .artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                            .first()
                                            .map { it.toMediaItem() }
                                            .shuffled()
                                    }
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = artist.artist.name,
                                        items = songs,
                                    ),
                                )
                            }
                            onDismiss()
                        },
                    ),
                )
            }
            if (!isLocalArtist && artist.artist.isYouTubeArtist) {
                add(
                    MenuAction(
                        label = stringResource(R.string.share),
                        icon = R.drawable.share,
                        onClick = {
                            onDismiss()
                            val intent =
                                Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "https://music.youtube.com/channel/${artist.id}",
                                    )
                                }
                            context.startActivity(Intent.createChooser(intent, null))
                        },
                    ),
                )
            }
        }

    MenuSheetLayout(
        hero = {
            MenuHero(
                title = artist.artist.name,
                style = MenuArtworkStyle.Artist,
                thumbnails = artistThumbnails,
                placeholderIcon = R.drawable.artist,
                overline = stringResource(R.string.menu_type_artist),
                subtitle = heroSubtitle,
                bottom = {
                    MenuHeroToggleButton(
                        checked = isBookmarked,
                        onCheckedChange = {
                            database.transaction {
                                update(artist.artist.toggleLike())
                            }
                        },
                        label =
                            stringResource(
                                when {
                                    isLocalArtist && isBookmarked -> R.string.remove_from_library
                                    isLocalArtist -> R.string.add_to_library
                                    isBookmarked -> R.string.subscribed
                                    else -> R.string.subscribe
                                },
                            ),
                        icon = R.drawable.subscribe,
                        checkedIcon = R.drawable.subscribed,
                    )
                },
            )
        },
        leadingContent = {
            MenuPrimaryActions(actions = primaryActions)
        },
    ) {
        MenuSection(title = stringResource(R.string.menu_section_library)) {
            action(
                title = stringResource(if (isInSpeedDial) R.string.remove_from_speed_dial else R.string.pin_to_speed_dial),
                icon = if (isInSpeedDial) R.drawable.bookmark_filled else R.drawable.bookmark,
                tone = if (isInSpeedDial) MenuRowTone.Active else MenuRowTone.Neutral,
                onClick = {
                    val updatedPins = toggleSpeedDialPin(speedDialPins, artistPin)
                    onSpeedDialSongIdsChange(serializeSpeedDialPins(updatedPins))
                    onDismiss()
                },
            )
        }
    }
}
