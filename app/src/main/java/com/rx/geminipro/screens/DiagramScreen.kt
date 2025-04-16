package com.rx.geminipro.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rx.geminipro.components.HtmlViewer
import com.rx.geminipro.components.generateHtmlWithMermaid

@Composable
fun DiagramScreen(showDiagram: Boolean, clipboardText: String, onBack: () -> Unit, onClose: () -> Unit) {
    AnimatedVisibility(showDiagram) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            BackHandler(showDiagram) {
                onBack()
            }
            HtmlViewer(htmlContent = generateHtmlWithMermaid(clipboardText))
            Button(
                onClick = {
                    onClose()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                colors = ButtonColors(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primary,
                )
            ) {
                Text("Close Diagram")
            }
        }
    }
}