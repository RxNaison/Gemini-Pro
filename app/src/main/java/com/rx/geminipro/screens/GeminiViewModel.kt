package com.rx.geminipro.screens

import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class GeminiViewModel:ViewModel() {
    var isReady = MutableStateFlow(false)
        private set

    var keepScreenOn = mutableStateOf(false)
        private set

    var webViewState = mutableStateOf<WebView?>(null)

    var splitScreen = mutableStateOf(false)



    fun Ready()
    {
        isReady.value = true
    }

    fun KeepScreenOnSwitch()
    {
        keepScreenOn.value = !keepScreenOn.value
    }
}