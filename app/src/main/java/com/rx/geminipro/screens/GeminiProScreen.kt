package com.rx.geminipro.screens

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rx.geminipro.R
import com.rx.geminipro.components.*
import com.rx.geminipro.utils.permissions.GetPermissions
import com.rx.geminipro.viewmodels.*
import kotlinx.coroutines.delay


@Composable
fun GeminiProScreen(
    modifier: Modifier = Modifier,
    viewModel: GeminiViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalContext.current.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    var webView by remember { mutableStateOf<WebView?>(null) }

    var showAdditionalMenu by remember { mutableStateOf(false) }
    var showDiagram by remember { mutableStateOf(false) }
    var showHtmlPreview by remember { mutableStateOf(false) }
    var clipboardText by remember { mutableStateOf("") }
    var isHighlightingMenu by remember { mutableStateOf(false) }


    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(isKeyboardVisible) {
        viewModel.onEvent(GeminiUiEvent.KeyboardVisibilityChanged(isKeyboardVisible))
    }

    // --- File Picker Setup ---
    val filePathCallbackState = remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (filePathCallbackState.value == null) {
            return@rememberLauncherForActivityResult
        }

        val uris: Array<Uri> = if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data?.clipData != null) {
                val clipData = data.clipData!!
                List(clipData.itemCount) { i ->
                    clipData.getItemAt(i).uri
                }.toTypedArray()
            } else if (data?.data != null) {
                arrayOf(data.data!!)
            } else {
                emptyArray()
            }
        } else {
            emptyArray()
        }
        filePathCallbackState.value?.onReceiveValue(uris)
        filePathCallbackState.value = null
    }

    // --- Document Launcher for "Save to File" ---
    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                stream.write(clipboardText.toByteArray())
            }
        }
    }

    // --- Side Effect Handler ---
    LaunchedEffect(Unit) {
        viewModel.sideEffectFlow.collect { effect ->
            val currentWebView = webView ?: return@collect
            when (effect) {
                is GeminiSideEffect.LaunchIntent -> context.startActivity(effect.intent)
                is GeminiSideEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                is GeminiSideEffect.LaunchSaveToFile -> documentLauncher.launch("gemini-note.txt")
                is GeminiSideEffect.WebViewGoBack -> currentWebView.goBack()
                is GeminiSideEffect.WebViewReload -> currentWebView.reload()
                is GeminiSideEffect.WebViewGoForward -> currentWebView.goForward()
            }
        }
    }

    // --- Clipboard Monitor ---
    LaunchedEffect(clipboardManager) {
        clipboardManager.addPrimaryClipChangedListener {
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text.toString()
                clipboardText = text
                viewModel.onClipboardTextChanged(text)
            }
        }
    }

    // --- Back Handler Logic ---
    BackHandler(enabled = uiState.canWebViewGoBack) {
        viewModel.onEvent(GeminiUiEvent.BackButtonPressed)
    }


    // --- Menu Highlight Animation Trigger ---
    LaunchedEffect(uiState.isMenuLeft) {
        isHighlightingMenu = true
        delay(2000)
        isHighlightingMenu = false
    }

    // --- UI Layer ---
    GetPermissions(context)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        WebViewLayout(
            uiState = uiState,
            filePickerLauncher = filePickerLauncher,
            filePathCallbackState = filePathCallbackState,
            onWebViewCreated = { createdWebView ->
                webView = createdWebView
                viewModel.onEvent(GeminiUiEvent.ApplicationReady)
            },
            onPageFinished = { finishedWebView ->
                viewModel.onEvent(
                    GeminiUiEvent.WebViewNavigated(
                        canGoBack = finishedWebView.canGoBack(),
                        url = finishedWebView.url
                    )
                )
            }

        )

        ReloadIndicator(isLoading = uiState.isReloading)

        PreviewButtons(
            clipboardContentType = uiState.clipboardContentType,
            onShowDiagram = { showDiagram = true },
            onShowHtml = { showHtmlPreview = true }
        )

        if (!uiState.isKeyboardVisible) {
            MenuHandles(
                isMenuLeft = uiState.isMenuLeft,
                isHighlighting = isHighlightingMenu,
                onReload = { viewModel.onEvent(GeminiUiEvent.ReloadPageClicked) },
                onOpenMenu = { showAdditionalMenu = true }
            )
        }

        if (showAdditionalMenu) {
            AdditionalMenu(
                onClose = { showAdditionalMenu = false },
                items = rememberMenuItems(viewModel::onEvent, uiState.isMenuLeft)
            )
        }

        DiagramScreen(
            isDiagramVisible = showDiagram,
            clipboardText = clipboardText,
            onBack = { showDiagram = false },
            onClose = { showDiagram = false }
        )

        HtmlPreviewScreen(
            isHtmlPreviewVisible = showHtmlPreview,
            clipboardText = clipboardText,
            onBack = { showHtmlPreview = false },
            onClose = { showHtmlPreview = false }
        )
    }
}

