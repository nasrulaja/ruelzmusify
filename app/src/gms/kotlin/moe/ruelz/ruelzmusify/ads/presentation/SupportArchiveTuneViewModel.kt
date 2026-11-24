/*
 * RuelzMusify (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.ruelz.ruelzmusify.ads.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import moe.ruelz.ruelzmusify.ads.domain.OpenSupportPageUseCase
import moe.ruelz.ruelzmusify.ads.domain.SupportPageOpenResult
import javax.inject.Inject

internal sealed interface SupportRuelzMusifyScreenState {
    @Immutable
    data object Loading : SupportRuelzMusifyScreenState

    @Immutable
    data object Success : SupportRuelzMusifyScreenState

    @Immutable
    data object Empty : SupportRuelzMusifyScreenState

    @Immutable
    data class Error(
        val reason: SupportRuelzMusifyError,
    ) : SupportRuelzMusifyScreenState
}

internal enum class SupportRuelzMusifyError {
    PageUnavailable,
}

internal enum class SupportRuelzMusifyUiEvent {
    OpenFailed,
}

@HiltViewModel
internal class SupportRuelzMusifyViewModel
    @Inject
    constructor(
        private val openSupportPage: OpenSupportPageUseCase,
    ) : ViewModel() {
        private val _screenState =
            MutableStateFlow<SupportRuelzMusifyScreenState>(SupportRuelzMusifyScreenState.Success)
        val screenState: StateFlow<SupportRuelzMusifyScreenState> = _screenState.asStateFlow()

        private val eventChannel = Channel<SupportRuelzMusifyUiEvent>(Channel.BUFFERED)
        val events = eventChannel.receiveAsFlow()

        fun onSupportRuelzMusifyClick() {
            if (_screenState.value is SupportRuelzMusifyScreenState.Loading) return
            _screenState.value = SupportRuelzMusifyScreenState.Loading
            when (openSupportPage()) {
                SupportPageOpenResult.Opened -> {
                    _screenState.value = SupportRuelzMusifyScreenState.Success
                }

                SupportPageOpenResult.Unavailable -> {
                    _screenState.value =
                        SupportRuelzMusifyScreenState.Error(SupportRuelzMusifyError.PageUnavailable)
                    eventChannel.trySend(SupportRuelzMusifyUiEvent.OpenFailed)
                }
            }
        }
    }
