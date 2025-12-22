package com.rx.geminipro.screens

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }


    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(isKeyboardVisible) {
        viewModel.onEvent(GeminiUiEvent.KeyboardVisibilityChanged(isKeyboardVisible))
    }

    // --- File Picker Setup ---
    val filePathCallbackState = remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (filePathCallbackState.value == null) return@rememberLauncherForActivityResult

        val uris: Array<Uri> = if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data

            if (data?.data != null) {
                arrayOf(data.data!!)
            }
            else if (data?.clipData != null) {
                val clip = data.clipData!!
                List(clip.itemCount) { clip.getItemAt(it).uri }.toTypedArray()
            }
            else if (tempCameraUri != null) {
                arrayOf(tempCameraUri!!)
            }
            else {
                emptyArray()
            }
        } else {
            emptyArray()
        }

        filePathCallbackState.value?.onReceiveValue(uris)
        filePathCallbackState.value = null
        tempCameraUri = null
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
                is GeminiSideEffect.ShowToast -> Toast.makeText(
                    context,
                    effect.message,
                    Toast.LENGTH_SHORT
                ).show()

                is GeminiSideEffect.LaunchSaveToFile -> documentLauncher.launch("gemini-note.txt")
                is GeminiSideEffect.WebViewGoBack -> currentWebView.goBack()
                is GeminiSideEffect.WebViewReload -> currentWebView.reload()
                is GeminiSideEffect.WebViewGoForward -> currentWebView.goForward()
                is GeminiSideEffect.LoadUrl -> currentWebView.loadUrl(effect.url)
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
        GeminiWebViewer(
            modifier = modifier.fillMaxSize(),
            filePathCallbackState = filePathCallbackState,
            filePickerLauncher = filePickerLauncher,
            isVideoSelectionMode = uiState.isVideoSelectionMode,
            onWebViewCreated = { createdWebView ->
                webView = createdWebView
            },
            onProgressChanged = { progress ->
                viewModel.onEvent(GeminiUiEvent.LoadingProgressChanged(progress))
            },
            onPageFinished = { webView, _ ->
                viewModel.onEvent(
                    GeminiUiEvent.WebViewNavigated(
                        canGoBack = webView.canGoBack(),
                        url = webView.url
                    )
                )
                viewModel.onEvent(GeminiUiEvent.ApplicationReady)
            },
            onCameraTmpFileCreated = { uri ->
                tempCameraUri = uri
            }
        )

        BrowserProgressBar(progress = uiState.loadingProgress)

        ReloadIndicator(isLoading = uiState.isReloading)

        VideoModeIndicator(uiState.isVideoSelectionMode)

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

        if (uiState.updateInfo != null) {
            UpdateAvailableDialog(
                newVersion = uiState.updateInfo!!.version,
                changelog = uiState.updateInfo!!.changelog,
                onDismiss = { viewModel.onEvent(GeminiUiEvent.DismissUpdateDialog) },
                onDownload = { viewModel.onEvent(GeminiUiEvent.UpdateClicked) },
                onSkipVersion = { viewModel.onEvent(GeminiUiEvent.SkipUpdateClicked) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateAvailableDialog(
    newVersion: String,
    changelog: String,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onSkipVersion: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 20.dp),
                text = "New Update Available",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                modifier = Modifier.align(Alignment.Start).padding(start = 20.dp),
                text = "Version $newVersion is available.\nWould you like to download it?"
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "What's New:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .heightIn(max = 200.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
                    Text(
                        text = changelog,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row (modifier = Modifier.navigationBarsPadding().padding(vertical = 20.dp).align(Alignment.CenterHorizontally)){
                TextButton(
                    onClick = onSkipVersion,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Text("Skip this version")
                }

                TextButton(onClick = onDismiss) {
                    Text("Remind me later")
                }

                TextButton(onClick = onDownload) {
                    Text("Download")
                }
            }
        }
    }
}

@Composable
fun BoxScope.BrowserProgressBar(progress: Int) {
    val isVisible = progress in 1..99

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(4.dp)
            .align(Alignment.TopCenter)
    ) {
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent,
        )
    }
}

@Composable
private fun BoxScope.VideoModeIndicator(isVisible: Boolean){
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(Color(0xFF2196F3).copy(alpha = 0.8f))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Tap and hold the video to download it",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
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
            MenuItemData(R.drawable.google_flow_icon, "Google Flow") { onEvent(GeminiUiEvent.OpenFlowClicked) },
            MenuItemData(R.drawable.note_text, "Save To File") { onEvent(GeminiUiEvent.SaveToFileClicked) },
            MenuItemData(R.drawable.baseline_save_alt_24, "Download Video") { onEvent(GeminiUiEvent.ToggleVideoSelectionMode) },
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
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.secondary
        )
    ) {
        Text(text)
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