@Composable
private fun WebViewLayout(
    uiState: GeminiUiState,
    filePickerLauncher: ActivityResultLauncher<Intent>,
    filePathCallbackState: MutableState<ValueCallback<Array<Uri>>?>,
    onWebViewCreated: (webView: WebView) -> Unit,
    onPageFinished: (webView: WebView) -> Unit
) {
    val configuration = LocalConfiguration.current
    val windowInfo = LocalWindowInfo.current
    val windowWidth = with(LocalDensity.current) { windowInfo.containerSize.width.toDp() }
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isLargeScreen = windowWidth > 600.dp
    val useHorizontalLayout = uiState.isSplitScreen && (isLandscape || isLargeScreen)

    val layoutModifier = Modifier.fillMaxSize()
    
    val splitModifier = Modifier
    val mainModifier = if(uiState.isSplitScreen) Modifier else splitModifier

    if (useHorizontalLayout) {
        Row(modifier = layoutModifier) {
            WebViewWrapper(
                modifier = splitModifier.weight(1f),
                filePickerLauncher = filePickerLauncher,
                filePathCallbackState = filePathCallbackState,
                onWebViewCreated = onWebViewCreated,
                onPageFinished = onPageFinished
            )
            WebViewWrapper(
                modifier = splitModifier.weight(1f),
                filePickerLauncher = filePickerLauncher,
                filePathCallbackState = filePathCallbackState,
                onWebViewCreated = onWebViewCreated,
                onPageFinished = onPageFinished
            )
        }
    } else {
        Column(modifier = layoutModifier) {
            if (uiState.isSplitScreen) {
                WebViewWrapper(
                    modifier = splitModifier.weight(1f),
                    filePickerLauncher = filePickerLauncher,
                    filePathCallbackState = filePathCallbackState,
                    onWebViewCreated = onWebViewCreated,
                    onPageFinished = onPageFinished
                )
            }
            WebViewWrapper(
                modifier = mainModifier.weight(1f),
                filePickerLauncher = filePickerLauncher,
                filePathCallbackState = filePathCallbackState,
                onWebViewCreated = onWebViewCreated,
                onPageFinished = onPageFinished
            )
        }
    }
}

@Composable
private fun WebViewWrapper(
    modifier: Modifier = Modifier,
    filePickerLauncher: ActivityResultLauncher<Intent>,
    filePathCallbackState: MutableState<ValueCallback<Array<Uri>>?>,
    onWebViewCreated: (webView: WebView) -> Unit,
    onPageFinished: (webView: WebView) -> Unit
) {
    GeminiWebViewer(
        modifier = modifier,
        filePathCallbackState = filePathCallbackState,
        filePickerLauncher = filePickerLauncher,
        onWebViewCreated = onWebViewCreated,
        onPageFinished = { webView, _ ->
            onPageFinished(webView)
        }
    )

}

