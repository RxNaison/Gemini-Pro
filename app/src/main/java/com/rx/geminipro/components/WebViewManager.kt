package com.rx.geminipro.components

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.MimeTypeMap
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
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.rx.geminipro.R
import com.rx.geminipro.utils.network.BlobDownloaderInterface
import java.io.File
import java.lang.ref.WeakReference


private const val TAG = "WebViewManager"


class WebViewManager(
    private val context: Context,
    private val activity: WeakReference<ComponentActivity>
) {
    var onShowFileChooser: (ValueCallback<Array<Uri>>, Intent) -> Unit = { _, _ -> }

    var onShowToast: (message: String) -> Unit = {}

    var onStartActivity: (intent: Intent) -> Unit = {}

    var onPageFinished: (view: WebView, url: String) -> Unit = { _, _ -> }

    var onPermissionRequest: (request: PermissionRequest) -> Unit = { it.deny() }

    // --- Private State ---

    private var lastUrl: String? = null
    private var lastFailedUrl: String? = null
    private val errorUrl = "file:///android_asset/webview_error.html"

    // --- Core WebView Clients and Listeners ---

    val webViewClient: WebViewClient = object : WebViewClient() {
        @SuppressLint("QueryPermissionsNeeded")
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            return when {
                url.startsWith("intent://") -> handleIntentUrl(url, view)
                url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:") || url.startsWith("market:") -> handleExternalAppUrl(url)
                else -> false
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
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

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            val intent = fileChooserParams.createIntent()
            onShowFileChooser(filePathCallback, intent)
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

            setFullscreen(true, currentActivity)

            onBackPressedCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { onHideCustomView() }
            }
            currentActivity.onBackPressedDispatcher.addCallback(currentActivity, onBackPressedCallback!!)
        }

        override fun onHideCustomView() {
            val currentActivity = activity.get() ?: return
            if (customView == null) return

            setFullscreen(false, currentActivity)

            (fullscreenContainer?.parent as? ViewGroup)?.removeView(fullscreenContainer)

            fullscreenContainer = null
            customView = null
            customViewCallback?.onCustomViewHidden()

            currentActivity.requestedOrientation = originalOrientation

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
            if (newProgress == 100 && currentUrl != null && currentUrl != lastUrl) {
                lastUrl = currentUrl
                onPageFinished.invoke(view, currentUrl)
            }
        }
    }
    var onBlobDownloadRequested: (url: String, contentDisposition: String?, mimeType: String?) -> Unit = { _, _, _ -> }
    val downloadListener: DownloadListener = DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
        when {
            url.startsWith("blob:") -> onBlobDownloadRequested(url, contentDisposition, mimeType)
            url.startsWith("data:") -> handleDataUrlDownload(url)
            else -> handleStandardDownload(url, userAgent, contentDisposition, mimeType)
        }
    }

    // --- Private Helper Functions ---

    private fun handleIntentUrl(url: String, webView: WebView): Boolean {
        try {
            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            val fallbackUrl = intent.getStringExtra("browser_fallback_url")

            if (intent.resolveActivity(context.packageManager) != null) {
                onStartActivity(intent)
            } else if (fallbackUrl != null) {
                webView.loadUrl(fallbackUrl)
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

    private fun handleDataUrlDownload(url: String) {
        try {
            val downloader = BlobDownloaderInterface()
            val header = url.substringBefore(',')
            val mimeType = header.substringAfter("data:").substringBefore(';')
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: mimeType.substringAfter("/")
            val filename = "download_${System.currentTimeMillis()}.$extension"

            val file = downloader.processBlobData(url, filename)
            file?.let {
                showDownloadCompleteNotification(file, mimeType)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing data URL download", e)
            onShowToast("Error processing download: ${e.message}")
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

    private fun showDownloadCompleteNotification(file: File, mimeType: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "blob_downloads"

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
        {
            val channel =
                NotificationChannel(channelId, "Downloads", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val fileUri: Uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "FileProvider error: ${e.message}")
            onShowToast("Error creating file URI for notification")
            return
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) == null) {
            Log.w(TAG, "No activity found to handle VIEW intent for type $mimeType")
        }

        val pendingIntentFlags =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.google_gemini_icon)
            .setContentTitle("Download complete")
            .setContentText(file.name)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }
}