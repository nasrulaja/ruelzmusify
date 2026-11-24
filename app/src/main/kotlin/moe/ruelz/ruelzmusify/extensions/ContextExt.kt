/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.ruelz.ruelzmusify.extensions

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import moe.ruelz.ruelzmusify.constants.InnerTubeCookieKey
import moe.ruelz.ruelzmusify.constants.YtmSyncKey
import moe.ruelz.ruelzmusify.innertube.utils.hasYouTubeLoginCookie
import moe.ruelz.ruelzmusify.utils.dataStore
import moe.ruelz.ruelzmusify.utils.get

fun Context.isSyncEnabled(): Boolean = dataStore.get(YtmSyncKey, true) && isUserLoggedIn()

fun Context.isUserLoggedIn(): Boolean {
    val cookie = dataStore[InnerTubeCookieKey] ?: ""
    return hasYouTubeLoginCookie(cookie) && isInternetConnected()
}

fun Context.isInternetConnected(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
}
