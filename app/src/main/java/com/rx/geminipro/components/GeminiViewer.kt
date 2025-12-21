package com.rx.geminipro.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.rx.geminipro.utils.network.WebAppInterface
import java.lang.ref.WeakReference

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun GeminiWebViewer(
    modifier: Modifier,
    filePathCallbackState: MutableState<ValueCallback<Array<Uri>>?>,
    filePickerLauncher: ActivityResultLauncher<Intent>?,
    isVideoSelectionMode: Boolean,
    onWebViewCreated: (WebView) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onPageFinished: (WebView, String) -> Unit,
    onCameraTmpFileCreated: (Uri) -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current as ComponentActivity
    val initialUrl = "https://aistudio.google.com/u/0/prompts/new_chat"
    val spoofHeaders = mapOf("X-Requested-With" to "")

    val currentVideoMode by rememberUpdatedState(isVideoSelectionMode)
    val currentCameraCallback by rememberUpdatedState(onCameraTmpFileCreated)

    var lastTouchX = 0
    var lastTouchY = 0

    val webViewManager = remember(context, activity) {
        WebViewManager(context, WeakReference(activity)).apply {
            this.onShowToast = { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
            this.onStartActivity = { intent -> context.startActivity(intent) }
            this.onShowFileChooser = { callback, intent ->
                filePathCallbackState.value?.onReceiveValue(null)
                filePathCallbackState.value = callback
                try {
                    filePickerLauncher?.launch(intent)
                } catch (e: Exception) {
                    Log.e("GeminiWebViewer", "Cannot open file picker", e)
                    this.onShowToast("Cannot open file picker")
                    filePathCallbackState.value?.onReceiveValue(null)
                    filePathCallbackState.value = null
                }
            }
            this.onProgressChanged = onProgressChanged
            this.onPageFinished = onPageFinished
            this.onPermissionRequest = { request -> request.grant(request.resources) }
            this.onCameraTmpFileCreated = { uri ->
                currentCameraCallback(uri)
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            android.widget.FrameLayout(ctx).apply {
                fitsSystemWindows = true

                val webView = WebView(ctx).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        MATCH_PARENT,
                        MATCH_PARENT
                    )
                    setBackgroundColor(Color.Transparent.toArgb())

                    addJavascriptInterface(
                        com.rx.geminipro.utils.network.BlobDownloaderInterface(context),
                        "AndroidBlob"
                    )

                    webViewManager.let { manager ->
                        webViewClient = manager.webViewClient
                        webChromeClient = manager.webChromeClient
                        setDownloadListener(manager.getDownloadListener(this))
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        mediaPlaybackRequiresUserGesture = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        allowFileAccess = true
                        allowContentAccess = true
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(false)

                        userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36"
                    }

                    addJavascriptInterface(
                        WebAppInterface(
                            webViewRef = WeakReference(this),
                            getRetryUrl = { initialUrl }
                        ),
                        "Android"
                    )

                    setOnTouchListener { v, event ->
                        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                            lastTouchX = event.x.toInt()
                            lastTouchY = event.y.toInt()
                        }
                        false
                    }

                    setOnCreateContextMenuListener { menu, v, menuInfo ->
                        val result = (v as WebView).hitTestResult

                        if (currentVideoMode) {
                            menu.add("Download Video Here").setOnMenuItemClickListener {

                                val density = resources.displayMetrics.density
                                val cssX = lastTouchX / density
                                val cssY = lastTouchY / density

                                val js = """
                             (function() {
                                 var el = document.elementFromPoint($cssX, $cssY);
                                 // Walk up the tree in case we clicked a play button OVER the video
                                 while (el && el.tagName !== 'VIDEO') {
                                     el = el.parentElement;
                                 }
                                 if (el && el.tagName === 'VIDEO') {
                                     return el.currentSrc;
                                 }
                                 return null;
                             })();
                         """.trimIndent()

                                evaluateJavascript(js) { result ->
                                    val url = result?.replace("\"", "")

                                    if (!url.isNullOrEmpty() && url != "null") {
                                        webViewManager.getDownloadListener(this).onDownloadStart(
                                            url,
                                            settings.userAgentString,
                                            "video.mp4",
                                            "video/mp4",
                                            0
                                        )
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "No video found at this location",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                true
                            }
                        }
                        else{
                            if (result.type == WebView.HitTestResult.IMAGE_TYPE ||
                                result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {

                                val imageUrl = result.extra

                                if (imageUrl != null) {
                                    menu.add("Save Image").setOnMenuItemClickListener {
                                        webViewManager.getDownloadListener(this).onDownloadStart(
                                            imageUrl,
                                            settings.userAgentString,
                                            "attachment",
                                            "image/png",
                                            0
                                        )
                                        true
                                    }
                                }
                            }
                        }
                    }

                    loadUrl(initialUrl, spoofHeaders)
                }

                addView(webView)
                onWebViewCreated(webView)
            }
        }
    )
}