package com.rx.geminipro.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.webkit.WebView
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rx.geminipro.components.getMermaidDiagramPrefixes
import com.rx.geminipro.data.UserPreferencesRepository
import com.rx.geminipro.utils.services.GoogleServices
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class GeminiViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val googleServices: GoogleServices,
    application: Application
) : AndroidViewModel(application) {
    private val  _uiState = MutableStateFlow(GeminiUiState())
    val uiState: StateFlow<GeminiUiState> = _uiState.asStateFlow()

    private val _sideEffectChannel = Channel<GeminiSideEffect>()
    val sideEffectFlow = _sideEffectChannel.receiveAsFlow()

    var webViewState = mutableStateOf<WebView?>(null)
        private set

    init {
        viewModelScope.launch {
            userPreferencesRepository.isMenuLeftFlow.collect { isLeft ->
                _uiState.update {
                    it.copy(isMenuLeft = isLeft)
                }
            }
        }
    }

    fun onEvent(event: GeminiUiEvent){
        when(event){
            is GeminiUiEvent.ApplicationReady -> handleApplicationReady()
            is GeminiUiEvent.OpenDocsClicked -> openDocs()
            is GeminiUiEvent.KeepScreenOnToggled -> keepScreenOn()
            is GeminiUiEvent.SplitScreenToggled -> _uiState.update {
                it.copy(isSplitScreen = !it.isSplitScreen)
            }
            is GeminiUiEvent.SaveToFileClicked -> saveToFile()
            is GeminiUiEvent.OpenInBrowserClicked -> openInBrowser()
            is GeminiUiEvent.SharePageClicked -> sharePage()
            is GeminiUiEvent.CopyLinkClicked -> copyLink()
            is GeminiUiEvent.ReloadPageClicked -> reloadPage()
            is GeminiUiEvent.GoForwardClicked -> goForward()
            is GeminiUiEvent.MenuPositionChanged -> setMenuPosition(event.isLeft)
            is GeminiUiEvent.BackButtonPressed -> handleBackPress()
            is GeminiUiEvent.KeyboardVisibilityChanged -> {
                _uiState.update { it.copy(isKeyboardVisible = event.isVisible) }
            }
        }
    }

    fun onClipboardTextChanged(text: String) {
        val type = when {
            getMermaidDiagramPrefixes().any { text.startsWith(it) } -> ClipboardContentType.DIAGRAM
            text.lowercase().startsWith("<!doctype html") -> ClipboardContentType.HTML
            else -> ClipboardContentType.NONE
        }
        _uiState.update { it.copy(clipboardContentType = type) }

        if (type != ClipboardContentType.NONE) {
            viewModelScope.launch {
                delay(6500)
                if (_uiState.value.clipboardContentType == type) {
                    _uiState.update { it.copy(clipboardContentType = ClipboardContentType.NONE) }
                }
            }
        }
    }

    fun onWebViewNavigation() {
        val canGoBack = webViewState.value?.canGoBack() ?: false
        _uiState.update { it.copy(canWebViewGoBack = canGoBack) }
    }

    private fun handleBackPress() {
        webViewState.value?.let { webView ->
            if (webView.canGoBack()) {
                webView.goBack()
            }
        }
    }


    private fun handleApplicationReady() {
        if (_uiState.value.isApplicationReady) return

        _uiState.update { it.copy(isApplicationReady = true) }
    }

    private fun setMenuPosition(isLeft: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveMenuPosition(isLeft)
        }
    }

    private fun openDocs()
    {
        googleServices.openGoogleDocs(getApplication())
    }

    private fun keepScreenOn()
    {
        _uiState.update { it.copy(isKeepScreenOn = !it.isKeepScreenOn) }

        viewModelScope.launch {
            if(uiState.value.isKeepScreenOn)
                _sideEffectChannel.send(GeminiSideEffect.ShowToast("Caffeine mode was turned on"))
            else
                _sideEffectChannel.send(GeminiSideEffect.ShowToast("Caffeine mode was turned off"))
        }
    }

    private fun saveToFile()
    {
        viewModelScope.launch {
            _sideEffectChannel.send(GeminiSideEffect.LaunchSaveToFile)
        }
    }

    private fun openInBrowser()
    {
        webViewState.value?.url?.let { url ->
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            viewModelScope.launch {
                _sideEffectChannel.send(GeminiSideEffect.LaunchIntent(intent))
            }
        }
    }

    private fun sharePage()
    {
        webViewState.value?.url?.let { url ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, url)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(intent, "Share URL")
            viewModelScope.launch {
                _sideEffectChannel.send(GeminiSideEffect.LaunchIntent(shareIntent))
            }
        }
    }

    private fun copyLink()
    {
        val url = webViewState.value?.url
        if (url != null) {
            val clipboardManager = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied URL", url)
            clipboardManager.setPrimaryClip(clip)

            viewModelScope.launch {
                _sideEffectChannel.send(GeminiSideEffect.ShowToast("Link copied!"))
            }
        } else {
            viewModelScope.launch {
                _sideEffectChannel.send(GeminiSideEffect.ShowToast("No URL to copy."))
            }
        }
    }

    private fun reloadPage()
    {
        webViewState.value?.reload()
    }

    private fun goForward()
    {
        webViewState.value?.goForward()
    }
}