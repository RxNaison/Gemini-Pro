package com.rx.geminipro.screens

import androidx.compose.runtime.Composable
import com.rx.geminipro.components.HtmlViewer

@Composable
fun HtmlPreviewScreen(isHtmlPreviewVisible: Boolean, clipboardText: String, onBack: () -> Unit, onClose: () -> Unit) {
    GenericPreviewScreen(
        isVisible = isHtmlPreviewVisible,
        title = "Close Preview",
        onBack = onBack,
        onClose = onClose
    ) {
        HtmlViewer(htmlContent = clipboardText)
    }
}