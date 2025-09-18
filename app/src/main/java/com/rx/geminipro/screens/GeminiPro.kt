package com.rx.geminipro.screens

import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.rx.geminipro.R
import com.rx.geminipro.components.AdditionalMenuItem
import com.rx.geminipro.utils.clipboard.RunClipboardManager
import com.rx.geminipro.utils.file.createDocumentLauncher
import com.rx.geminipro.components.geminiHtmlViewer
import com.rx.geminipro.utils.file.getFilePickerLauncher
import com.rx.geminipro.components.getMermaidDiagramPrefixes
import com.rx.geminipro.utils.permissions.GetPermissions
import com.rx.geminipro.utils.services.GoogleServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.net.toUri

private enum class ActiveWebView { VIEW_ONE, VIEW_TWO }

@Composable
fun GeminiViewer(
    isConnected: Boolean,
    geminiViewModel: GeminiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val clipboardText = remember { mutableStateOf("") }
    var showDiagramButton by remember { mutableStateOf(false) }
    var showDiagram by remember { mutableStateOf(false) }

    var showHtmlPreviewButton by remember { mutableStateOf(false) }
    var showHtmlPreviewScreen by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    var openAdditionalMenu by remember { mutableStateOf(false) }

    val filePathCallbackState = remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val filePickerLauncher = getFilePickerLauncher(filePathCallbackState)

    val density = LocalDensity.current
    val keyboardHeightDp = with(density) { WindowInsets.ime.getBottom(density).toDp() }

    val isKeyBoardShown = keyboardHeightDp > 10.dp

    var pointerPosition by remember { mutableStateOf(Offset.Zero) }
    val pointerPositionDp = with(density) { pointerPosition.y.toDp() }

    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp

    var lastTouchedWebView by remember { mutableStateOf(ActiveWebView.VIEW_ONE) }

    val keyboardTopThreshold = screenHeightDp - keyboardHeightDp

    val keyboardOffset = if (
        pointerPositionDp > keyboardTopThreshold - 50.dp ||
        geminiViewModel.splitScreen.value &&
        lastTouchedWebView == ActiveWebView.VIEW_ONE
    ) -keyboardHeightDp else 0.dp

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isLargeScreen = configuration.screenWidthDp.dp > 600.dp

    val useHorizontalLayout = geminiViewModel.splitScreen.value && (isLandscape || isLargeScreen)
    val clipboardManager = LocalClipboardManager.current
    val isMenuLeft by geminiViewModel.isMenuLeft.collectAsState()

    var isHighlighting by remember { mutableStateOf(false) }

    val animatedColor by animateColorAsState(
        targetValue = if (isHighlighting) MaterialTheme.colorScheme.secondary else Color.Transparent,
        animationSpec = tween(durationMillis = 500),
        label = "menuHighlightColor"
    )

    GetPermissions(context)

    val diagramPrefixes = getMermaidDiagramPrefixes()

    RunClipboardManager(clipboardText, onCopied = {
        if (diagramPrefixes.any { clipboardText.value.startsWith(it) }) {
            scope.launch {
                showHtmlPreviewButton = false
                showDiagramButton = true
                delay(6500)
                showDiagramButton = false
            }
        } else if (clipboardText.value.lowercase().startsWith("<!doctype html") ||
            clipboardText.value.lowercase().startsWith("<html>")
        ) {
            scope.launch {
                showDiagramButton = false
                showHtmlPreviewButton = true
                delay(6500)
                showHtmlPreviewButton = false
            }
        } else {
            showDiagramButton = false
            showHtmlPreviewButton = false
        }
    })


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val parentModifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)

        if (useHorizontalLayout) {
            Row(modifier = parentModifier) {

                ShowWebview(
                    modifier = Modifier
                        .weight(1f)
                        .offset(y = keyboardOffset)
                        .statusBarsPadding(),
                    geminiViewModel = geminiViewModel,
                    filePathCallbackState = filePathCallbackState,
                    filePickerLauncher = filePickerLauncher,
                    isKeyBoardShown = isKeyBoardShown,
                    onViewTouched = { offset ->
                        pointerPosition = offset
                        lastTouchedWebView = ActiveWebView.VIEW_TWO
                    }
                )

                ShowWebview(
                    modifier = modifier
                        .weight(1f)
                        .offset(y = keyboardOffset)
                        .statusBarsPadding(),
                    geminiViewModel = geminiViewModel,
                    filePathCallbackState = filePathCallbackState,
                    filePickerLauncher = filePickerLauncher,
                    isKeyBoardShown = isKeyBoardShown,
                    onViewTouched = { offset ->
                        pointerPosition = offset
                        lastTouchedWebView = ActiveWebView.VIEW_ONE
                    },
                    onWebViewReady = {
                        val thresholdInPx = with(density) { keyboardTopThreshold.toPx() }
                        pointerPosition = Offset(x = pointerPosition.x, y = thresholdInPx)
                        geminiViewModel.ready()
                    }
                )
            }
        } else {
            Column(modifier = parentModifier) {

                if (geminiViewModel.splitScreen.value) {
                    ShowWebview(
                        modifier = Modifier
                            .weight(1f)
                            .statusBarsPadding(),
                        geminiViewModel = geminiViewModel,
                        filePathCallbackState = filePathCallbackState,
                        filePickerLauncher = filePickerLauncher,
                        isKeyBoardShown = isKeyBoardShown,
                        onViewTouched = {
                            lastTouchedWebView = ActiveWebView.VIEW_TWO
                            pointerPosition = Offset.Zero
                        }
                    )
                }

                ShowWebview(
                    modifier = modifier
                        .weight(1f)
                        .offset(y = keyboardOffset),
                    geminiViewModel = geminiViewModel,
                    filePathCallbackState = filePathCallbackState,
                    filePickerLauncher = filePickerLauncher,
                    isKeyBoardShown = isKeyBoardShown,
                    onViewTouched = { offset ->
                        pointerPosition = offset
                        lastTouchedWebView = ActiveWebView.VIEW_ONE
                    },
                    onWebViewReady = {
                        val thresholdInPx = with(density) { keyboardTopThreshold.toPx() }
                        pointerPosition = Offset(x = pointerPosition.x, y = thresholdInPx)
                        geminiViewModel.ready()
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = showDiagramButton,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Button(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                onClick = {
                    showDiagram = true
                    showDiagramButton = false
                },
                colors = ButtonColors(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primary,
                )
            ) {
                Text("Show Diagram")
            }
        }
        DiagramScreen(
            showDiagram,
            clipboardText.value,
            onBack = {
                showDiagram = false
            },
            onClose = {
                showDiagram = false
                showDiagramButton = false
            }
        )

        AnimatedVisibility(
            visible = showHtmlPreviewButton,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Button(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                onClick = {
                    showHtmlPreviewScreen = true
                    showHtmlPreviewButton = false
                },
                colors = ButtonColors(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primary,
                )
            ) {
                Text("Preview HTML")
            }
        }
        HtmlPreviewScreen(
            showHtmlPreview = showHtmlPreviewScreen,
            htmlContent = clipboardText.value,
            onBack = {
                showHtmlPreviewScreen = false
            },
            onClose = {
                showHtmlPreviewScreen = false
                showHtmlPreviewButton = false
            }
        )
        NoConnectionScreen(isConnected)
    }

    Box(modifier = Modifier.fillMaxSize())
    {
        Box(
            modifier = Modifier
                .size(width = 30.dp, height = 150.dp)
                .align(Alignment.TopCenter)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            if (dragAmount != 0f)
                                geminiViewModel.webViewState.value?.reload()
                            change.consume()
                        }
                    )
                }
        )

        LaunchedEffect(isMenuLeft) {
            isHighlighting = true
            delay(2000)
            isHighlighting = false
        }

        if(!isKeyBoardShown)
        {
            var touchAreaModifier = Modifier
                .size(width = 30.dp, height = 350.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            openAdditionalMenu = dragAmount != 0f
                            change.consume()
                        }
                    )
                }
                .background(animatedColor)

            touchAreaModifier = if(isMenuLeft)
                touchAreaModifier.align(Alignment.CenterStart)
            else
                touchAreaModifier.align(Alignment.CenterEnd)

            Box(modifier = touchAreaModifier.align(Alignment.CenterEnd))
        }

        val documentLauncher = createDocumentLauncher(context, clipboardText.value)
        val googleServices = GoogleServices()
        if (openAdditionalMenu) {
            AdditionalMenu(
                { openAdditionalMenu = false },
                listOf(
                    {
                        AdditionalMenuItem(
                            painterResource(id = R.drawable.google_docs),
                            "Open Docs"
                        ) {
                            googleServices.openGoogleDocs(context)
                        }
                    },
                    {
                        AdditionalMenuItem(
                            painterResource(id = R.drawable.coffee_cup),
                            "Caffeine"
                        ) {
                            geminiViewModel.keepScreenOnSwitch()

                            if (geminiViewModel.keepScreenOn.value)
                                Toast.makeText(
                                    context,
                                    "Caffeine is turned on",
                                    Toast.LENGTH_SHORT
                                ).show()
                            else
                                Toast.makeText(
                                    context,
                                    "Caffeine is turned off",
                                    Toast.LENGTH_SHORT
                                ).show()
                        }
                    },
                    {
                        AdditionalMenuItem(
                            painterResource(id = R.drawable.split_screen),
                            "Split Screen"
                        ) {
                            geminiViewModel.splitScreen.value = !geminiViewModel.splitScreen.value
                        }
                    },
                    {
                        AdditionalMenuItem(
                            painterResource(id = R.drawable.note_text),
                            "Save To File"
                        ) {
                            documentLauncher.launch("file.txt")
                        }
                    },
                    {
                        AdditionalMenuItem(
                            painterResource(id = R.drawable.baseline_open_in_browser_24),
                            "Open in Browser"
                        ) {
                            geminiViewModel.webViewState.value?.url?.let { url ->
                                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                context.startActivity(intent)
                            }
                            openAdditionalMenu = false
                        }
                    },
                    {
                        AdditionalMenuItem(
                            painterResource(id = R.drawable.baseline_share_24),
                            "Share Page"
                        ) {
                            geminiViewModel.webViewState.value?.url?.let { url ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, url)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(intent, "Share URL")
                                context.startActivity(shareIntent)
                            }
                            openAdditionalMenu = false
                        }
                    },
                    {
                        AdditionalMenuItem(
                            painterResource(id = R.drawable.baseline_link_24),
                            "Copy Link"
                        ) {
                            geminiViewModel.webViewState.value?.url?.let { url ->
                                clipboardManager.setText(AnnotatedString(url))
                                Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
                            }
                            openAdditionalMenu = false
                        }
                    },
                    {
                        AdditionalMenuItem(
                            painterResource(id = R.drawable.baseline_replay_circle_filled_24),
                            "Reload Page"
                        ) {
                            geminiViewModel.webViewState.value?.reload()
                            openAdditionalMenu = false
                        }
                    },
                    {
                        AdditionalMenuItem(
                            painterResource(id = R.drawable.baseline_arrow_circle_right_24),
                            "Go Forward",
                        ) {
                            geminiViewModel.webViewState.value?.goForward()
                            openAdditionalMenu = false
                        }
                    },
                    {
                        AdditionalMenuItem(
                            painterResource(
                                id = if(isMenuLeft)
                                    R.drawable.outline_arrow_menu_close_24
                                else
                                    R.drawable.outline_arrow_menu_open_24
                            ),
                            "Menu Side",
                        ) {
                            geminiViewModel.setMenuPosition(!isMenuLeft)
                        }
                    }
                )
            )
        }
    }
}


@Composable
private fun ShowWebview(
    modifier: Modifier = Modifier,
    geminiViewModel: GeminiViewModel,
    filePathCallbackState: MutableState<ValueCallback<Array<Uri>>?>,
    filePickerLauncher: ActivityResultLauncher<Intent>?,
    isKeyBoardShown: Boolean,
    onViewTouched: (Offset) -> Unit,
    onWebViewReady: () -> Unit = {}
) {
    geminiViewModel.webViewState = geminiHtmlViewer(
        filePathCallbackState,
        filePickerLauncher,
        isKeyBoardShown,
        modifier = modifier
            .pointerInput(Unit) {
                while (true) {
                    awaitPointerEventScope {
                        val event = awaitPointerEvent()
                        event.changes
                            .firstOrNull()
                            ?.let { change ->
                                onViewTouched(change.position)
                            }
                    }
                }
            }
    ) {
        onWebViewReady()
    }
}

