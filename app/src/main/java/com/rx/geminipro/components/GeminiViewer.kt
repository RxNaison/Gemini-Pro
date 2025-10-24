package com.rx.geminipro.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    var lastFailedExternalUrl by remember { mutableStateOf<String?>(null) }
    val initialUrl = "https://aistudio.google.com/u/0/prompts/new_chat"

    val webViewManager = remember(context, activity) {
        activity.let {
            WebViewManager(context, WeakReference(it)).apply {
                this.onShowToast = { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                this.onStartActivity = { intent ->
                    context.startActivity(intent)
                }
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
    }


    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                setBackgroundColor(Color.Transparent.toArgb())

                webViewManager.let { manager ->
                    webViewClient = manager.webViewClient
                    webChromeClient = manager.webChromeClient
                    setDownloadListener(manager.downloadListener)
                }
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
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
                        getRetryUrl = { lastFailedExternalUrl ?: initialUrl }
                    ),
                    "Android"
                )

                onWebViewCreated(this)
                loadUrl(initialUrl)
            }
        }
    )
}