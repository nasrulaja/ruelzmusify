/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.ruelz.ruelzmusify.lyrics

import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import moe.ruelz.ruelzmusify.constants.LyricsClickKey
import moe.ruelz.ruelzmusify.constants.LyricsLineBlurKey
import moe.ruelz.ruelzmusify.constants.LyricsLineSpacingKey
import moe.ruelz.ruelzmusify.constants.LyricsRomanizeChineseKey
import moe.ruelz.ruelzmusify.constants.LyricsRomanizeHindiKey
import moe.ruelz.ruelzmusify.constants.LyricsRomanizeJapaneseKey
import moe.ruelz.ruelzmusify.constants.LyricsRomanizeKoreanKey
import moe.ruelz.ruelzmusify.constants.LyricsRomanizeOtherLanguagesKey
import moe.ruelz.ruelzmusify.constants.LyricsScrollKey
import moe.ruelz.ruelzmusify.constants.LyricsTextSizeKey
import moe.ruelz.ruelzmusify.constants.LyricsV2BounceFactorKey
import moe.ruelz.ruelzmusify.constants.LyricsV2FillTransitionWidthKey
import moe.ruelz.ruelzmusify.constants.LyricsV2GlowFactorKey
import moe.ruelz.ruelzmusify.constants.LyricsV2LrcBounceEnabledKey
import moe.ruelz.ruelzmusify.constants.TranslateLyricsKey
import moe.ruelz.ruelzmusify.db.MusicDatabase
import moe.ruelz.ruelzmusify.utils.dataStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRenderingRepository
    @Inject
    constructor(
        private val database: MusicDatabase,
        @ApplicationContext context: Context,
    ) {
        private val dataStore = context.dataStore
        private val preferences = dataStore.data

        suspend fun setShowTranslations(show: Boolean) {
            dataStore.edit { values -> values[TranslateLyricsKey] = show }
        }

        fun observeLyrics(mediaId: String): Flow<String?> =
            database
                .lyrics(mediaId)
                .map { entity -> entity?.lyrics }
                .distinctUntilChanged()

        fun observePreferences(): Flow<LyricsRenderingPreferences> =
            preferences
                .map { values ->
                    LyricsRenderingPreferences(
                        clickEnabled = values[LyricsClickKey] ?: true,
                        showTranslations = values[TranslateLyricsKey] ?: true,
                        scrollEnabled = values[LyricsScrollKey] ?: true,
                        textSizeSp = values[LyricsTextSizeKey] ?: 26f,
                        lineSpacing = values[LyricsLineSpacingKey] ?: 1.3f,
                        lineBlurEnabled = values[LyricsLineBlurKey] ?: true,
                        v2BounceFactor = values[LyricsV2BounceFactorKey] ?: 1f,
                        v2GlowFactor = values[LyricsV2GlowFactorKey] ?: 1f,
                        v2FillTransitionWidthDp = values[LyricsV2FillTransitionWidthKey] ?: 8f,
                        v2LrcBounceEnabled = values[LyricsV2LrcBounceEnabledKey] ?: true,
                        romanization =
                            LyricsRomanizationPreferences(
                                romanizeJapanese = values[LyricsRomanizeJapaneseKey] ?: true,
                                romanizeKorean = values[LyricsRomanizeKoreanKey] ?: true,
                                romanizeChinese = values[LyricsRomanizeChineseKey] ?: true,
                                romanizeHindi = values[LyricsRomanizeHindiKey] ?: true,
                                romanizeOther = values[LyricsRomanizeOtherLanguagesKey] ?: true,
                            ),
                    )
                }.distinctUntilChanged()
    }
