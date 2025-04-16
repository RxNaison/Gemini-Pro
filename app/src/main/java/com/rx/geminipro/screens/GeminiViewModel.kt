package com.rx.geminipro.screens

import android.webkit.WebView
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class GeminiViewModel:ViewModel() {
    var isReady = MutableStateFlow(false)
        private set

    var keepScreenOn = mutableStateOf(false)
        private set

    var webViewState = mutableStateOf<WebView?>(null)


    fun Ready()
    {
        isReady.value = true
    }

    fun KeepScreenOnSwitch()
    {
        keepScreenOn.value = !keepScreenOn.value
    }
}