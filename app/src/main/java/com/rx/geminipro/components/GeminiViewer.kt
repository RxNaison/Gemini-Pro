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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    isAudioSelectionMode: Boolean,
    onWebViewCreated: (WebView) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onPageFinished: (WebView, String) -> Unit,
    onCameraTmpFileCreated: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current as? ComponentActivity
    val initialUrl = "https://aistudio.google.com/u/0/prompts/new_chat"
    val spoofHeaders = mapOf("X-Requested-With" to "")

    val currentVideoMode by rememberUpdatedState(isVideoSelectionMode)
    val currentAudioMode by rememberUpdatedState(isAudioSelectionMode)
    val currentCameraCallback by rememberUpdatedState(onCameraTmpFileCreated)
    val currentProgressCallback by rememberUpdatedState(onProgressChanged)
    val currentPageFinishedCallback by rememberUpdatedState(onPageFinished)

    val lastTouchX = remember { mutableFloatStateOf(0f) }
    val lastTouchY = remember { mutableFloatStateOf(0f) }

    val noVideoFoundText = stringResource(com.rx.geminipro.R.string.no_video_found)
    val currentNoVideoFoundText by rememberUpdatedState(noVideoFoundText)

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
            this.onProgressChanged = { currentProgressCallback(it) }
            this.onPageFinished = { view, url -> currentPageFinishedCallback(view, url) }
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
                        MATCH_PARENT,
                    )
                    setBackgroundColor(Color.Transparent.toArgb())

                    addJavascriptInterface(
                        com.rx.geminipro.utils.network.BlobDownloaderInterface(context),
                        "AndroidBlob",
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
                        allowFileAccess = false
                        allowContentAccess = true
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(false)

                        val defaultUserAgent = userAgentString ?: ""

                        val chromeVersionToken = try {
                            val pattern = "Chrome/[.0-9]+".toRegex()
                            pattern.find(defaultUserAgent)?.value ?: "Chrome/144.0.0.0"
                        } catch (_: Exception) {
                            "Chrome/144.0.0.0"
                        }
                        userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) $chromeVersionToken Mobile Safari/537.36"
                    }

                    addJavascriptInterface(
                        WebAppInterface(WeakReference(this as WebView?)) { initialUrl },
                        "Android"
                    )

                    setOnTouchListener { _, event ->
                        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                            lastTouchX.floatValue = event.x
                            lastTouchY.floatValue = event.y
                        }
                        false
                    }

                    setOnCreateContextMenuListener { menu, v, _ ->
                        val hitTestWebView = v as WebView
                        val result = hitTestWebView.hitTestResult

                        if (currentVideoMode || currentAudioMode) {
                            val targetTag = if (currentAudioMode) "AUDIO" else "VIDEO"
                            val menuTitle = if (currentAudioMode) "Download Audio Here" else "Download Video Here"
                            val defaultExt = if (currentAudioMode) "mp3" else "mp4"
                            val defaultMime = if (currentAudioMode) "audio/mpeg" else "video/mp4"

                            menu.add(menuTitle).setOnMenuItemClickListener {

                                val density = resources.displayMetrics.density
                                val cssX = lastTouchX.floatValue / density
                                val cssY = lastTouchY.floatValue / density

                                val js = """
                             (function() {
                                 function extractSrc(mediaEl) {
                                     if (!mediaEl) return null;
                                     if (mediaEl.currentSrc) return mediaEl.currentSrc;
                                     if (mediaEl.src) return mediaEl.src;
                                     var source = mediaEl.querySelector('source');
                                     return source ? (source.src || source.getAttribute('src')) : null;
                                 }

                                 var el = document.elementFromPoint($cssX, $cssY);
                                 if (!el) return null;

                                 // 1. Walk up the parent tree looking for the target tag
                                 var curr = el;
                                 while (curr) {
                                     if (curr.tagName === '$targetTag') {
                                         var src = extractSrc(curr);
                                         if (src) return src;
                                     }
                                     if ('$targetTag' === 'AUDIO' && curr.tagName === 'VIDEO') {
                                         var src = extractSrc(curr);
                                         if (src) return src;
                                     }
                                     curr = curr.parentElement;
                                 }

                                 // 2. If clicked on a player container/wrapper, search descendants
                                 curr = el;
                                 var depth = 0;
                                 while (curr && depth < 3) {
                                     var child = curr.querySelector('$targetTag');
                                     if (child) {
                                         var src = extractSrc(child);
                                         if (src) return src;
                                     }
                                     if ('$targetTag' === 'AUDIO') {
                                         var vChild = curr.querySelector('video');
                                         if (vChild) {
                                             var src = extractSrc(vChild);
                                             if (src) return src;
                                         }
                                     }
                                     curr = curr.parentElement;
                                     depth++;
                                 }

                                 return null;
                             })();
                         """.trimIndent()

                                evaluateJavascript(js) { resultJs ->
                                    val url = resultJs?.trim()?.removeSurrounding("\"")

                                    if (!url.isNullOrEmpty() && url != "null") {
                                        webViewManager.getDownloadListener(this).onDownloadStart(
                                            url,
                                            settings.userAgentString,
                                            "attachment.$defaultExt",
                                            defaultMime,
                                            0L
                                        )
                                    } else {
                                        Toast.makeText(
                                            context,
                                            currentNoVideoFoundText,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                                true
                            }
                        }
                        else{
                            if ((result.type == WebView.HitTestResult.IMAGE_TYPE) ||
                                (result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE)) {

                                val imageUrl = result.extra

                                if (imageUrl != null) {
                                    menu.add("Save Image").setOnMenuItemClickListener {
                                        webViewManager.getDownloadListener(this).onDownloadStart(
                                            imageUrl,
                                            settings.userAgentString,
                                            "attachment",
                                            "image/png",
                                            0L
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