@Composable
private fun BoxScope.PreviewButtons(
    clipboardContentType: ClipboardContentType,
    onShowDiagram: () -> Unit,
    onShowHtml: () -> Unit
) {
    val isVisible = clipboardContentType != ClipboardContentType.NONE
    val buttonText = when (clipboardContentType) {
        ClipboardContentType.DIAGRAM -> "Show Diagram"
        ClipboardContentType.HTML -> "Preview HTML"
        else -> ""
    }
    val onClick = when (clipboardContentType) {
        ClipboardContentType.DIAGRAM -> onShowDiagram
        ClipboardContentType.HTML -> onShowHtml
        else -> ({})
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier.align(Alignment.BottomEnd)
    ) {
        DialogButton(text = buttonText, onClick = onClick)
    }
}

@Composable
private fun BoxScope.MenuHandles(
    isMenuLeft: Boolean,
    isHighlighting: Boolean,
    onReload: () -> Unit,
    onOpenMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .statusBarsPadding()
            .align(Alignment.TopCenter)
            .width(80.dp)
            .height(60.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    onReload()
                    change.consume()
                }
            }
    )

    val animatedColor by animateColorAsState(
        targetValue = if (isHighlighting) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else Color.Transparent,
        animationSpec = tween(durationMillis = 500),
        label = "menuHighlightColor"
    )
    val alignment = if (isMenuLeft) Alignment.CenterStart else Alignment.CenterEnd

    Box(
        modifier = Modifier
            .align(alignment)
            .width(30.dp)
            .fillMaxHeight(0.4f)
            .background(animatedColor)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    onOpenMenu()
                    change.consume()
                }
            }
    )
}

@Composable
private fun rememberMenuItems(onEvent: (GeminiUiEvent) -> Unit, isMenuLeft: Boolean): List<MenuItemData> {
    return remember(isMenuLeft) {
        listOf(
            MenuItemData(R.drawable.google_docs, "Open Docs") { onEvent(GeminiUiEvent.OpenDocsClicked) },
            MenuItemData(R.drawable.coffee_cup, "Caffeine") { onEvent(GeminiUiEvent.KeepScreenOnToggled) },
            MenuItemData(R.drawable.split_screen, "Split Screen") { onEvent(GeminiUiEvent.SplitScreenToggled) },
            MenuItemData(R.drawable.note_text, "Save To File") { onEvent(GeminiUiEvent.SaveToFileClicked) },
            MenuItemData(R.drawable.baseline_open_in_browser_24, "Open in Browser") { onEvent(GeminiUiEvent.OpenInBrowserClicked) },
            MenuItemData(R.drawable.baseline_share_24, "Share Page") { onEvent(GeminiUiEvent.SharePageClicked) },
            MenuItemData(R.drawable.baseline_link_24, "Copy Link") { onEvent(GeminiUiEvent.CopyLinkClicked) },
            MenuItemData(R.drawable.baseline_replay_circle_filled_24, "Reload Page") { onEvent(GeminiUiEvent.ReloadPageClicked) },
            MenuItemData(R.drawable.baseline_arrow_circle_right_24, "Go Forward") { onEvent(GeminiUiEvent.GoForwardClicked) },
            MenuItemData(
                painterResId = if (isMenuLeft) R.drawable.outline_arrow_menu_close_24 else R.drawable.outline_arrow_menu_open_24,
                name = "Menu Side"
            ) { onEvent(GeminiUiEvent.MenuPositionChanged(!isMenuLeft)) }
        )
    }
}

@Composable
private fun DialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.padding(16.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.secondary
        )
    ) {
        androidx.compose.material3.Text(text)
    }
}

@Composable
private fun BoxScope.ReloadIndicator(isLoading: Boolean) {
    AnimatedVisibility(
        visible = isLoading,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            modifier = Modifier.padding(top = 16.dp),
            shape = CircleShape,
            shadowElevation = 8.dp
        ) {
            CircularProgressIndicator(
                modifier = Modifier.padding(8.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}