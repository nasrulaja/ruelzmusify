/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.ruelz.ruelzmusify.extensions

import moe.ruelz.ruelzmusify.innertube.models.AlbumItem
import moe.ruelz.ruelzmusify.innertube.models.ArtistItem
import moe.ruelz.ruelzmusify.innertube.models.EpisodeItem
import moe.ruelz.ruelzmusify.innertube.models.PlaylistItem
import moe.ruelz.ruelzmusify.innertube.models.PodcastItem
import moe.ruelz.ruelzmusify.innertube.models.SongItem
import moe.ruelz.ruelzmusify.innertube.models.YTItem
import moe.ruelz.ruelzmusify.innertube.pages.BrowseResult

fun <T : YTItem> List<T>.filterBlockedArtists(blockedArtistIds: Set<String>): List<T> {
    if (blockedArtistIds.isEmpty()) return this

    return filter { item ->
        when (item) {
            is ArtistItem -> item.id !in blockedArtistIds
            is SongItem -> item.artists.none { it.id in blockedArtistIds }
            is AlbumItem -> item.artists.orEmpty().none { it.id in blockedArtistIds }
            is PlaylistItem -> item.author?.id !in blockedArtistIds
            is PodcastItem -> item.author?.id !in blockedArtistIds
            is EpisodeItem -> item.podcast?.id !in blockedArtistIds
        }
    }
}

fun BrowseResult.filterBlockedArtists(blockedArtistIds: Set<String>): BrowseResult {
    if (blockedArtistIds.isEmpty()) return this

    return copy(
        items =
            items.mapNotNull { section ->
                section.copy(
                    items =
                        section.items
                            .filterBlockedArtists(blockedArtistIds)
                            .ifEmpty { return@mapNotNull null },
                )
            },
    )
}
