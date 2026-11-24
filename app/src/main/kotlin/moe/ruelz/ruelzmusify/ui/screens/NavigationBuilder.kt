/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.ruelz.ruelzmusify.ui.screens

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import moe.ruelz.ruelzmusify.BuildConfig
import moe.ruelz.ruelzmusify.updates.AppUpdateRelease
import moe.ruelz.ruelzmusify.musicrecognition.MusicRecognitionRoute
import moe.ruelz.ruelzmusify.musicrecognition.MusicRecognitionDetailsRoute
import moe.ruelz.ruelzmusify.ui.screens.BrowseScreen
import moe.ruelz.ruelzmusify.ui.screens.artist.ArtistAlbumsScreen
import moe.ruelz.ruelzmusify.ui.screens.artist.ArtistItemsScreen
import moe.ruelz.ruelzmusify.ui.screens.artist.ArtistScreen
import moe.ruelz.ruelzmusify.ui.screens.artist.ArtistSongsScreen
import moe.ruelz.ruelzmusify.ui.screens.library.LibraryScreen
import moe.ruelz.ruelzmusify.ui.screens.library.LocalSongScreen
import moe.ruelz.ruelzmusify.ui.screens.musicrecognition.MusicRecognitionScreen
import moe.ruelz.ruelzmusify.ui.screens.musicrecognition.MusicRecognitionDetailsScreen
import moe.ruelz.ruelzmusify.ui.screens.playlist.AutoPlaylistScreen
import moe.ruelz.ruelzmusify.ui.screens.playlist.CachePlaylistScreen
import moe.ruelz.ruelzmusify.ui.screens.playlist.LocalPlaylistScreen
import moe.ruelz.ruelzmusify.ui.screens.playlist.OnlinePlaylistScreen
import moe.ruelz.ruelzmusify.ui.screens.playlist.SpotifyLikedSongsScreen
import moe.ruelz.ruelzmusify.ui.screens.playlist.SpotifyPlaylistScreen
import moe.ruelz.ruelzmusify.ui.screens.playlist.TopPlaylistScreen
import moe.ruelz.ruelzmusify.ui.screens.podcast.PodcastRoute
import moe.ruelz.ruelzmusify.ui.screens.podcast.PodcastScreen
import moe.ruelz.ruelzmusify.ui.screens.search.OnlineSearchResult
import moe.ruelz.ruelzmusify.ui.screens.search.OnlineSearchResultArgument
import moe.ruelz.ruelzmusify.ui.screens.search.OnlineSearchResultRoute
import moe.ruelz.ruelzmusify.ui.screens.search.OnlineSearchResultRoutePrefix
import moe.ruelz.ruelzmusify.ui.screens.search.SearchScreen
import moe.ruelz.ruelzmusify.ui.screens.settings.AboutScreen
import moe.ruelz.ruelzmusify.ui.screens.settings.AccountSettings
import moe.ruelz.ruelzmusify.ui.screens.settings.AiIntegrationSettings
import moe.ruelz.ruelzmusify.ui.screens.settings.AodCustomizedScreen
import moe.ruelz.ruelzmusify.ui.screens.settings.AndroidAutoSettings
import moe.ruelz.ruelzmusify.ui.screens.settings.AppUpdateRoute
import moe.ruelz.ruelzmusify.ui.screens.settings.AppUpdateScreen
import moe.ruelz.ruelzmusify.ui.screens.settings.CanvasSettings
import moe.ruelz.ruelzmusify.ui.screens.settings.AppearanceSettings
import moe.ruelz.ruelzmusify.ui.screens.settings.BackupAndRestore
import moe.ruelz.ruelzmusify.ui.screens.settings.ChangelogScreen
import moe.ruelz.ruelzmusify.ui.screens.settings.ContentSettings
import moe.ruelz.ruelzmusify.ui.screens.settings.CustomizeBackground
import moe.ruelz.ruelzmusify.ui.screens.settings.DebugSettings
import moe.ruelz.ruelzmusify.ui.screens.settings.DiscordSettings
import moe.ruelz.ruelzmusify.ui.screens.settings.HiddenPlaylistsScreen
import moe.ruelz.ruelzmusify.ui.screens.settings.IconScreen
import moe.ruelz.ruelzmusify.ui.screens.settings.IntegrationScreen
import moe.ruelz.ruelzmusify.ui.screens.settings.InternetSettings
import moe.ruelz.ruelzmusify.ui.screens.settings.LastFMSettings
import moe.ruelz.ruelzmusify.ui.screens.settings.LocalSongSettings
import moe.ruelz.ruelzmusify.ui.screens.settings.LocalSongSettingsRoute
import moe.ruelz.ruelzmusify.ui.screens.settings.LogcatScreen
import moe.ruelz.ruelzmusify.ui.screens.settings.LyricsAnimationSettings
import moe.ruelz.ruelzmusify.ui.screens.settings.LyricsSettings
import moe.ruelz.ruelzmusify.ui.screens.settings.MediaNotificationSettings
import moe.ruelz.ruelzmusify.ui.screens.settings.MusicTogetherScreen
import moe.ruelz.ruelzmusify.ui.screens.settings.PalettePickerScreen
import moe.ruelz.ruelzmusify.ui.screens.settings.PlayerSettings
import moe.ruelz.ruelzmusify.ui.screens.settings.PrivacySettings
import moe.ruelz.ruelzmusify.ui.screens.settings.SettingsScreen
import moe.ruelz.ruelzmusify.ui.screens.settings.StorageSettings
import moe.ruelz.ruelzmusify.ui.screens.settings.ThemeCreatorScreen
import moe.ruelz.ruelzmusify.ui.screens.settings.UpdateScreen
import moe.ruelz.ruelzmusify.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    homeViewModel: HomeViewModel,
    latestVersionName: () -> String?,
    disableAnimations: Boolean = false,
    onUpdateReleaseChecked: (AppUpdateRelease?) -> Unit = {},
    homeScrollConnection: NestedScrollConnection? = null,
    searchScrollConnection: NestedScrollConnection? = null,
    libraryScrollBehavior: TopAppBarScrollBehavior,
) {
    composable(Screens.Home.route) {
        HomeScreen(
            navController = navController,
            viewModel = homeViewModel,
            headerScrollConnection = homeScrollConnection,
        )
    }
    composable(
        Screens.Library.route,
    ) {
        LibraryScreen(navController = navController, scrollBehavior = libraryScrollBehavior)
    }
    composable(Screens.Search.route) {
        SearchScreen(
            navController = navController,
            onSearchClick = {
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("openSearch", true)
            },
            headerScrollConnection = searchScrollConnection,
        )
    }
    composable("local_songs") {
        LocalSongScreen(navController)
    }
    composable(LocalSongSettingsRoute) { backStackEntry ->
        val localSongsEntry = remember(backStackEntry) { navController.getBackStackEntry("local_songs") }
        LocalSongSettings(navController, hiltViewModel(localSongsEntry))
    }
    composable("history") {
        HistoryScreen(navController)
    }
    composable("stats") {
        StatsScreen(navController)
    }
    composable("news") {
        NewsScreen(navController)
    }
    composable(
        route = "view_news/{newsId}",
        arguments =
            listOf(
                navArgument("newsId") { type = NavType.StringType },
            ),
    ) {
        ViewNewsScreen(navController)
    }
    composable(
        route = "year_in_music?year={year}",
        arguments =
            listOf(
                navArgument("year") {
                    type = NavType.IntType
                    defaultValue = -1
                },
            ),
    ) { backStackEntry ->
        val selectedYear = backStackEntry.arguments?.getInt("year")?.takeIf { it > 0 }
        YearInMusicScreen(
            navController = navController,
            initialYear = selectedYear,
        )
    }
    composable(MusicRecognitionRoute) {
        MusicRecognitionScreen(navController)
    }
    composable(MusicRecognitionDetailsRoute) {
        MusicRecognitionDetailsScreen(navController)
    }
    composable(Screens.MoodAndGenres.route) {
        MoodAndGenresScreen(navController)
    }
    composable("account") {
        AccountScreen(navController, scrollBehavior)
    }
    composable("new_release") {
        NewReleaseScreen(navController, scrollBehavior)
    }
    composable("charts_screen") {
        ChartsScreen(navController)
    }
    composable(
        route = "browse/{browseId}",
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                },
            ),
    ) {
        BrowseScreen(
            navController,
            scrollBehavior,
        )
    }
    composable(
        route = OnlineSearchResultRoute,
        arguments =
            listOf(
                navArgument(OnlineSearchResultArgument) {
                    type = NavType.StringType
                },
            ),
        enterTransition = {
            if (disableAnimations) {
                fadeIn(tween(0))
            } else {
                fadeIn(tween(250))
            }
        },
        exitTransition = {
            if (disableAnimations) {
                fadeOut(tween(0))
            } else if (targetState.destination.route?.startsWith(OnlineSearchResultRoutePrefix) == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            if (disableAnimations) {
                fadeIn(tween(0))
            } else if (initialState.destination.route?.startsWith(OnlineSearchResultRoutePrefix) == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = {
            if (disableAnimations) {
                fadeOut(tween(0))
            } else {
                fadeOut(tween(200))
            }
        },
    ) {
        OnlineSearchResult(navController = navController)
    }
    composable(
        route = "album/{albumId}",
        arguments =
            listOf(
                navArgument("albumId") {
                    type = NavType.StringType
                },
            ),
    ) {
        AlbumScreen(navController, scrollBehavior)
    }
    composable(
        route = PodcastRoute,
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                },
            ),
    ) {
        PodcastScreen(navController)
    }
    composable(
        route = "artist/{artistId}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/songs",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistSongsScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/albums",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistAlbumsScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/items?browseId={browseId}&params={params}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        ArtistItemsScreen(navController, scrollBehavior)
    }
    composable(
        route = "online_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        OnlinePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "local_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        LocalPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "spotify_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        SpotifyPlaylistScreen(navController, scrollBehavior)
    }
    composable(route = "spotify_liked_songs") {
        SpotifyLikedSongsScreen(navController, scrollBehavior)
    }
    composable(
        route = "auto_playlist/{playlist}?tab={tab}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
                navArgument("tab") {
                    type = NavType.StringType
                    defaultValue = "downloaded"
                },
            ),
    ) {
        AutoPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "cache_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        CachePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "top_playlist/{top}",
        arguments =
            listOf(
                navArgument("top") {
                    type = NavType.StringType
                },
            ),
    ) {
        TopPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "youtube_browse/{browseId}?params={params}&genreTitle={genreTitle}",
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("genreTitle") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) {
        YouTubeBrowseScreen(navController)
    }
    composable("settings") {
        SettingsScreen(
            navController = navController,
            latestVersionName = latestVersionName() ?: BuildConfig.VERSION_NAME,
            viewModel = homeViewModel,
        )
    }
    composable("settings/account") {
        AccountSettings(
            navController = navController,
            latestVersionName = latestVersionName() ?: BuildConfig.VERSION_NAME,
            viewModel = homeViewModel,
        )
    }
    composable("settings/hidden_playlists") {
        HiddenPlaylistsScreen(navController)
    }
    composable("settings/appearance") {
        AppearanceSettings(navController)
    }
    composable("settings/appearance/icon") {
        IconScreen(navController)
    }
    composable("settings/appearance/aod_customized") {
        AodCustomizedScreen(navController)
    }
    composable("settings/appearance/palette_picker") {
        PalettePickerScreen(navController)
    }
    composable("settings/appearance/lyrics_animations") {
        LyricsAnimationSettings(navController)
    }
    composable("settings/appearance/theme_creator") {
        ThemeCreatorScreen(navController)
    }
    composable("settings/content") {
        ContentSettings(navController)
    }
    composable("settings/lyrics") {
        LyricsSettings(navController)
    }
    composable("settings/internet") {
        InternetSettings(navController)
    }
    composable("settings/player") {
        PlayerSettings(navController)
    }
    composable("settings/media_notification") {
        MediaNotificationSettings(navController)
    }
    composable("settings/modules") {
        moe.ruelz.ruelzmusify.ui.screens.settings.ModulesSettings(navController)
    }
    composable("settings/canvas") {
        CanvasSettings(navController)
    }
    composable("settings/android_auto") {
        AndroidAutoSettings(navController)
    }
    composable("settings/storage") {
        StorageSettings(navController)
    }
    composable("settings/privacy") {
        PrivacySettings(navController)
    }
    composable("settings/backup_restore") {
        BackupAndRestore(navController)
    }
    composable("settings/discord") {
        DiscordSettings(navController)
    }
    composable("settings/integration") {
        IntegrationScreen(navController)
    }
    composable("settings/ai_integration") {
        AiIntegrationSettings(navController)
    }
    composable("settings/music_together") {
        MusicTogetherScreen(navController)
    }
    composable("settings/lastfm") {
        LastFMSettings(navController)
    }
    composable("settings/discord/experimental") {
        moe.ruelz.ruelzmusify.ui.screens.settings
            .DiscordExperimental(navController)
    }
    composable("settings/misc") {
        DebugSettings(navController)
    }
    composable("settings/logcat") {
        LogcatScreen(navController)
    }
    if (BuildConfig.UPDATER_AVAILABLE) {
        composable(
            route = "$AppUpdateRoute?forceRefresh={forceRefresh}",
            arguments = listOf(
                navArgument("forceRefresh") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) {
            AppUpdateScreen(navController, onReleaseChecked = onUpdateReleaseChecked)
        }
        composable("settings/update") {
            UpdateScreen(navController, latestVersion = latestVersionName())
        }
    }
    composable("settings/changelog") {
        ChangelogScreen(navController)
    }
    composable("settings/about") {
        AboutScreen(navController)
    }
    composable("customize_background") {
        CustomizeBackground(navController)
    }
    composable(
        route = "$LOGIN_ROUTE?$LOGIN_URL_ARGUMENT={$LOGIN_URL_ARGUMENT}",
        arguments =
            listOf(
                navArgument(LOGIN_URL_ARGUMENT) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        LoginScreen(
            navController,
            startUrl = backStackEntry.arguments?.getString(LOGIN_URL_ARGUMENT)?.let(Uri::decode),
        )
    }
}
