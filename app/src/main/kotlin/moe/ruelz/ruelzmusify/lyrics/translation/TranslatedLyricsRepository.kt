/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.ruelz.ruelzmusify.lyrics.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.ruelz.ruelzmusify.db.MusicDatabase
import moe.ruelz.ruelzmusify.db.entities.LyricsEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslatedLyricsRepository
    @Inject
    constructor(
        private val database: MusicDatabase,
    ) {
        suspend fun replaceLyrics(
            mediaId: String,
            lyrics: String,
        ) {
            withContext(Dispatchers.IO) {
                database.withTransaction {
                    replaceLyricsKeepingProvider(
                        id = mediaId,
                        lyrics = lyrics,
                        source = LyricsEntity.Source.TRANSLATION.value,
                    )
                }
            }
        }
    }
