/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.ruelz.ruelzmusify.lyrics

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import moe.ruelz.ruelzmusify.constants.EnableBetterLyricsKey
import moe.ruelz.ruelzmusify.constants.EnableBetterLyricsPortatoKey
import moe.ruelz.ruelzmusify.constants.EnableBinimumLyricsKey
import moe.ruelz.ruelzmusify.constants.EnableKugouKey
import moe.ruelz.ruelzmusify.constants.EnableLrcLibKey
import moe.ruelz.ruelzmusify.constants.EnableLrcRedLyricsKey
import moe.ruelz.ruelzmusify.constants.EnableMegalobizLyricsKey
import moe.ruelz.ruelzmusify.constants.EnablePaxsenixAppleMusicLyricsKey
import moe.ruelz.ruelzmusify.constants.EnablePaxsenixLyricsKey
import moe.ruelz.ruelzmusify.constants.EnablePaxsenixMusixmatchLyricsKey
import moe.ruelz.ruelzmusify.constants.EnablePaxsenixSpotifyLyricsKey
import moe.ruelz.ruelzmusify.constants.EnableSimpMusicLyricsKey
import moe.ruelz.ruelzmusify.constants.EnableUnisonLyricsKey
import moe.ruelz.ruelzmusify.constants.EnableYouLyPlusLyricsKey
import moe.ruelz.ruelzmusify.constants.LyricsProviderOrderKey
import moe.ruelz.ruelzmusify.constants.PreferredLyricsProvider
import moe.ruelz.ruelzmusify.constants.deserializeLyricsProviderOrder
import moe.ruelz.ruelzmusify.utils.dataStore
import javax.inject.Inject
import javax.inject.Singleton

data class LyricsProviderPreferences(
    val order: ImmutableList<PreferredLyricsProvider>,
    val enabledProviders: ImmutableSet<PreferredLyricsProvider>,
    val paxsenixEnabled: Boolean,
)

data class LyricsProviderSelection(
    val providers: ImmutableList<PreferredLyricsProvider>,
    val selectedProviders: ImmutableList<PreferredLyricsProvider>,
)

@Singleton
class LyricsProviderSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.dataStore
    private val providerKeys = mapOf(
        PreferredLyricsProvider.BETTER_LYRICS to EnableBetterLyricsKey,
        PreferredLyricsProvider.BETTER_LYRICS_PORTATO to EnableBetterLyricsPortatoKey,
        PreferredLyricsProvider.YOULY_PLUS to EnableYouLyPlusLyricsKey,
        PreferredLyricsProvider.BINIMUM to EnableBinimumLyricsKey,
        PreferredLyricsProvider.LRC_RED to EnableLrcRedLyricsKey,
        PreferredLyricsProvider.LRCLIB to EnableLrcLibKey,
        PreferredLyricsProvider.KUGOU to EnableKugouKey,
        PreferredLyricsProvider.MEGALOBIZ to EnableMegalobizLyricsKey,
        PreferredLyricsProvider.SIMPMUSIC to EnableSimpMusicLyricsKey,
        PreferredLyricsProvider.UNISON to EnableUnisonLyricsKey,
        PreferredLyricsProvider.PAXSENIX_APPLE_MUSIC to EnablePaxsenixAppleMusicLyricsKey,
        PreferredLyricsProvider.PAXSENIX_SPOTIFY to EnablePaxsenixSpotifyLyricsKey,
        PreferredLyricsProvider.PAXSENIX_MUSIXMATCH to EnablePaxsenixMusixmatchLyricsKey,
    )

    fun observe(): Flow<LyricsProviderPreferences> = dataStore.data.map { preferences ->
        LyricsProviderPreferences(
            order = ImmutableList.copyOf(deserializeLyricsProviderOrder(preferences[LyricsProviderOrderKey])),
            enabledProviders = ImmutableSet.copyOf(providerKeys.keys.filter { preferences[providerKeys.getValue(it)] ?: true }),
            paxsenixEnabled = preferences[EnablePaxsenixLyricsKey] ?: true,
        )
    }.distinctUntilChanged()

    suspend fun setEnabledProviders(
        providers: Set<PreferredLyricsProvider>,
        paxsenixEnabled: Boolean,
    ) {
        withContext(Dispatchers.IO) {
            dataStore.edit { preferences ->
                providerKeys.forEach { (provider, key) ->
                    preferences[key] = provider in providers
                }
                preferences[EnablePaxsenixLyricsKey] = paxsenixEnabled
            }
        }
    }
}

class LyricsProviderSettingsUseCase @Inject constructor(
    private val repository: LyricsProviderSettingsRepository,
) {
    private val paxsenixProviders = setOf(
        PreferredLyricsProvider.PAXSENIX_APPLE_MUSIC,
        PreferredLyricsProvider.PAXSENIX_SPOTIFY,
        PreferredLyricsProvider.PAXSENIX_MUSIXMATCH,
    )

    fun observe(): Flow<LyricsProviderSelection> = repository.observe().map { preferences ->
        LyricsProviderSelection(
            providers = preferences.order,
            selectedProviders = ImmutableList.copyOf(
                preferences.order.filter { provider ->
                    provider in preferences.enabledProviders &&
                        (provider !in paxsenixProviders || preferences.paxsenixEnabled)
                },
            ),
        )
    }.distinctUntilChanged()

    suspend fun setEnabledProviders(providers: ImmutableList<PreferredLyricsProvider>) {
        repository.setEnabledProviders(
            providers = providers.toSet(),
            paxsenixEnabled = providers.any { it in paxsenixProviders },
        )
    }
}
