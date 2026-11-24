/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.ruelz.ruelzmusify.lyrics

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.ruelz.ruelzmusify.constants.EnableLrcRedLyricsKey
import moe.ruelz.ruelzmusify.lrcred.LrcRed
import moe.ruelz.ruelzmusify.utils.dataStore
import moe.ruelz.ruelzmusify.utils.get
import java.io.IOException

object LrcRedLyricsProvider : LyricsProvider {
    override val name = "lrc.red"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableLrcRedLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            LrcRed.getLyrics(title = title, artist = artist, durationSeconds = duration).mapCatching { lyrics ->
                if (!LyricsUtils.isTtml(lyrics)) throw IOException("Invalid TTML response from lrc.red")
                lyrics
            }
        }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ): Unit =
        withContext(Dispatchers.IO) {
            LrcRed.getAllLyrics(title = title, artist = artist, durationSeconds = duration) { lyrics ->
                if (LyricsUtils.isTtml(lyrics)) callback(lyrics)
            }
        }
}
