package com.rx.geminipro.components

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

@Composable
fun geminiHtmlViewer(
    filePathCallbackState: MutableState<ValueCallback<Array<Uri>>?>,
    filePickerLauncher: ActivityResultLauncher<Intent>?,
    isKeyBoardShown: Boolean,
    modifier: Modifier,
    postTransition: ()-> Unit = {}
): MutableState<WebView?>
{
    val background = MaterialTheme.colorScheme.background
    val webViewState = remember { mutableStateOf<WebView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                this.setBackgroundColor(background.toArgb())
//                if(WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
//                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true);
//                }
//                WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)

//                setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
//                    val request = DownloadManager.Request(Uri.parse(url)).apply {
//                        setMimeType(mimeType)
//                        addRequestHeader("User-Agent", userAgent)
//                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
//                        setDestinationInExternalPublicDir(
//                            Environment.DIRECTORY_DOWNLOADS,
//                            URLUtil.guessFileName(url, contentDisposition, mimeType))
//                    }
//                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
//                    dm.enqueue(request)
//                    Toast.makeText(context, "Downloading File", Toast.LENGTH_LONG).show()
//                }

                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        url: String
                    ): Boolean {
                        return if (url.startsWith("intent://")) {
                            try {
                                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                context.startActivity(intent)
                                true
                            } catch (e: Exception) {
                                e.printStackTrace()
                                false
                            }
                        } else {
                            view.loadUrl(url)
                            true
                        }
                    }
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        postTransition()
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView,
                        filePathCallback: ValueCallback<Array<Uri>>,
                        fileChooserParams: FileChooserParams
                    ): Boolean {
                        filePathCallbackState.value?.onReceiveValue(null)
                        filePathCallbackState.value = filePathCallback

                        val intent = fileChooserParams.createIntent()
                        try {
                            filePickerLauncher?.launch(intent)
                        } catch (e: Exception) {
                            filePathCallbackState.value?.onReceiveValue(null)
                            filePathCallbackState.value = null
                            return false
                        }
                        return true
                    }

                    override fun onPermissionRequest(request: PermissionRequest) {
                        val resources = request.resources
                        val permissionsToRequest = mutableListOf<String>()

                        for (resource in resources) {
                            when (resource) {
                                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                                    permissionsToRequest.add(android.Manifest.permission.RECORD_AUDIO)
                                }

                                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                                    permissionsToRequest.add(android.Manifest.permission.CAMERA)
                                }
                            }
                        }

                        if (permissionsToRequest.isNotEmpty()) {
                            request.grant(resources)
                        }
                    }
                }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.setSupportZoom(true)
                settings.mediaPlaybackRequiresUserGesture = false
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.allowFileAccess = true
                settings.allowContentAccess = true

                loadUrl("https://aistudio.google.com")

                webViewState.value = this
            }
        }
    )
    webViewState.value?.let { webView ->
        if(!isKeyBoardShown)
            BackHandler(webView.canGoBack()) {
                webView.goBack()
            }
    }

    return  webViewState
}