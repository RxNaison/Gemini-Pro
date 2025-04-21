package com.rx.geminipro.screens

import android.content.Context
import android.net.Uri
import android.webkit.ValueCallback
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
    val scope = rememberCoroutineScope()

    var splitScreen by remember { mutableStateOf(false) }
    var openAdditionalMenu by remember {mutableStateOf(false)}

    val filePathCallbackState = remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val filePickerLauncher = getFilePickerLauncher(filePathCallbackState)

    val density = LocalDensity.current
    val keyboardHeightDp = with(density) { WindowInsets.ime.getBottom(density).toDp() }

    val isKeyBoardShown = keyboardHeightDp > 10.dp

    var pointerPosition by remember { mutableStateOf(Offset.Zero) }
    val pointerPositionDp = with(density) { pointerPosition.y.toDp() }

    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp

    val keyboardTopThreshold = screenHeightDp - keyboardHeightDp

    GetPermissions(context)

    val diagramPrefixes = getMermaidDiagramPrefixes()

    RunClipboardManager(clipboardText, onCopied = {
        if (diagramPrefixes.any { clipboardText.value.startsWith(it) }) {
            scope.launch {
                showDiagramButton = true
                delay(6500)
                showDiagramButton = false
            }
        }
    })


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        if(splitScreen)
        {
            geminiHtmlViewer(
                filePathCallbackState,
                filePickerLauncher,
                isKeyBoardShown,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .weight(1f)
            )

        }

        geminiViewModel.webViewState = geminiHtmlViewer(
            filePathCallbackState,
            filePickerLauncher,
            isKeyBoardShown,
            modifier = modifier
                .fillMaxSize()
                .weight(1f)
                .offset(y = if (pointerPositionDp > keyboardTopThreshold - 50.dp) -keyboardHeightDp else 0.dp)
                .pointerInput(Unit) {
                    while (true) {
                        awaitPointerEventScope {
                            val event = awaitPointerEvent()
                            event.changes
                                .firstOrNull()
                                ?.let { change ->
                                    pointerPosition = change.position
                                }
                        }
                    }
                }
        ) {
            geminiViewModel.Ready()
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
                             if(dragAmount != 0f)
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
        if(openAdditionalMenu)
            AdditionalMenu(
                {openAdditionalMenu = false},
                listOf(
                    {
                        AdditionalMenuItem(
                            painterResource(id = R.drawable.google_docs),
                            "Open Docs"
                        ){ googleServices.openGoogleDocs(context) }
                    },
                    {
                        AdditionalMenuItem(
                            painterResource(id = R.drawable.coffee_cup),
                            "Caffeine"
                        ){
                            geminiViewModel.KeepScreenOnSwitch()

                            if(geminiViewModel.keepScreenOn.value)
                                Toast.makeText(context, "Caffeine is turned on", Toast.LENGTH_SHORT).show()
                            else
                                Toast.makeText(context, "Caffeine is turned off", Toast.LENGTH_SHORT).show()
                        }
                    },
                    {
                        AdditionalMenuItem(
                            painterResource(id = R.drawable.split_screen),
                            "Split Screen"
                        ){ splitScreen = !splitScreen }
                    },
                    {
                        AdditionalMenuItem(
                            painterResource(id = R.drawable.note_text),
                            "Save To File"
                        ){ documentLauncher.launch("myFile.txt") }
                    }
                )
            )
    }
}

