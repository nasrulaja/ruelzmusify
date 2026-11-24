/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.ruelz.ruelzmusify.playback.queues

import androidx.media3.common.MediaItem
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import moe.ruelz.ruelzmusify.extensions.toMediaItem
import moe.ruelz.ruelzmusify.innertube.YouTube
import moe.ruelz.ruelzmusify.innertube.utils.completed
import moe.ruelz.ruelzmusify.models.MediaMetadata

internal class YouTubePlaylistShuffleQueue(
    private val playlistId: String,
) : Queue {
    override val preloadItem: MediaMetadata? = null

    override suspend fun getInitialStatus(): Queue.Status =
        withContext(IO) {
            val page = YouTube.playlist(playlistId).completed().getOrThrow()
            Queue.Status(
                title = page.playlist.title,
                items = page.songs.shuffled().map { it.toMediaItem() },
                mediaItemIndex = 0,
            )
        }

    override fun hasNextPage(): Boolean = false

    override suspend fun nextPage(): List<MediaItem> = throw UnsupportedOperationException()
}
