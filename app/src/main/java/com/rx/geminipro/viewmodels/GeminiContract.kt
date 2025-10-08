package com.rx.geminipro.viewmodels

import android.content.Intent

data class GeminiUiState(
    val isApplicationReady: Boolean = false,
    val isSplitScreen: Boolean = false,
    val isKeepScreenOn: Boolean = false,
    val isMenuLeft: Boolean = false,
    val isKeyboardVisible: Boolean = false,
    val clipboardContentType: ClipboardContentType = ClipboardContentType.NONE,
    val activeWebViewUrl: String? = null,
    val canWebViewGoBack: Boolean = false,
    val isReloading: Boolean = false
)

enum class ClipboardContentType { NONE, DIAGRAM, HTML }

sealed interface GeminiUiEvent {
    object ApplicationReady : GeminiUiEvent
    object OpenDocsClicked : GeminiUiEvent
    object KeepScreenOnToggled : GeminiUiEvent
    object SplitScreenToggled : GeminiUiEvent
    object SaveToFileClicked : GeminiUiEvent
    object OpenInBrowserClicked : GeminiUiEvent
    object SharePageClicked : GeminiUiEvent
    object CopyLinkClicked : GeminiUiEvent
    object ReloadPageClicked : GeminiUiEvent
    object GoForwardClicked : GeminiUiEvent
    data class MenuPositionChanged(val isLeft: Boolean) : GeminiUiEvent
    object BackButtonPressed : GeminiUiEvent
    data class KeyboardVisibilityChanged(val isVisible: Boolean) : GeminiUiEvent
}

sealed interface GeminiSideEffect {
    data class LaunchIntent(val intent: Intent) : GeminiSideEffect
    data class ShowToast(val message: String) : GeminiSideEffect
    object LaunchSaveToFile : GeminiSideEffect
}