package com.rx.geminipro.utils

import android.content.Context
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

enum class NavigationMode {
    GESTURAL, THREE_BUTTON, UNKNOWN
}

fun getSystemNavigationMode(view: View): NavigationMode {
    val rootWindowInsets = ViewCompat.getRootWindowInsets(view)
    if (rootWindowInsets != null) {
        val navInsets = rootWindowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
        val tappableInsets = rootWindowInsets.getInsets(WindowInsetsCompat.Type.tappableElement())

        return if (navInsets.bottom > 0 && navInsets.bottom == tappableInsets.bottom) {
            NavigationMode.THREE_BUTTON
        } else {
            NavigationMode.GESTURAL
        }
    }
    return NavigationMode.UNKNOWN
}
