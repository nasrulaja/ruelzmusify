/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.ruelz.ruelzmusify.ui.menu

import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.ruelz.ruelzmusify.LocalDatabase
import moe.ruelz.ruelzmusify.LocalPlayerConnection
import moe.ruelz.ruelzmusify.R
import moe.ruelz.ruelzmusify.constants.SpeedDialSongIdsKey
import moe.ruelz.ruelzmusify.db.entities.ArtistEntity
import moe.ruelz.ruelzmusify.innertube.models.ArtistItem
import moe.ruelz.ruelzmusify.playback.queues.YouTubeQueue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeArtistMenu(
    artist: ArtistItem,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val libraryArtist by database.artist(artist.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val (speedDialSongIds, onSpeedDialSongIdsChange) = rememberPreference(SpeedDialSongIdsKey, "")
    val speedDialPins = remember(speedDialSongIds) { parseSpeedDialPins(speedDialSongIds) }
    val artistPin = remember(artist.id) { SpeedDialPin(type = SpeedDialPinType.ARTIST, id = artist.id) }
    val isInSpeedDial =
        remember(speedDialPins, artistPin) {
            speedDialPins.any { it.type == artistPin.type && it.id == artistPin.id }
        }

    val artistThumbnails = remember(artist.thumbnail) { listOfNotNull(artist.thumbnail) }
    val isSubscribed = libraryArtist?.artist?.bookmarkedAt != null

    val primaryActions =
        buildList {
            artist.radioEndpoint?.let { watchEndpoint ->
                add(
                    MenuAction(
                        label = stringResource(R.string.start_radio),
                        icon = R.drawable.radio,
                        onClick = {
                            playerConnection.playQueue(YouTubeQueue(watchEndpoint))
                            onDismiss()
                        },
                    ),
                )
            }
            artist.shuffleEndpoint?.let { watchEndpoint ->
                add(
                    MenuAction(
                        label = stringResource(R.string.shuffle),
                        icon = R.drawable.shuffle,
                        onClick = {
                            playerConnection.playQueue(YouTubeQueue(watchEndpoint))
                            onDismiss()
                        },
                    ),
                )
            }
            add(
                MenuAction(
                    label = stringResource(R.string.share),
                    icon = R.drawable.share,
                    onClick = {
                        val intent =
                            Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, artist.shareLink)
                            }
                        context.startActivity(Intent.createChooser(intent, null))
                        onDismiss()
                    },
                ),
            )
        }

    MenuSheetLayout(
        hero = {
            MenuHero(
                title = artist.title,
                style = MenuArtworkStyle.Artist,
                thumbnails = artistThumbnails,
                placeholderIcon = R.drawable.artist,
                overline = stringResource(R.string.menu_type_artist),
                subtitle = artist.subscriberCountText ?: artist.monthlyListenerCountText,
                bottom = {
                    MenuHeroToggleButton(
                        checked = isSubscribed,
                        onCheckedChange = {
                            database.query {
                                val libraryArtist = libraryArtist
                                if (libraryArtist != null) {
                                    update(libraryArtist.artist.toggleLike())
                                } else {
                                    insert(
                                        ArtistEntity(
                                            id = artist.id,
                                            name = artist.title,
                                            channelId = artist.channelId,
                                            thumbnailUrl = artist.thumbnail,
                                        ).toggleLike(),
                                    )
                                }
                            }
                        },
                        label = stringResource(if (isSubscribed) R.string.subscribed else R.string.subscribe),
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
                    coroutineScope.launch {
                        if (!isInSpeedDial) {
                            withContext(Dispatchers.IO) {
                                database.transaction {
                                    insert(
                                        ArtistEntity(
                                            id = artist.id,
                                            name = artist.title,
                                            channelId = artist.channelId,
                                            thumbnailUrl = artist.thumbnail,
                                        ),
                                    )
                                }
                            }
                        }

                        val updatedPins = toggleSpeedDialPin(speedDialPins, artistPin)
                        onSpeedDialSongIdsChange(serializeSpeedDialPins(updatedPins))
                        onDismiss()
                    }
                },
            )
        }
    }
}
