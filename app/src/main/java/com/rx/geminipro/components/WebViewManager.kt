package com.rx.geminipro.components

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.rx.geminipro.utils.network.BlobDownloaderInterface
import java.io.File
import java.lang.ref.WeakReference


private const val TAG = "WebViewManager"


class WebViewManager(
    private val context: Context,
    private val activity: WeakReference<ComponentActivity>,
) {
    var onShowFileChooser: (ValueCallback<Array<Uri>>, Intent) -> Unit = { _, _ -> }

    var onShowToast: (message: String) -> Unit = {}

    var onStartActivity: (intent: Intent) -> Unit = {}

    var onPageFinished: (view: WebView, url: String) -> Unit = { _, _ -> }

    var onPermissionRequest: (request: PermissionRequest) -> Unit = { it.deny() }
    var onCameraTmpFileCreated: (Uri) -> Unit = {}
    var onProgressChanged: (Int) -> Unit = {}

    // --- Private State ---

    private var lastUrl: String? = null
    private var lastFailedUrl: String? = null
    private val errorUrl = "file:///android_asset/webview_error.html"
    private val spoofHeaders = mapOf("X-Requested-With" to "")

    // --- Core WebView Clients and Listeners ---

    val webViewClient: WebViewClient = object : WebViewClient() {
        @SuppressLint("QueryPermissionsNeeded")
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            return when {
                url.startsWith("http://") || url.startsWith("https://") -> {
                    view.loadUrl(url, spoofHeaders)
                    true
                }
                url.startsWith("intent://") -> handleIntentUrl(url, view)
                url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:") || url.startsWith("market:") -> handleExternalAppUrl(url)
                else -> false
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)

            val preventRevokeScript = """
            window.URL.revokeObjectURL = function(url) {
                console.log("GeminiPro: Blocked revocation of " + url);
                // Do nothing! Keep the blob alive.
            };
        """.trimIndent()

            view.evaluateJavascript(preventRevokeScript, null)

            onPageFinished.invoke(view, url)
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true) {
                val failingUrl = request.url.toString()
                Log.e(TAG, "WebView Error: ${error?.errorCode} - ${error?.description} for $failingUrl")
                lastFailedUrl = failingUrl
                view?.loadUrl(errorUrl)
            }
        }
    }

    val webChromeClient: WebChromeClient = object : WebChromeClient() {
        private var customView: View? = null
        private var customViewCallback: CustomViewCallback? = null
        private var originalOrientation: Int = 0
        private var fullscreenContainer: FrameLayout? = null
        private var onBackPressedCallback: OnBackPressedCallback? = null

        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
            Log.d("WebViewConsole", "[${consoleMessage?.messageLevel()}] ${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
            return true
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams,
        ): Boolean {
            var photoUri: Uri? = null
            var captureIntent: Intent? = null

            try {
                val photoFile = File.createTempFile("gemini_cam_", ".jpg", context.cacheDir)

                photoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    photoFile
                )

                photoUri?.let { onCameraTmpFileCreated(it) }

                captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

            } catch (e: Exception) {
                Log.e(TAG, "Unable to create camera temp file", e)
            }

            val galleryIntent = fileChooserParams.createIntent()

            val chooserIntent = Intent(Intent.ACTION_CHOOSER)
            chooserIntent.putExtra(Intent.EXTRA_INTENT, galleryIntent)
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Upload Image")

            captureIntent?.let {
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(it))
            }

            onShowFileChooser(filePathCallback, chooserIntent)
            return true
        }

        override fun onPermissionRequest(request: PermissionRequest) {
            onPermissionRequest.invoke(request)
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            val currentActivity = activity.get() ?: run {
                callback?.onCustomViewHidden()
                return
            }

            if (customView != null) {
                callback?.onCustomViewHidden()
                return
            }

            customView = view
            customViewCallback = callback
            originalOrientation = currentActivity.requestedOrientation

            val decorView = currentActivity.window.decorView as ViewGroup
            fullscreenContainer = FrameLayout(currentActivity).apply {
                setBackgroundColor(Color.BLACK)
                addView(customView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }

            decorView.addView(fullscreenContainer, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setFullscreen(enabled = true, activity = currentActivity)

            onBackPressedCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { onHideCustomView() }
            }
            currentActivity.onBackPressedDispatcher.addCallback(currentActivity, onBackPressedCallback!!)
        }

        override fun onHideCustomView() {
            val currentActivity = activity.get() ?: return
            if (customView == null) return

            setFullscreen(enabled = false, activity = currentActivity)
            (fullscreenContainer?.parent as? ViewGroup)?.removeView(fullscreenContainer)

            fullscreenContainer = null
            customView = null
            customViewCallback?.onCustomViewHidden()

            onBackPressedCallback?.remove()
            onBackPressedCallback = null
        }

        private fun setFullscreen(enabled: Boolean, activity: Activity) {
            val window = activity.window
            val container = fullscreenContainer ?: return
            WindowInsetsControllerCompat(window, container).let { controller ->
                if (enabled) {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            val currentUrl = view?.url

            onProgressChanged(newProgress)

            if ((newProgress == 100) && (currentUrl != null) && (currentUrl != lastUrl)) {
                lastUrl = currentUrl
                onPageFinished.invoke(view, currentUrl)
            }
        }
    }

    fun getDownloadListener(webView: WebView): DownloadListener {
        return DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            Log.d(TAG, "Download listener triggered for: $url")
            when {
                url.startsWith("blob:") -> {
                    val quotedUrl = org.json.JSONObject.quote(url)
                    val quotedMime = org.json.JSONObject.quote(mimeType ?: "")
                    val jsScript = """
                        (async function() {
                            function log(msg) {
                                if (window.AndroidBlob && window.AndroidBlob.jsLog) {
                                    window.AndroidBlob.jsLog(msg);
                                }
                                console.log("GeminiPro: " + msg);
                            }

                            const targetUrl = $quotedUrl;
                            const fallbackMime = $quotedMime;
                            log("Starting extraction for " + targetUrl);
                            try {
                                const response = await fetch(targetUrl);
                                const blob = await response.blob();
                                log("Blob fetched, size: " + blob.size + ", type: " + blob.type);
                                
                                const reader = new FileReader();
                                reader.onloadend = function() {
                                    if (reader.result && window.AndroidBlob) {
                                        log("Data converted to Base64, length: " + reader.result.length);
                                        AndroidBlob.processBlobData(reader.result, blob.type || fallbackMime);
                                    }
                                };
                                reader.readAsDataURL(blob);
                            } catch (err) {
                                log("Fetch error: " + err.message);
                                log("Trying XHR fallback...");
                                try {
                                    var xhr = new XMLHttpRequest();
                                    xhr.open('GET', targetUrl, true);
                                    xhr.responseType = 'blob';
                                    xhr.onload = function() {
                                        if (this.status === 200) {
                                            const blob = this.response;
                                            const reader = new FileReader();
                                            reader.onloadend = function() {
                                                if (reader.result && window.AndroidBlob) {
                                                    AndroidBlob.processBlobData(reader.result, blob.type || fallbackMime);
                                                }
                                            };
                                            reader.readAsDataURL(blob);
                                        } else {
                                            log("XHR failed with status: " + this.status);
                                        }
                                    };
                                    xhr.onerror = function() { log("XHR error"); };
                                    xhr.send();
                                } catch (e2) {
                                    log("Fallback failed: " + e2.message);
                                }
                            }
                        })();
                    """.trimIndent()

                    webView.evaluateJavascript(jsScript, null)
                }

                url.startsWith("data:") -> {
                    val cleanUrl = url.substringAfter("base64,")
                    val extractedMime = url.substringAfter("data:").substringBefore(";")
                    BlobDownloaderInterface(context).processBlobData(cleanUrl, extractedMime)
                }

                else -> handleStandardDownload(url, userAgent, contentDisposition, mimeType)
            }
        }
    }

    // --- Private Helper Functions ---

    private fun handleIntentUrl(url: String, webView: WebView): Boolean {
        try {
            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)

            // Sanitize intent to prevent launching internal unexported components or selectors
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.component = null
            intent.selector = null

            val fallbackUrl = intent.getStringExtra("browser_fallback_url")

            if (intent.resolveActivity(context.packageManager) != null) {
                onStartActivity(intent)
            } else if (fallbackUrl != null && (fallbackUrl.startsWith("http://") || fallbackUrl.startsWith("https://"))) {
                webView.loadUrl(fallbackUrl, spoofHeaders)
            } else {
                onShowToast("Cannot handle this type of link.")
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing intent URL: $url", e)
            onShowToast("Error handling link.")
            return false
        }
    }

    private fun handleExternalAppUrl(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            onStartActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start activity for URL: $url", e)
            onShowToast("Cannot open link.")
            true
        }
    }

    private fun handleStandardDownload(url: String, userAgent: String, contentDisposition: String, mimeType: String) {
        try {
            val request = DownloadManager.Request(url.toUri()).apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent)
                addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                setTitle(filename)
                setDescription("Downloading file...")
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            onShowToast("Starting download: ${URLUtil.guessFileName(url, contentDisposition, mimeType)}")
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for URL: $url", e)
            onShowToast("Download failed: ${e.message}")
        }
    }
}