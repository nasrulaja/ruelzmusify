/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.ruelz.ruelzmusify.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.ruelz.ruelzmusify.aicontentfilter.FilterAiContentUseCase
import moe.ruelz.ruelzmusify.aicontentfilter.LoadAiContentFilterPolicyUseCase
import moe.ruelz.ruelzmusify.constants.HideExplicitKey
import moe.ruelz.ruelzmusify.constants.HideVideoKey
import moe.ruelz.ruelzmusify.db.MusicDatabase
import moe.ruelz.ruelzmusify.extensions.filterBlockedArtists
import moe.ruelz.ruelzmusify.innertube.YouTube
import moe.ruelz.ruelzmusify.innertube.models.filterExplicit
import moe.ruelz.ruelzmusify.innertube.models.filterVideo
import moe.ruelz.ruelzmusify.innertube.pages.ExplorePage
import moe.ruelz.ruelzmusify.utils.dataStore
import moe.ruelz.ruelzmusify.utils.get
import moe.ruelz.ruelzmusify.utils.reportException
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel
    @Inject
    constructor(
        @ApplicationContext val context: Context,
        val database: MusicDatabase,
        private val loadAiContentFilterPolicy: LoadAiContentFilterPolicyUseCase,
        private val filterAiContent: FilterAiContentUseCase,
    ) : ViewModel() {
        val explorePage = MutableStateFlow<ExplorePage?>(null)

        private suspend fun load() {
            YouTube
                .explore()
                .onSuccess { page ->
                    val blockedArtistIds = database.getBlockedArtistIds().toSet()
                    val aiContentFilterPolicy = loadAiContentFilterPolicy()
                    val artists: MutableMap<Int, String> = mutableMapOf()
                    val favouriteArtists: MutableMap<Int, String> = mutableMapOf()
                    database.allArtistsByPlayTime().first().let { list ->
                        var favIndex = 0
                        for ((artistsIndex, artist) in list.withIndex()) {
                            artists[artistsIndex] = artist.id
                            if (artist.artist.bookmarkedAt != null) {
                                favouriteArtists[favIndex] = artist.id
                                favIndex++
                            }
                        }
                    }
                    explorePage.value =
                        page.copy(
                            newReleaseAlbums =
                                filterAiContent(
                                    page.newReleaseAlbums
                                        .sortedBy { album ->
                                            val artistIds = album.artists.orEmpty().mapNotNull { it.id }
                                            val firstArtistKey =
                                                artistIds.firstNotNullOfOrNull { artistId ->
                                                    if (artistId in favouriteArtists.values) {
                                                        favouriteArtists.entries.firstOrNull { it.value == artistId }?.key
                                                    } else {
                                                        artists.entries.firstOrNull { it.value == artistId }?.key
                                                    }
                                                } ?: Int.MAX_VALUE
                                            firstArtistKey
                                        }.filterExplicit(
                                            context.dataStore.get(HideExplicitKey, false),
                                        ).filterVideo(context.dataStore.get(HideVideoKey, false))
                                        .filterBlockedArtists(blockedArtistIds),
                                    aiContentFilterPolicy,
                                ),
                        )
                }.onFailure {
                    reportException(it)
                }
        }

        init {
            viewModelScope.launch(Dispatchers.IO) {
                load()
            }
        }
    }
