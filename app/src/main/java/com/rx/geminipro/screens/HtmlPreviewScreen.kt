package com.rx.geminipro.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rx.geminipro.components.HtmlViewer

@Composable
fun HtmlPreviewScreen(
    showHtmlPreview: Boolean,
    htmlContent: String,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    AnimatedVisibility(showHtmlPreview) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            BackHandler(showHtmlPreview) {
                onBack()
            }
            HtmlViewer(htmlContent = htmlContent)
            Button(
                onClick = {
                    onClose()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                colors = ButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.secondary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    disabledContentColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                )
            ) {
                Text("Close Preview")
            }
        }
    }
}