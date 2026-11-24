/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.ruelz.ruelzmusify.browse

import android.content.Context
import com.google.common.collect.ImmutableList
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.ruelz.ruelzmusify.aicontentfilter.FilterAiContentUseCase
import moe.ruelz.ruelzmusify.aicontentfilter.LoadAiContentFilterPolicyUseCase
import moe.ruelz.ruelzmusify.constants.HideExplicitKey
import moe.ruelz.ruelzmusify.constants.HideVideoKey
import moe.ruelz.ruelzmusify.db.MusicDatabase
import moe.ruelz.ruelzmusify.extensions.filterBlockedArtists
import moe.ruelz.ruelzmusify.innertube.models.BrowseEndpoint
import moe.ruelz.ruelzmusify.innertube.models.filterExplicit
import moe.ruelz.ruelzmusify.innertube.models.filterVideo
import moe.ruelz.ruelzmusify.repository.GenreDiscoveryRepository
import moe.ruelz.ruelzmusify.utils.dataStore
import moe.ruelz.ruelzmusify.utils.get
import javax.inject.Inject

data class GenrePage(
    val title: String?,
    val thumbnail: String?,
    val sections: ImmutableList<GenreSection>,
    val continuation: String?,
)

class LoadGenreDiscoveryUseCase @Inject constructor(
    private val repository: GenreDiscoveryRepository,
    private val database: MusicDatabase,
    @ApplicationContext private val context: Context,
    private val loadAiContentFilterPolicy: LoadAiContentFilterPolicyUseCase,
    private val filterAiContent: FilterAiContentUseCase,
) {
    suspend operator fun invoke(
        source: GenreSource,
        endpoint: BrowseEndpoint,
        query: String?,
        continuation: String? = null,
    ): Result<GenrePage> = withContext(Dispatchers.IO) {
        try {
            val page = repository.load(source, endpoint, query, continuation).getOrThrow()
            check(continuation == null || (page.continuation != continuation && page.items.none { it.continuation == continuation })) {
                "Repeated browse continuation"
            }
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideo = context.dataStore.get(HideVideoKey, false)
            val blockedArtists = database.getBlockedArtistIds().toSet()
            val policy = loadAiContentFilterPolicy()
            Result.success(
                GenrePage(
                    title = page.title,
                    thumbnail = page.thumbnail,
                    sections = ImmutableList.copyOf(page.items.mapIndexed { index, section ->
                        GenreSection(
                            id = "${source.name}:${section.endpoint?.browseId.orEmpty()}:${section.endpoint?.params.orEmpty()}:${section.title ?: index}",
                            title = section.title,
                            items = ImmutableList.copyOf(
                                filterAiContent(
                                    section.items.filter { it.id.isNotBlank() && it.title.isNotBlank() }
                                        .filterExplicit(hideExplicit).filterVideo(hideVideo)
                                        .filterBlockedArtists(blockedArtists),
                                    policy,
                                ).distinctBy { it.genreKey() },
                            ),
                            endpoint = section.endpoint,
                            continuation = section.continuation,
                        )
                    }),
                    continuation = page.continuation,
                ),
            )
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            Result.failure(exception)
        }
    }
}
