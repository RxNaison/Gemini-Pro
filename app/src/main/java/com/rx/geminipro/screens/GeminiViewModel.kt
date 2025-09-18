package com.rx.geminipro.screens

import android.app.Application
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rx.geminipro.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GeminiViewModel(application: Application) : AndroidViewModel(application) {
    var isReady = MutableStateFlow(false)
        private set

    var keepScreenOn = mutableStateOf(false)
        private set

    var webViewState = mutableStateOf<WebView?>(null)

    var splitScreen = mutableStateOf(false)

    private val userPreferencesRepository = UserPreferencesRepository(application)

    val isMenuLeft = userPreferencesRepository.isMenuLeftFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun ready()
    {
        isReady.value = true
    }

    fun keepScreenOnSwitch()
    {
        keepScreenOn.value = !keepScreenOn.value
    }

    fun setMenuPosition(isLeft: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveMenuPosition(isLeft)
        }
    }
}