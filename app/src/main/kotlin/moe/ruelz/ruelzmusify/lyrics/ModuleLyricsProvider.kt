/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.ruelz.ruelzmusify.lyrics

import android.content.Context
import kotlinx.coroutines.CancellationException
import moe.ruelz.ruelzmusify.modules.InstalledModule
import moe.ruelz.ruelzmusify.modules.ModuleContentRepository
import moe.ruelz.ruelzmusify.modules.ModuleException
import moe.ruelz.ruelzmusify.modules.ModuleFailure
import moe.ruelz.ruelzmusify.modules.ModuleLoadStage
import moe.ruelz.ruelzmusify.modules.ModuleLyrics
import moe.ruelz.ruelzmusify.modules.ModuleLyricsFormat
import moe.ruelz.ruelzmusify.modules.displayLabel
import moe.ruelz.ruelzmusify.modules.engineId
import java.io.IOException

class ModuleLyricsProvider(
    private val module: InstalledModule,
    private val content: ModuleContentRepository,
) : LyricsProvider {
    override val name: String = module.displayLabel

    override val identity: String = "module:${module.engineId}"

    override fun isEnabled(context: Context): Boolean = true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = getAttributedLyrics(id, title, artist, album, duration).map(ProviderLyrics::lyrics)

    override suspend fun getAttributedLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<ProviderLyrics> = fetch(id, title, artist, album, duration).mapCatching { values ->
        values.firstOrNull() ?: throw ModuleLyricsNotFoundException()
    }

    override suspend fun getAllAttributedLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (ProviderLyrics) -> Unit,
    ) {
        fetch(id, title, artist, album, duration)
            .onSuccess { values -> values.forEach(callback) }
            .onFailure { if (it is IOException) throw it }
    }

    private suspend fun fetch(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<List<ProviderLyrics>> =
        try {
            val song = content.song(id, title, listOf(artist), album, duration)
            if (song == null) {
                Result.success(emptyList())
            } else {
                Result.success(content.lyrics(module, song).map { it.toProviderLyrics() })
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: ModuleException) {
            Result.failure(
                if (error.failure == ModuleFailure.NETWORK || error.failure == ModuleFailure.TIMEOUT) {
                    IOException("Module lyrics are temporarily unavailable", error)
                } else {
                    error
                },
            )
        }

    private fun ModuleLyrics.toProviderLyrics(): ProviderLyrics {
        val normalized = LyricsUtils.normalizeLyricsText(text)
        val ttml = LyricsUtils.isTtml(normalized)
        val valid = when (format) {
            ModuleLyricsFormat.TTML -> ttml
            ModuleLyricsFormat.LRC -> !ttml && LyricsUtils.isLineSyncedLrc(normalized)
            ModuleLyricsFormat.SYLLABLE -> !ttml && LyricsUtils.isLineSyncedLrc(normalized) && LyricsUtils.hasWordSyncedLyrics(normalized)
            ModuleLyricsFormat.PLAIN -> !normalized.startsWith("<") && !LyricsUtils.isLineSyncedLrc(normalized)
        }
        if (!valid) {
            throw ModuleException(ModuleFailure.EXECUTION, stage = ModuleLoadStage.PROVIDER, diagnosticCode = "LYRICS_FORMAT_MISMATCH")
        }
        return ProviderLyrics(lyrics = normalized, author = author, sourceUrl = sourceUrl)
    }
}

private class ModuleLyricsNotFoundException : IllegalStateException("The module returned no lyrics")
