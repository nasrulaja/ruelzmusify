/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.ruelz.ruelzmusify.mediainfo

import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import moe.ruelz.ruelzmusify.db.MusicDatabase
import moe.ruelz.ruelzmusify.innertube.YouTube
import moe.ruelz.ruelzmusify.modules.ModuleCapabilities
import moe.ruelz.ruelzmusify.modules.ModuleContentRepository
import moe.ruelz.ruelzmusify.modules.ModuleException
import moe.ruelz.ruelzmusify.modules.ModuleFailure
import moe.ruelz.ruelzmusify.modules.ModuleTrackDetailsResult

class MediaInfoRepository @Inject constructor(
    private val database: MusicDatabase,
    private val moduleContent: ModuleContentRepository,
) {
    fun observeLocal(videoId: String): Flow<LocalMediaInfo> =
        combine(database.song(videoId), database.format(videoId)) { song, format ->
            LocalMediaInfo(
                title = song?.title,
                artists = song?.artists?.takeIf { it.isNotEmpty() }?.joinToString { it.name },
                artwork = song?.thumbnailUrl,
                isLocal = song?.song?.isLocal == true,
                format = format?.let {
                    MediaInfoFormat(
                        itag = it.itag,
                        mimeType = it.mimeType,
                        codecs = it.codecs,
                        bitrate = it.bitrate,
                        sampleRate = it.sampleRate,
                        loudnessDb = it.loudnessDb,
                        contentLength = it.contentLength,
                    )
                },
            )
        }.flowOn(Dispatchers.IO)

    suspend fun metadata(videoId: String): MediaInfoMetadata = withContext(Dispatchers.IO) {
        val info = YouTube.getMediaMetadata(videoId)
        MediaInfoMetadata(
            title = info.title,
            author = info.author,
            artwork = info.authorThumbnail,
            description = info.description?.takeIf(String::isNotBlank),
            subscribers = info.subscribers,
        )
    }

    suspend fun hasModuleMetadata(): Boolean = moduleContent.providers(ModuleCapabilities.METADATA).isNotEmpty()

    suspend fun moduleMetadata(videoId: String, local: LocalMediaInfo?): ModuleTrackDetailsResult? = withContext(Dispatchers.IO) {
        val song = moduleContent.song(videoId, local?.title.orEmpty(), listOfNotNull(local?.artists), album = null, durationSeconds = 0)
            ?: return@withContext null
        try {
            moduleContent.metadata(song)
        } catch (error: ModuleException) {
            if (error.failure == ModuleFailure.NETWORK || error.failure == ModuleFailure.TIMEOUT) {
                throw IOException("Module metadata is temporarily unavailable", error)
            }
            throw error
        }
    }

    suspend fun statistics(videoId: String): MediaInfoStatistics = withContext(Dispatchers.IO) {
        val statistics = YouTube.getMediaStatistics(videoId)
        MediaInfoStatistics(
            views = statistics.viewCount,
            likes = statistics.likes,
            dislikes = statistics.dislikes,
        )
    }
}
