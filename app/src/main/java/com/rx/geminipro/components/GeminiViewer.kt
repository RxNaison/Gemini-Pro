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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.rx.geminipro.utils.network.WebAppInterface
import java.lang.ref.WeakReference

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GeminiWebViewer(
    modifier: Modifier,
    filePathCallbackState: MutableState<ValueCallback<Array<Uri>>?>,
    filePickerLauncher: ActivityResultLauncher<Intent>?,
    onWebViewCreated: (WebView) -> Unit,
    onPageFinished: (WebView, String) -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current as ComponentActivity
    val initialUrl = "https://aistudio.google.com/u/0/prompts/new_chat"

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
            this.onPageFinished = onPageFinished
            this.onPermissionRequest = { request -> request.grant(request.resources) }
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
                    }

                    addJavascriptInterface(
                        WebAppInterface(
                            webViewRef = WeakReference(this),
                            getRetryUrl = { initialUrl }
                        ),
                        "Android"
                    )

                    setOnCreateContextMenuListener { menu, v, menuInfo ->
                        val result = (v as WebView).hitTestResult

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

                    loadUrl(initialUrl)
                }

                addView(webView)
                onWebViewCreated(webView)
            }
        }
    )
}