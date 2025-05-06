package com.rx.geminipro.components

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.rx.geminipro.utils.network.BlobDownloaderInterface
import com.rx.geminipro.utils.network.WebAppInterface
import java.lang.ref.WeakReference

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun geminiHtmlViewer(
    filePathCallbackState: MutableState<ValueCallback<Array<Uri>>?>,
    filePickerLauncher: ActivityResultLauncher<Intent>?,
    isKeyBoardShown: Boolean,
    modifier: Modifier,
    postTransition: () -> Unit = {}
): MutableState<WebView?> {
    val background = MaterialTheme.colorScheme.background
    val webViewState = remember { mutableStateOf<WebView?>(null) }

    var lastFailedExternalUrl by remember { mutableStateOf<String?>(null) }
    val initialUrl = "https://aistudio.google.com"
    val errorUrl = "file:///android_asset/webview_error.html"

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                this.setBackgroundColor(background.toArgb())

                addJavascriptInterface(BlobDownloaderInterface(context), "AndroidBlobHandler")

                setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                    if (url != null && url.startsWith("blob:")) {
                        val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
                        val jsToRun = BlobDownloaderInterface.getBase64StringFromBlobUrl(url, mimeType, filename)
                        this.loadUrl(jsToRun)
                        Toast.makeText(context, "Preparing download...", Toast.LENGTH_SHORT).show()
                    } else if (url != null) {
                        try {
                            val request = DownloadManager.Request(Uri.parse(url)).apply {
                                setMimeType(mimeType)
                                addRequestHeader("User-Agent", userAgent)
                                addRequestHeader("Cookie", android.webkit.CookieManager.getInstance().getCookie(url))
                                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
                                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                                allowScanningByMediaScanner()
                            }
                            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            dm.enqueue(request)
                            Toast.makeText(context, "Starting download: ${URLUtil.guessFileName(url, contentDisposition, mimeType)}", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Invalid download URL", Toast.LENGTH_SHORT).show()
                    }
                }


                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = object : WebViewClient() {
                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        url: String
                    ): Boolean {
                        return if (url.startsWith("intent://")) {
                            try {
                                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(intent)
                                } else if (fallbackUrl != null) {
                                    view.loadUrl(fallbackUrl)
                                } else {
                                    Toast.makeText(context, "Cannot handle intent", Toast.LENGTH_SHORT).show()
                                }
                                true
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Error handling intent URL", Toast.LENGTH_SHORT).show()
                                false
                            }
                        } else if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:") || url.startsWith("market:")) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                                true
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                                true
                            }
                        } else {
                            view.loadUrl(url)
                            true
                        }
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        if (url != errorUrl) {
                            lastFailedExternalUrl = null
                        }
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        postTransition()

                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true && request.url.toString() != errorUrl) {
                            val failingUrl = request.url.toString()
                            println("WebView Error: ${error?.errorCode} - ${error?.description} for $failingUrl")
                            lastFailedExternalUrl = failingUrl
                            view?.loadUrl(errorUrl)
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        super.onReceivedError(view, errorCode, description, failingUrl)
                        if (failingUrl != null && failingUrl == view?.url && failingUrl != errorUrl) {
                            println("WebView Error (Deprecated): $errorCode - $description for $failingUrl")
                            lastFailedExternalUrl = failingUrl
                            view.loadUrl(errorUrl)
                        }
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
                            Toast.makeText(context, "Cannot open file picker", Toast.LENGTH_SHORT).show()
                            filePathCallbackState.value?.onReceiveValue(null)
                            filePathCallbackState.value = null
                            return false
                        }
                        return true
                    }

                    override fun onPermissionRequest(request: PermissionRequest) {
                        val resources = request.resources
                        request.grant(resources)
                    }
                }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.allowUniversalAccessFromFileURLs = true
                settings.allowFileAccessFromFileURLs = true

                settings.javaScriptCanOpenWindowsAutomatically = true

                addJavascriptInterface(
                    WebAppInterface(
                        webViewRef = WeakReference(this),
                        getRetryUrl = { lastFailedExternalUrl ?: initialUrl }
                    ),
                    "Android"
                )

                loadUrl("https://aistudio.google.com")

                webViewState.value = this
            }
        },
    )

    webViewState.value?.let { webView ->
        if (!isKeyBoardShown)
            BackHandler(webView.canGoBack()) {
                webView.goBack()
            }
    }

    return webViewState
}