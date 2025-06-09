package com.rx.geminipro.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlViewer(htmlContent: String) {
//    val isDark = isSystemInDarkTheme()
    val background = Color.White
    AndroidView(factory = { context ->
        WebView(context).apply {
            this.setBackgroundColor(background.toArgb())
//            if(isDark)
//            {
//                if(WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
//                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true);
//                }
//                WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)
//            }

            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }
    }, update = { webView ->
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    })
}