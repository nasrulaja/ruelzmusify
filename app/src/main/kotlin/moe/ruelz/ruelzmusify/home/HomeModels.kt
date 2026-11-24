/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.ruelz.ruelzmusify.home

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.google.common.collect.ImmutableList
import moe.ruelz.ruelzmusify.constants.QuickPicks
import moe.ruelz.ruelzmusify.constants.QuickPicksDisplayMode
import moe.ruelz.ruelzmusify.db.entities.LocalItem
import moe.ruelz.ruelzmusify.db.entities.Song
import moe.ruelz.ruelzmusify.innertube.models.PlaylistItem
import moe.ruelz.ruelzmusify.innertube.pages.HomePage
import moe.ruelz.ruelzmusify.models.SimilarRecommendation
import moe.ruelz.ruelzmusify.podcast.PodcastPlaybackRequest

sealed interface HomeScreenState {
    data object Loading : HomeScreenState

    @Immutable
    data class Success(
        val uiState: HomeUiState,
    ) : HomeScreenState

    data object Empty : HomeScreenState

    @Immutable
    data class Error(
        @StringRes val messageResId: Int,
    ) : HomeScreenState
}

@Immutable
data class HomeUiState(
    val quickPicks: ImmutableList<Song>,
    val speedDialItems: ImmutableList<LocalItem>,
    val forgottenFavorites: ImmutableList<Song>,
    val keepListening: ImmutableList<LocalItem>,
    val similarRecommendations: ImmutableList<SimilarRecommendation>,
    val accountPlaylists: ImmutableList<PlaylistItem>,
    val homePage: HomePage?,
    val remoteQuickPicks: HomePage.Section?,
    val communitySection: HomePage.Section?,
    val selectedChip: HomePage.Chip?,
    val accountName: String,
    val accountImageUrl: String?,
    val quickPicksMode: QuickPicks,
    val quickPicksDisplayMode: QuickPicksDisplayMode,
    val showCategoryChips: Boolean,
    val showTonalBackdrop: Boolean,
    val isRefreshing: Boolean,
    val isLoadingMore: Boolean,
)

fun HomePage.Section.hasVisibleContent(): Boolean = items.isNotEmpty() || featuredCards.isNotEmpty()

sealed interface HomeAction {
    data object Refresh : HomeAction

    data class SelectChip(
        val chip: HomePage.Chip?,
    ) : HomeAction

    data class LoadMore(
        val continuation: String?,
    ) : HomeAction

    data class OpenRemoteItem(
        val itemId: String,
    ) : HomeAction
}

sealed interface HomeEvent {
    data class OpenPodcast(
        val browseId: String,
    ) : HomeEvent

    @Immutable
    data class PlayPodcastEpisode(
        val request: PodcastPlaybackRequest,
    ) : HomeEvent
}
