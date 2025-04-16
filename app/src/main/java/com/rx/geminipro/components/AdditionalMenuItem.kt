package com.rx.geminipro.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun AdditionalMenuItem(painter: Painter, name: String, onClick: () -> Unit)
{
    val haptic = LocalHapticFeedback.current
    Column(modifier = Modifier.padding(horizontal = 5.dp),){
        IconButton(
            modifier = Modifier.align(Alignment.CenterHorizontally).size(70.dp),
            onClick =
            {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            },
            content = {
                Image(
                    painter = painter,
                    contentDescription = null
                )
            }
        )
        Text(modifier = Modifier.align(Alignment.CenterHorizontally), text = name, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
    }
}