package com.rx.geminipro.components

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.rx.geminipro.utils.network.BlobDownloaderInterface
import com.rx.geminipro.utils.network.WebAppInterface
import java.lang.ref.WeakReference
import androidx.core.net.toUri

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

    var lastUrl: String? = null

    val activity = LocalContext.current as? ComponentActivity

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
                        Toast.makeText(context, "Download is starting...", Toast.LENGTH_SHORT).show()
                    } else if (url != null && url.startsWith("data:")) {
                        try {
                            Toast.makeText(context, "Download is starting...", Toast.LENGTH_SHORT).show()
                            val commaIndex = url.indexOf(',')
                            if (commaIndex == -1) {
                                Toast.makeText(context, "Invalid data URL: missing comma", Toast.LENGTH_LONG).show()
                                return@setDownloadListener
                            }

                            val header = url.substring(0, commaIndex)
                            val base64EncodedData = url.substring(commaIndex + 1)

                            var extractedMimeType = "application/octet-stream"
                            val mimeAndEncodingPart = header.substringAfter("data:", missingDelimiterValue = "")
                            if (mimeAndEncodingPart.isNotBlank()) {
                                extractedMimeType = mimeAndEncodingPart.substringBefore(';', missingDelimiterValue = mimeAndEncodingPart).trim()
                            }

                            var fileExtension: String? = MimeTypeMap.getSingleton().getExtensionFromMimeType(extractedMimeType)

                            if (fileExtension == null) {
                                val slashIndex = extractedMimeType.lastIndexOf('/')
                                if (slashIndex != -1 && slashIndex < extractedMimeType.length - 1) {
                                    var subtype = extractedMimeType.substring(slashIndex + 1)
                                    val plusIndex = subtype.indexOf('+')
                                    if (plusIndex != -1) {
                                        subtype = subtype.substring(0, plusIndex)
                                    }
                                    if (subtype.isNotBlank() && subtype.length <= 5 && subtype.all { it.isLetterOrDigit() }) {
                                        fileExtension = subtype
                                    }
                                }
                            }

                            if (fileExtension.isNullOrBlank()) {
                                fileExtension = "bin"
                            }

                            val filenameHint = "download_${System.currentTimeMillis()}.$fileExtension"

                            BlobDownloaderInterface(context).processBlobData(base64EncodedData, extractedMimeType, filenameHint)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error processing data URL: ${e.message}", Toast.LENGTH_LONG).show()
                            Log.e("DataUrlDownload", "Error processing data URL", e)
                        }
                    } else if (url != null) {
                        try {
                            val request = DownloadManager.Request(url.toUri()).apply {
                                setMimeType(mimeType)
                                addRequestHeader("User-Agent", userAgent)
                                addRequestHeader("Cookie", android.webkit.CookieManager.getInstance().getCookie(url))
                                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
                                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
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
                    @SuppressLint("QueryPermissionsNeeded")
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
                                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                context.startActivity(intent)
                                true
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                                e.printStackTrace()
                                true
                            }
                        } else {
                            false
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

                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        if (failingUrl != null && failingUrl == view?.url && failingUrl != errorUrl) {
                            println("WebView Error (Deprecated): $errorCode - $description for $failingUrl")
                            lastFailedExternalUrl = failingUrl
                            view.loadUrl(errorUrl)
                        }
                    }
                }
                webChromeClient = object : WebChromeClient() {

                    private var customView: View? = null
                    private var customViewCallback: CustomViewCallback? = null
                    private var originalOrientation: Int = 0
                    private var originalSystemUiVisibility: Int = 0
                    private var fullscreenContainer: FrameLayout? = null
                    private var onBackPressedCallback: OnBackPressedCallback? = null

                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                        if (activity == null) {
                            callback?.onCustomViewHidden()
                            return
                        }

                        if (customView != null) {
                            callback?.onCustomViewHidden()
                            return
                        }

                        customView = view
                        customViewCallback = callback
                        originalSystemUiVisibility = activity.window.decorView.visibility
                        originalOrientation = activity.requestedOrientation

                        if (fullscreenContainer == null) {
                            fullscreenContainer = FrameLayout(activity).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setBackgroundColor(android.graphics.Color.BLACK)
                            }
                        }

                        val decorView = activity.window.decorView as ViewGroup
                        decorView.addView(fullscreenContainer, ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT))
                        fullscreenContainer?.addView(customView, ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT))

                        fullscreenContainer?.visibility = View.VISIBLE


                        if (onBackPressedCallback == null) {
                            onBackPressedCallback = object : OnBackPressedCallback(true) {
                                override fun handleOnBackPressed() {
                                    onHideCustomView()
                                }
                            }
                        }
                        onBackPressedCallback?.isEnabled = true
                        activity.onBackPressedDispatcher.addCallback(activity, onBackPressedCallback!!)
                    }

                    override fun onHideCustomView() {
                        if (activity == null || customView == null) {
                            return
                        }

                        customView?.visibility = View.GONE
                        fullscreenContainer?.removeView(customView)

                        val decorView = activity.window.decorView as ViewGroup
                        decorView.removeView(fullscreenContainer)
                        fullscreenContainer?.visibility = View.GONE

                        customView = null
                        customViewCallback?.onCustomViewHidden()
                        customViewCallback = null

                        activity.window.decorView.visibility = originalSystemUiVisibility
                        activity.requestedOrientation = originalOrientation

                        this@apply.visibility = View.VISIBLE

                        onBackPressedCallback?.isEnabled = false
                        onBackPressedCallback?.remove()
                    }

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
                            e.printStackTrace()
                            return false
                        }
                        return true
                    }

                    override fun onPermissionRequest(request: PermissionRequest) {
                        val resources = request.resources
                        request.grant(resources)
                    }

                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)

                        val currentUrl = view?.url
                        if (newProgress == 100 && currentUrl != null && currentUrl != lastUrl && currentUrl.startsWith("https://aistudio.google.com")) {
                            lastUrl = currentUrl
                            postTransition()
                        }
                    }
                }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.allowFileAccess = true
                settings.allowContentAccess = true

                settings.javaScriptCanOpenWindowsAutomatically = true

                settings.setSupportMultipleWindows(false)

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