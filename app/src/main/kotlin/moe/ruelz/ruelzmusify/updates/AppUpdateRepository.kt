/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.ruelz.ruelzmusify.updates

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.ruelz.ruelzmusify.utils.AppUpdateInstaller
import moe.ruelz.ruelzmusify.utils.ReleaseInfo
import moe.ruelz.ruelzmusify.utils.Updater
import java.io.File
import javax.inject.Inject

class AppUpdateRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend fun releases(forceRefresh: Boolean): List<ReleaseInfo> = withContext(Dispatchers.IO) {
        Updater.getAllReleases(forceRefresh = forceRefresh).getOrThrow()
    }

    suspend fun download(url: String, onProgress: (AppUpdateInstaller.Progress) -> Unit): File =
        AppUpdateInstaller.download(context, url, onProgress)
}
