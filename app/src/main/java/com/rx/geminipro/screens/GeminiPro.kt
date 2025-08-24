package com.rx.geminipro.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
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

private enum class ActiveWebView { VIEW_ONE, VIEW_TWO }

@Composable
fun GeminiViewer(
    isConnected: Boolean,
    context: Context,
    geminiViewModel: GeminiViewModel,
    modifier: Modifier = Modifier
) {
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


    GetPermissions(context)

    val diagramPrefixes = getMermaidDiagramPrefixes()

    RunClipboardManager(clipboardText, onCopied = {
        if (diagramPrefixes.any { clipboardText.value.startsWith(it) }) {
            scope.launch {
                showDiagramButton = true
                delay(6500)
                showDiagramButton = false
            }
        } else if (clipboardText.value.lowercase().startsWith("<!doctype html") ||
            clipboardText.value.lowercase().startsWith("<html>")
        ) {
            scope.launch {
                showHtmlPreviewButton = true
                delay(6500)
                showHtmlPreviewButton = false
            }
        } else {
            showDiagramButton = false
            showHtmlPreviewButton = false
        }
    })


    Column(
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
                    modifier = Modifier.weight(1f).offset(y = keyboardOffset).statusBarsPadding(),
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
                    modifier = modifier.weight(1f).offset(y = keyboardOffset).statusBarsPadding(),
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
                        geminiViewModel.Ready()
                    }
                )
            }
        } else {
            Column(modifier = parentModifier) {

                if (geminiViewModel.splitScreen.value) {
                    ShowWebview(
                        modifier = Modifier.weight(1f).statusBarsPadding(),
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
                    modifier = modifier.offset(y = keyboardOffset).weight(1f),
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
                        geminiViewModel.Ready()
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = showDiagramButton,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.End)
        ) {
            Button(
                modifier = Modifier
                    .align(Alignment.End)
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
            modifier = Modifier.align(Alignment.End)
        ) {
            Button(
                modifier = Modifier
                    .align(Alignment.End)
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

        Box(
            modifier = Modifier
                .size(width = 30.dp, height = 350.dp)
                .align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            openAdditionalMenu = dragAmount != 0f
                            change.consume()
                        }
                    )
                }
        )
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
                            geminiViewModel.KeepScreenOnSwitch()

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
                            documentLauncher.launch("myFile.txt")
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

