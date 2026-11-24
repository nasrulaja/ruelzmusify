/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.ruelz.ruelzmusify.ai

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import moe.ruelz.ruelzmusify.constants.AiApiKeyKey
import moe.ruelz.ruelzmusify.constants.AiApiValidationStatus
import moe.ruelz.ruelzmusify.constants.AiApiValidationStatusKey
import moe.ruelz.ruelzmusify.constants.AiCustomEndpointKey
import moe.ruelz.ruelzmusify.constants.AiCustomModelKey
import moe.ruelz.ruelzmusify.constants.AiCustomPromptKey
import moe.ruelz.ruelzmusify.constants.AiProvider
import moe.ruelz.ruelzmusify.constants.AiProviderKey
import moe.ruelz.ruelzmusify.constants.AiRequestParametersKey
import moe.ruelz.ruelzmusify.constants.AiSelectedModelKey
import moe.ruelz.ruelzmusify.extensions.toEnum
import moe.ruelz.ruelzmusify.utils.dataStore
import javax.inject.Inject
import javax.inject.Singleton

data class AiIntegrationSettingsData(
    val provider: AiProvider,
    val apiKey: String,
    val customEndpoint: String,
    val validationStatus: AiApiValidationStatus,
    val selectedModel: String,
    val customModel: String,
    val customPrompt: String,
    val requestParameters: String,
) {
    fun toServiceConfig(): AiServiceConfig =
        AiServiceConfig(
            provider = provider,
            apiKey = apiKey,
            customEndpoint = customEndpoint,
            model = if (provider == AiProvider.CUSTOM) customModel else selectedModel,
            requestParameters = requestParameters,
        )
}

@Singleton
class AiIntegrationSettingsRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun observeSettings(): Flow<AiIntegrationSettingsData> =
            context.dataStore.data.map(::settingsFromPreferences)

        suspend fun currentSettings(): AiIntegrationSettingsData =
            settingsFromPreferences(context.dataStore.data.first())

        suspend fun setProvider(provider: AiProvider) {
            context.dataStore.edit { preferences ->
                val currentProvider = preferences[AiProviderKey].toEnum(AiProvider.NONE)
                if (currentProvider != provider) {
                    preferences[AiSelectedModelKey] = ""
                }
                preferences[AiProviderKey] = provider.name
                preferences[AiApiValidationStatusKey] = AiApiValidationStatus.UNKNOWN.name
            }
        }

        suspend fun setApiKey(apiKey: String) {
            context.dataStore.edit { preferences ->
                preferences[AiApiKeyKey] = apiKey.trim()
                preferences[AiApiValidationStatusKey] = AiApiValidationStatus.UNKNOWN.name
            }
        }

        suspend fun setCustomEndpoint(endpoint: String) {
            context.dataStore.edit { preferences ->
                preferences[AiCustomEndpointKey] = endpoint.trim()
                preferences[AiApiValidationStatusKey] = AiApiValidationStatus.UNKNOWN.name
            }
        }

        suspend fun setSelectedModel(model: String) {
            context.dataStore.edit { preferences ->
                preferences[AiSelectedModelKey] = model
                preferences[AiApiValidationStatusKey] = AiApiValidationStatus.UNKNOWN.name
            }
        }

        suspend fun setCustomModel(model: String) {
            context.dataStore.edit { preferences ->
                preferences[AiCustomModelKey] = model
                preferences[AiApiValidationStatusKey] = AiApiValidationStatus.UNKNOWN.name
            }
        }

        suspend fun setCustomPrompt(prompt: String) {
            context.dataStore.edit { preferences ->
                preferences[AiCustomPromptKey] = prompt.trim()
            }
        }

        suspend fun setRequestParameters(value: String) {
            val normalized = AiRequestParameters.normalize(value)
            context.dataStore.edit { preferences ->
                preferences[AiRequestParametersKey] = normalized
                preferences[AiApiValidationStatusKey] = AiApiValidationStatus.UNKNOWN.name
            }
        }

        suspend fun setValidationStatus(status: AiApiValidationStatus, config: AiServiceConfig): Boolean {
            var updated = false
            context.dataStore.edit { preferences ->
                if (settingsFromPreferences(preferences).toServiceConfig() == config) {
                    preferences[AiApiValidationStatusKey] = status.name
                    updated = true
                }
            }
            return updated
        }

        suspend fun fetchModels(config: AiServiceConfig): List<AiModelOption> =
            AiTextService.fetchModels(config)

        suspend fun test(config: AiServiceConfig) {
            AiTextService.test(config)
        }

        private fun settingsFromPreferences(preferences: Preferences): AiIntegrationSettingsData =
            AiIntegrationSettingsData(
                provider = preferences[AiProviderKey].toEnum(AiProvider.NONE),
                apiKey = preferences[AiApiKeyKey].orEmpty(),
                customEndpoint = preferences[AiCustomEndpointKey].orEmpty(),
                validationStatus =
                    preferences[AiApiValidationStatusKey].toEnum(AiApiValidationStatus.UNKNOWN),
                selectedModel = preferences[AiSelectedModelKey].orEmpty(),
                customModel = preferences[AiCustomModelKey].orEmpty(),
                customPrompt = preferences[AiCustomPromptKey].orEmpty(),
                requestParameters = preferences[AiRequestParametersKey].orEmpty(),
            )
    }
