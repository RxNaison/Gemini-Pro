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

fun generateHtmlWithMermaid(mermaidCode: String): String {
    return """
<!doctype html>
<html lang="en">
  <head>
    <style>
      html, body {
        height: 100%;
        margin: 0;
        display: flex;
        align-items: center;
        justify-content: center;
      }
      pre.mermaid {
        margin: 0;
      }
    </style>
  </head>
  <body>
    <pre class="mermaid">$mermaidCode</pre>
    <script type="module">
      import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs';
      mermaid.initialize({ startOnLoad: true });
    </script>
  </body>
</html>
    """.trimIndent()
}